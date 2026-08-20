package tz.go.tirdo.teltp.assessment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.assessment.dto.AssessmentDtos.*;
import tz.go.tirdo.teltp.assessment.entity.*;
import tz.go.tirdo.teltp.assessment.repository.AssessmentRepository;
import tz.go.tirdo.teltp.assessment.repository.AttemptRepository;
import tz.go.tirdo.teltp.assessment.repository.QuestionRepository;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final ReferenceNumberGenerator refGen;

    public AssessmentService(AssessmentRepository assessments, QuestionRepository questions,
                             AttemptRepository attempts, ReferenceNumberGenerator refGen) {
        this.assessments = assessments;
        this.questions = questions;
        this.attempts = attempts;
        this.refGen = refGen;
    }

    // ---- authoring ----

    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest req) {
        Assessment a = new Assessment();
        a.setReferenceNumber(refGen.next("ASMT"));
        a.setCourseUuid(req.courseUuid());
        a.setTitle(req.title());
        a.setType(AssessmentType.valueOf(req.type()));
        a.setPassMark(req.passMark());
        a.setTimeLimitMinutes(req.timeLimitMinutes());
        a.setPricingPlanUuid(req.pricingPlanUuid());
        return toResponse(assessments.save(a));
    }

    @Transactional
    public QuestionView addQuestion(AddQuestionRequest req) {
        Assessment a = requireAssessment(req.assessmentUuid());
        QuestionType type = QuestionType.valueOf(req.type());
        Question q = new Question();
        q.setAssessment(a);
        q.setPrompt(req.prompt());
        q.setType(type);
        q.setPoints(req.points());
        if (type == QuestionType.MULTIPLE_CHOICE) {
            if (req.options() == null || req.options().isEmpty())
                throw new BusinessRuleException("Multiple-choice question requires options");
            if (req.options().stream().noneMatch(OptionRequest::correct))
                throw new BusinessRuleException("At least one option must be marked correct");
            req.options().forEach(o -> {
                AnswerOption opt = new AnswerOption();
                opt.setQuestion(q);
                opt.setText(o.text());
                opt.setCorrect(o.correct());
                q.getOptions().add(opt);
            });
        }
        a.getQuestions().add(q);
        assessments.save(a);
        return toQuestionView(q);
    }

    // ---- taking ----

    /** Student-facing rendering with correct flags stripped. */
    @Transactional(readOnly = true)
    public AssessmentView view(String assessmentUuid) {
        Assessment a = requireAssessment(assessmentUuid);
        List<QuestionView> qv = a.getQuestions().stream().map(this::toQuestionView).toList();
        return new AssessmentView(a.getUuid(), a.getTitle(), a.getType().name(),
                a.getPassMark(), a.getTimeLimitMinutes(), qv);
    }

    @Transactional
    public AttemptResponse startAttempt(String studentUuid, String assessmentUuid) {
        requireAssessment(assessmentUuid);
        Attempt attempt = new Attempt();
        attempt.setAssessmentUuid(assessmentUuid);
        attempt.setStudentUuid(studentUuid);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        return toAttemptResponse(attempts.save(attempt));
    }

    /**
     * Submit answers. MCQ questions are graded immediately; if any manual-grade questions
     * exist the attempt parks in AWAITING_MANUAL_GRADING, otherwise it is AUTO_GRADED and
     * the pass/fail outcome is computed against the assessment pass mark.
     */
    @Transactional
    public AttemptResponse submit(String studentUuid, SubmitAttemptRequest req) {
        Assessment a = requireAssessment(req.assessmentUuid());
        Attempt attempt = attempts.findByStudentUuidAndAssessmentUuid(studentUuid, req.assessmentUuid())
                .stream().filter(at -> at.getStatus() == AttemptStatus.IN_PROGRESS)
                .reduce((first, second) -> second)
                .orElseGet(() -> {
                    Attempt fresh = new Attempt();
                    fresh.setAssessmentUuid(req.assessmentUuid());
                    fresh.setStudentUuid(studentUuid);
                    return fresh;
                });

        Map<String, Question> byUuid = a.getQuestions().stream()
                .collect(Collectors.toMap(q -> q.getUuid(), Function.identity()));

        boolean manualPending = false;
        for (SubmittedAnswer sa : req.answers()) {
            Question q = byUuid.get(sa.questionUuid());
            if (q == null) throw new BusinessRuleException("Unknown question: " + sa.questionUuid());

            AttemptAnswer ans = new AttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestionUuid(sa.questionUuid());
            ans.setSelectedOptionUuid(sa.selectedOptionUuid());
            ans.setResponse(sa.response());

            if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                boolean correct = q.getOptions().stream()
                        .anyMatch(o -> o.isCorrect() && o.getUuid().equals(sa.selectedOptionUuid()));
                ans.setAwardedPoints(correct ? q.getPoints() : 0);
            } else {
                manualPending = true;  // graded later by instructor
            }
            attempt.getAnswers().add(ans);
        }

        attempt.setSubmittedAt(Instant.now());
        if (manualPending) {
            attempt.setStatus(AttemptStatus.AWAITING_MANUAL_GRADING);
        } else {
            finalizeGrade(a, attempt);
            attempt.setStatus(AttemptStatus.AUTO_GRADED);
        }
        return toAttemptResponse(attempts.save(attempt));
    }

    // ---- manual grading ----

    @Transactional
    public AttemptResponse gradeAnswer(GradeAnswerRequest req) {
        Attempt attempt = attempts.findByUuid(req.attemptUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", req.attemptUuid()));
        AttemptAnswer ans = attempt.getAnswers().stream()
                .filter(x -> x.getQuestionUuid().equals(req.questionUuid())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AttemptAnswer", req.questionUuid()));
        ans.setAwardedPoints(req.awardedPoints());
        ans.setGraderFeedback(req.feedback());

        Assessment a = requireAssessment(attempt.getAssessmentUuid());
        boolean allGraded = attempt.getAnswers().stream().allMatch(x -> x.getAwardedPoints() != null);
        if (allGraded) {
            finalizeGrade(a, attempt);
            attempt.setStatus(AttemptStatus.GRADED);
        }
        return toAttemptResponse(attempts.save(attempt));
    }

    private void finalizeGrade(Assessment a, Attempt attempt) {
        int totalPoints = a.getQuestions().stream().mapToInt(Question::getPoints).sum();
        int earned = attempt.getAnswers().stream()
                .mapToInt(x -> x.getAwardedPoints() == null ? 0 : x.getAwardedPoints()).sum();
        int percent = totalPoints == 0 ? 0 : (int) Math.round(100.0 * earned / totalPoints);
        attempt.setScorePercent(percent);
        attempt.setPassed(percent >= a.getPassMark());
    }

    private Assessment requireAssessment(String uuid) {
        return assessments.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Assessment", uuid));
    }

    private AssessmentResponse toResponse(Assessment a) {
        return new AssessmentResponse(a.getUuid(), a.getReferenceNumber(), a.getCourseUuid(),
                a.getTitle(), a.getType().name(), a.getPassMark(), a.getTimeLimitMinutes());
    }

    private QuestionView toQuestionView(Question q) {
        List<OptionView> opts = q.getOptions().stream()
                .map(o -> new OptionView(o.getUuid(), o.getText())).toList();
        return new QuestionView(q.getUuid(), q.getPrompt(), q.getType().name(), q.getPoints(), opts);
    }

    private AttemptResponse toAttemptResponse(Attempt at) {
        return new AttemptResponse(at.getUuid(), at.getAssessmentUuid(), at.getStudentUuid(),
                at.getStatus().name(), at.getScorePercent(), at.getPassed());
    }
}
