package tz.go.tirdo.teltp.assessment.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.assessment.dto.AssessmentDtos.*;
import tz.go.tirdo.teltp.assessment.entity.*;
import tz.go.tirdo.teltp.assessment.repository.AssessmentRepository;
import tz.go.tirdo.teltp.assessment.repository.AttemptRepository;
import tz.go.tirdo.teltp.assessment.repository.QuestionRepository;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.event.CourseCompletionCandidateEvent;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    /** Clock-skew tolerance: submissions arriving within this window past expiry are still accepted. */
    private static final Duration GRACE = Duration.ofSeconds(30);

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final ReferenceNumberGenerator refGen;
    private final ApplicationEventPublisher events;

    public AssessmentService(AssessmentRepository assessments, QuestionRepository questions,
                             AttemptRepository attempts, ReferenceNumberGenerator refGen,
                             ApplicationEventPublisher events) {
        this.assessments = assessments;
        this.questions = questions;
        this.attempts = attempts;
        this.refGen = refGen;
        this.events = events;
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
        a.setMaxAttempts(req.maxAttempts());
        a.setCooldownMinutes(req.cooldownMinutes());
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

    @Transactional(readOnly = true)
    public List<AssessmentResponse> listForCourse(String courseUuid) {
        return assessments.findByCourseUuid(courseUuid).stream().map(this::toResponse).toList();
    }

    /**
     * Whether the student has satisfied the exam requirement for a course: true when the
     * course has no EXAM, or when the student has a passed attempt for every EXAM it has.
     * Consulted by the progress module when computing course completion.
     */
    @Transactional(readOnly = true)
    public boolean hasSatisfiedExamGate(String studentUuid, String courseUuid) {
        var exams = assessments.findByCourseUuid(courseUuid).stream()
                .filter(a -> a.getType() == AssessmentType.EXAM)
                .toList();
        if (exams.isEmpty()) return true;
        for (var exam : exams) {
            boolean passed = attempts.findByStudentUuidAndAssessmentUuid(studentUuid, exam.getUuid())
                    .stream().anyMatch(at -> Boolean.TRUE.equals(at.getPassed()));
            if (!passed) return false;
        }
        return true;
    }

    /** Student-facing rendering with correct flags stripped. */
    @Transactional(readOnly = true)
    public AssessmentView view(String assessmentUuid) {
        Assessment a = requireAssessment(assessmentUuid);
        List<QuestionView> qv = a.getQuestions().stream().map(this::toQuestionView).toList();
        return new AssessmentView(a.getUuid(), a.getTitle(), a.getType().name(),
                a.getPassMark(), a.getTimeLimitMinutes(), qv);
    }

    /** Report whether the student may (re)take this assessment, expiring any stale attempt first. */
    @Transactional
    public AttemptEligibility eligibility(String studentUuid, String assessmentUuid) {
        return computeEligibility(studentUuid, requireAssessment(assessmentUuid));
    }

    /**
     * Start (or resume) an attempt. Resumes an existing non-expired in-progress attempt without
     * consuming a new one; otherwise enforces the retake policy (max attempts, cooldown,
     * no retake after a pass) and stamps an expiry when the assessment is timed.
     */
    @Transactional
    public AttemptResponse startAttempt(String studentUuid, String assessmentUuid) {
        Assessment a = requireAssessment(assessmentUuid);
        AttemptEligibility e = computeEligibility(studentUuid, a);

        if (e.hasActiveAttempt()) {
            Attempt active = attempts.findByUuid(e.activeAttemptUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Attempt", e.activeAttemptUuid()));
            return toAttemptResponse(active);
        }
        if (!e.canAttempt()) throw new BusinessRuleException(e.reason());

        Attempt attempt = new Attempt();
        attempt.setAssessmentUuid(assessmentUuid);
        attempt.setStudentUuid(studentUuid);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        if (a.getTimeLimitMinutes() != null && a.getTimeLimitMinutes() > 0) {
            attempt.setExpiresAt(Instant.now().plus(Duration.ofMinutes(a.getTimeLimitMinutes())));
        }
        return toAttemptResponse(attempts.save(attempt));
    }

    /**
     * Submit answers for the student's active attempt. MCQ questions are graded immediately;
     * if any manual-grade questions exist the attempt parks in AWAITING_MANUAL_GRADING,
     * otherwise it is AUTO_GRADED and pass/fail is computed against the pass mark. A submission
     * arriving after the time limit (plus a small grace) expires the attempt instead of grading it.
     */
    @Transactional
    public AttemptResponse submit(String studentUuid, SubmitAttemptRequest req) {
        Assessment a = requireAssessment(req.assessmentUuid());
        Attempt attempt = attempts
                .findByStudentUuidAndAssessmentUuidOrderByCreatedAtDesc(studentUuid, req.assessmentUuid())
                .stream().filter(at -> at.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "No active attempt to submit; start the assessment first."));

        if (isElapsed(attempt)) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setSubmittedAt(Instant.now());
            attempts.save(attempt);
            throw new BusinessRuleException("Time limit exceeded; this attempt has expired.");
        }

        Map<String, Question> byUuid = a.getQuestions().stream()
                .collect(Collectors.toMap(Question::getUuid, Function.identity()));

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
        Attempt saved = attempts.save(attempt);
        maybePublishPass(a, saved);
        return toAttemptResponse(saved);
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
        Attempt saved = attempts.save(attempt);
        if (allGraded) maybePublishPass(a, saved);
        return toAttemptResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttemptSummary> listAwaitingGrading() {
        return attempts.findByStatus(AttemptStatus.AWAITING_MANUAL_GRADING).stream()
                .map(at -> {
                    Assessment a = assessments.findByUuid(at.getAssessmentUuid()).orElse(null);
                    return new AttemptSummary(at.getUuid(), at.getAssessmentUuid(),
                            a == null ? "" : a.getTitle(), at.getStudentUuid(),
                            at.getStatus().name(), at.getSubmittedAt());
                }).toList();
    }

    @Transactional(readOnly = true)
    public AttemptGradingView gradingView(String attemptUuid) {
        Attempt at = attempts.findByUuid(attemptUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptUuid));
        Assessment a = requireAssessment(at.getAssessmentUuid());
        Map<String, Question> byUuid = a.getQuestions().stream()
                .collect(Collectors.toMap(Question::getUuid, Function.identity()));
        List<AnswerToGrade> answers = at.getAnswers().stream().map(ans -> {
            Question q = byUuid.get(ans.getQuestionUuid());
            String prompt = q == null ? "(unknown question)" : q.getPrompt();
            String type = q == null ? "" : q.getType().name();
            int maxPoints = q == null ? 0 : q.getPoints();
            boolean autoGraded = q != null && q.getType().isAutoGradable();
            String selectedText = selectedOptionText(q, ans);
            return new AnswerToGrade(ans.getQuestionUuid(), prompt, type, maxPoints,
                    ans.getResponse(), selectedText, ans.getAwardedPoints(),
                    ans.getGraderFeedback(), autoGraded);
        }).toList();
        return new AttemptGradingView(at.getUuid(), at.getAssessmentUuid(), a.getTitle(),
                at.getStudentUuid(), at.getStatus().name(), at.getScorePercent(),
                at.getPassed(), a.getPassMark(), answers);
    }

    // ---- student-facing results ----

    /** The student's latest attempt for an assessment, with their own answers and feedback (never correct flags). */
    @Transactional(readOnly = true)
    public MyAttemptResult myLatestResult(String studentUuid, String assessmentUuid) {
        Assessment a = requireAssessment(assessmentUuid);
        Attempt at = attempts.findByStudentUuidAndAssessmentUuidOrderByCreatedAtDesc(studentUuid, assessmentUuid)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Attempt for assessment", assessmentUuid));
        return toMyResult(a, at);
    }

    /** Every attempt the student has made, newest first. */
    @Transactional(readOnly = true)
    public List<MyAttemptSummary> myHistory(String studentUuid) {
        Map<String, Assessment> cache = new HashMap<>();
        return attempts.findByStudentUuidOrderByCreatedAtDesc(studentUuid).stream().map(at -> {
            Assessment a = cache.computeIfAbsent(at.getAssessmentUuid(),
                    u -> assessments.findByUuid(u).orElse(null));
            return new MyAttemptSummary(at.getUuid(), at.getAssessmentUuid(),
                    a == null ? "" : a.getTitle(), a == null ? "" : a.getType().name(),
                    at.getStatus().name(), at.getScorePercent(), at.getPassed(), at.getSubmittedAt());
        }).toList();
    }

    // ---- internals ----

    private MyAttemptResult toMyResult(Assessment a, Attempt at) {
        Map<String, Question> byUuid = a.getQuestions().stream()
                .collect(Collectors.toMap(Question::getUuid, Function.identity()));
        List<MyAnswerResult> answers = at.getAnswers().stream().map(ans -> {
            Question q = byUuid.get(ans.getQuestionUuid());
            String prompt = q == null ? "(unknown question)" : q.getPrompt();
            String type = q == null ? "" : q.getType().name();
            int maxPoints = q == null ? 0 : q.getPoints();
            return new MyAnswerResult(ans.getQuestionUuid(), prompt, type, maxPoints,
                    ans.getResponse(), selectedOptionText(q, ans),
                    ans.getAwardedPoints(), ans.getGraderFeedback());
        }).toList();
        return new MyAttemptResult(at.getUuid(), a.getUuid(), a.getTitle(), at.getStatus().name(),
                at.getScorePercent(), at.getPassed(), a.getPassMark(),
                at.getSubmittedAt(), at.getExpiresAt(), answers);
    }

    private String selectedOptionText(Question q, AttemptAnswer ans) {
        if (q == null || ans.getSelectedOptionUuid() == null) return null;
        return q.getOptions().stream()
                .filter(o -> o.getUuid().equals(ans.getSelectedOptionUuid()))
                .map(AnswerOption::getText).findFirst().orElse(null);
    }

    /**
     * Compute retake eligibility, first marking any elapsed in-progress attempt as EXPIRED so it
     * neither blocks a resume nor escapes the consumed-attempt count.
     */
    private AttemptEligibility computeEligibility(String studentUuid, Assessment a) {
        List<Attempt> all = new ArrayList<>(
                attempts.findByStudentUuidAndAssessmentUuidOrderByCreatedAtDesc(studentUuid, a.getUuid()));
        for (Attempt at : all) {
            if (at.getStatus() == AttemptStatus.IN_PROGRESS && isElapsed(at)) {
                at.setStatus(AttemptStatus.EXPIRED);
                at.setSubmittedAt(Instant.now());
                attempts.save(at);
            }
        }

        Attempt active = all.stream()
                .filter(at -> at.getStatus() == AttemptStatus.IN_PROGRESS).findFirst().orElse(null);
        boolean alreadyPassed = all.stream().anyMatch(at -> Boolean.TRUE.equals(at.getPassed()));
        int used = (int) all.stream().filter(at -> at.getStatus() != AttemptStatus.IN_PROGRESS).count();
        Integer max = a.getMaxAttempts();
        Integer remaining = max == null ? null : Math.max(0, max - used);

        Instant cooldownUntil = null;
        if (a.getCooldownMinutes() != null && a.getCooldownMinutes() > 0) {
            Instant lastDone = all.stream()
                    .filter(at -> at.getStatus() != AttemptStatus.IN_PROGRESS)
                    .map(this::completionTime).filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null);
            if (lastDone != null) cooldownUntil = lastDone.plus(Duration.ofMinutes(a.getCooldownMinutes()));
        }

        boolean canAttempt;
        String reason;
        if (active != null) {
            canAttempt = true;  // resumable
            reason = null;
        } else if (alreadyPassed) {
            canAttempt = false;
            reason = "You have already passed this assessment.";
        } else if (max != null && used >= max) {
            canAttempt = false;
            reason = "No attempts remaining (used " + used + " of " + max + ").";
        } else if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) {
            canAttempt = false;
            reason = "Please wait before retrying; the cooldown ends at " + cooldownUntil + ".";
        } else {
            canAttempt = true;
            reason = null;
        }

        return new AttemptEligibility(a.getUuid(), canAttempt, reason, used, max, remaining,
                alreadyPassed, cooldownUntil,
                active != null, active == null ? null : active.getUuid(),
                active == null ? null : active.getExpiresAt());
    }

    private Instant completionTime(Attempt at) {
        return at.getSubmittedAt() != null ? at.getSubmittedAt() : at.getExpiresAt();
    }

    private boolean isElapsed(Attempt at) {
        return at.getExpiresAt() != null && Instant.now().isAfter(at.getExpiresAt().plus(GRACE));
    }

    /** On a passing EXAM attempt, nudge certification to (idempotently) evaluate course completion. */
    private void maybePublishPass(Assessment a, Attempt attempt) {
        if (a.getType() == AssessmentType.EXAM && Boolean.TRUE.equals(attempt.getPassed())) {
            events.publishEvent(new CourseCompletionCandidateEvent(
                    attempt.getStudentUuid(), a.getCourseUuid(), a.getUuid()));
        }
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
                a.getTitle(), a.getType().name(), a.getPassMark(), a.getTimeLimitMinutes(),
                a.getMaxAttempts(), a.getCooldownMinutes());
    }

    private QuestionView toQuestionView(Question q) {
        List<OptionView> opts = q.getOptions().stream()
                .map(o -> new OptionView(o.getUuid(), o.getText())).toList();
        return new QuestionView(q.getUuid(), q.getPrompt(), q.getType().name(), q.getPoints(), opts);
    }

    private AttemptResponse toAttemptResponse(Attempt at) {
        return new AttemptResponse(at.getUuid(), at.getAssessmentUuid(), at.getStudentUuid(),
                at.getStatus().name(), at.getScorePercent(), at.getPassed(), at.getExpiresAt());
    }
}
