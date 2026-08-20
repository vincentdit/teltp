package tz.go.tirdo.teltp.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tz.go.tirdo.teltp.assessment.dto.AssessmentDtos.*;
import tz.go.tirdo.teltp.assessment.entity.*;
import tz.go.tirdo.teltp.assessment.repository.AssessmentRepository;
import tz.go.tirdo.teltp.assessment.repository.AttemptRepository;
import tz.go.tirdo.teltp.assessment.repository.QuestionRepository;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.event.CourseCompletionCandidateEvent;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the assessment-taking rules added to the flow: attempt limits,
 * retake cooldown, stale-attempt expiry, time-limit enforcement on submit,
 * auto-grading, and the pass event that drives certificate auto-issue.
 */
class AssessmentServiceTest {

    private AssessmentRepository assessments;
    private QuestionRepository questions;
    private AttemptRepository attempts;
    private ReferenceNumberGenerator refGen;
    private ApplicationEventPublisher events;
    private AssessmentService service;

    private static final String A = "A1";      // assessment uuid
    private static final String C = "C1";      // course uuid
    private static final String S = "S1";      // student uuid

    @BeforeEach
    void setUp() {
        assessments = mock(AssessmentRepository.class);
        questions = mock(QuestionRepository.class);
        attempts = mock(AttemptRepository.class);
        refGen = mock(ReferenceNumberGenerator.class);
        events = mock(ApplicationEventPublisher.class);
        service = new AssessmentService(assessments, questions, attempts, refGen, events);
        // save returns its argument
        when(attempts.save(any(Attempt.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- builders ----------

    private Assessment exam(int passMark, Integer timeLimit, Integer maxAttempts, Integer cooldown) {
        Assessment a = new Assessment();
        a.setUuid(A);
        a.setCourseUuid(C);
        a.setTitle("Final Exam");
        a.setType(AssessmentType.EXAM);
        a.setPassMark(passMark);
        a.setTimeLimitMinutes(timeLimit);
        a.setMaxAttempts(maxAttempts);
        a.setCooldownMinutes(cooldown);
        when(assessments.findByUuid(A)).thenReturn(Optional.of(a));
        return a;
    }

    private Question mcq(Assessment a, String uuid, int points, String correctOpt, String wrongOpt) {
        Question q = new Question();
        q.setUuid(uuid);
        q.setType(QuestionType.MULTIPLE_CHOICE);
        q.setPoints(points);
        q.setPrompt("prompt " + uuid);
        AnswerOption ok = new AnswerOption();  ok.setUuid(correctOpt); ok.setText("right"); ok.setCorrect(true);  ok.setQuestion(q);
        AnswerOption no = new AnswerOption();  no.setUuid(wrongOpt);   no.setText("wrong"); no.setCorrect(false); no.setQuestion(q);
        q.getOptions().add(ok); q.getOptions().add(no);
        a.getQuestions().add(q);
        return q;
    }

    private Question essay(Assessment a, String uuid, int points) {
        Question q = new Question();
        q.setUuid(uuid);
        q.setType(QuestionType.ESSAY);
        q.setPoints(points);
        q.setPrompt("essay " + uuid);
        a.getQuestions().add(q);
        return q;
    }

    private Attempt attempt(String uuid, AttemptStatus status, Instant expiresAt) {
        Attempt at = new Attempt();
        at.setUuid(uuid);
        at.setAssessmentUuid(A);
        at.setStudentUuid(S);
        at.setStatus(status);
        at.setExpiresAt(expiresAt);
        return at;
    }

    private void ordered(Attempt... list) {
        when(attempts.findByStudentUuidAndAssessmentUuidOrderByCreatedAtDesc(S, A))
                .thenReturn(List.of(list));
    }

    // ---------- start / eligibility ----------

    @Test
    void start_timedExam_setsExpiryAndInProgress() {
        exam(60, 20, 3, null);
        ordered();  // no prior attempts

        AttemptResponse r = service.startAttempt(S, A);

        assertThat(r.status()).isEqualTo("IN_PROGRESS");
        assertThat(r.expiresAt()).isNotNull();
        assertThat(r.expiresAt()).isAfter(Instant.now().plusSeconds(19 * 60));
    }

    @Test
    void start_resumesActiveAttempt_withoutConsumingAnother() {
        exam(60, 20, 3, null);
        Attempt active = attempt("ATT-ACTIVE", AttemptStatus.IN_PROGRESS, Instant.now().plusSeconds(600));
        ordered(active);
        when(attempts.findByUuid("ATT-ACTIVE")).thenReturn(Optional.of(active));

        AttemptResponse r = service.startAttempt(S, A);

        assertThat(r.uuid()).isEqualTo("ATT-ACTIVE");
        // resume must not persist a brand-new attempt
        verify(attempts, never()).save(argThat(x -> x.getUuid() == null));
    }

    @Test
    void start_blockedAfterPass() {
        exam(60, null, 3, null);
        Attempt passed = attempt("ATT-P", AttemptStatus.AUTO_GRADED, null);
        passed.setPassed(true);
        ordered(passed);

        assertThatThrownBy(() -> service.startAttempt(S, A))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already passed");
    }

    @Test
    void start_blockedWhenMaxAttemptsReached() {
        exam(60, null, 2, null);
        Attempt a1 = attempt("ATT-1", AttemptStatus.AUTO_GRADED, null); a1.setPassed(false);
        Attempt a2 = attempt("ATT-2", AttemptStatus.AUTO_GRADED, null); a2.setPassed(false);
        ordered(a1, a2);

        assertThatThrownBy(() -> service.startAttempt(S, A))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No attempts remaining");
    }

    @Test
    void start_blockedDuringCooldown() {
        exam(60, null, null, 10);
        Attempt recent = attempt("ATT-R", AttemptStatus.AUTO_GRADED, null);
        recent.setPassed(false);
        recent.setSubmittedAt(Instant.now());   // just submitted -> cooldown active
        ordered(recent);

        assertThatThrownBy(() -> service.startAttempt(S, A))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cooldown");
    }

    @Test
    void start_expiresStaleInProgress_thenAllowsNewAttempt() {
        exam(60, 20, null, null);
        Attempt stale = attempt("ATT-STALE", AttemptStatus.IN_PROGRESS, Instant.now().minusSeconds(120));
        ordered(stale);

        AttemptResponse r = service.startAttempt(S, A);

        assertThat(stale.getStatus()).isEqualTo(AttemptStatus.EXPIRED);   // marked expired
        assertThat(r.status()).isEqualTo("IN_PROGRESS");                  // fresh attempt issued
        assertThat(r.uuid()).isNotEqualTo("ATT-STALE");
    }

    @Test
    void eligibility_reportsRemainingAndAllows() {
        exam(60, null, 3, null);
        Attempt a1 = attempt("ATT-1", AttemptStatus.AUTO_GRADED, null); a1.setPassed(false);
        ordered(a1);

        AttemptEligibility e = service.eligibility(S, A);

        assertThat(e.canAttempt()).isTrue();
        assertThat(e.attemptsUsed()).isEqualTo(1);
        assertThat(e.attemptsRemaining()).isEqualTo(2);
        assertThat(e.alreadyPassed()).isFalse();
    }

    // ---------- submit ----------

    @Test
    void submit_withNoActiveAttempt_throws() {
        Assessment a = exam(60, null, null, null);
        mcq(a, "Q1", 10, "o1", "o2");
        ordered();  // nothing in progress

        assertThatThrownBy(() -> service.submit(S,
                new SubmitAttemptRequest(A, List.of(new SubmittedAnswer("Q1", "o1", null)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No active attempt");
    }

    @Test
    void submit_correctMcq_passes_andPublishesPassEvent() {
        Assessment a = exam(50, null, null, null);
        mcq(a, "Q1", 10, "o1", "o2");
        Attempt active = attempt("ATT", AttemptStatus.IN_PROGRESS, null);
        ordered(active);

        AttemptResponse r = service.submit(S,
                new SubmitAttemptRequest(A, List.of(new SubmittedAnswer("Q1", "o1", null))));

        assertThat(r.status()).isEqualTo("AUTO_GRADED");
        assertThat(r.scorePercent()).isEqualTo(100);
        assertThat(r.passed()).isTrue();

        ArgumentCaptor<CourseCompletionCandidateEvent> cap =
                ArgumentCaptor.forClass(CourseCompletionCandidateEvent.class);
        verify(events).publishEvent(cap.capture());
        assertThat(cap.getValue().studentUuid()).isEqualTo(S);
        assertThat(cap.getValue().courseUuid()).isEqualTo(C);
    }

    @Test
    void submit_wrongMcq_fails_andPublishesNothing() {
        Assessment a = exam(50, null, null, null);
        mcq(a, "Q1", 10, "o1", "o2");
        ordered(attempt("ATT", AttemptStatus.IN_PROGRESS, null));

        AttemptResponse r = service.submit(S,
                new SubmitAttemptRequest(A, List.of(new SubmittedAnswer("Q1", "o2", null))));

        assertThat(r.passed()).isFalse();
        assertThat(r.scorePercent()).isEqualTo(0);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void submit_withEssay_parksForManualGrading() {
        Assessment a = exam(50, null, null, null);
        essay(a, "Q1", 10);
        ordered(attempt("ATT", AttemptStatus.IN_PROGRESS, null));

        AttemptResponse r = service.submit(S,
                new SubmitAttemptRequest(A, List.of(new SubmittedAnswer("Q1", null, "my answer"))));

        assertThat(r.status()).isEqualTo("AWAITING_MANUAL_GRADING");
        assertThat(r.scorePercent()).isNull();
        verify(events, never()).publishEvent(any());
    }

    @Test
    void submit_afterExpiry_marksExpired_andThrows() {
        Assessment a = exam(50, 20, null, null);
        mcq(a, "Q1", 10, "o1", "o2");
        Attempt active = attempt("ATT", AttemptStatus.IN_PROGRESS, Instant.now().minusSeconds(120));
        ordered(active);

        assertThatThrownBy(() -> service.submit(S,
                new SubmitAttemptRequest(A, List.of(new SubmittedAnswer("Q1", "o1", null)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");

        assertThat(active.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        verify(events, never()).publishEvent(any());
    }

    // ---------- manual grading finalisation ----------

    @Test
    void gradeAnswer_completesGrading_andPublishesPassEvent() {
        Assessment a = exam(50, null, null, null);
        Question q = essay(a, "Q1", 10);

        Attempt at = attempt("ATT", AttemptStatus.AWAITING_MANUAL_GRADING, null);
        AttemptAnswer ans = new AttemptAnswer();
        ans.setAttempt(at);
        ans.setQuestionUuid("Q1");
        ans.setResponse("essay text");
        at.getAnswers().add(ans);
        when(attempts.findByUuid("ATT")).thenReturn(Optional.of(at));

        AttemptResponse r = service.gradeAnswer(new GradeAnswerRequest("ATT", "Q1", 10, "good"));

        assertThat(r.status()).isEqualTo("GRADED");
        assertThat(r.passed()).isTrue();
        assertThat(r.scorePercent()).isEqualTo(100);
        verify(events).publishEvent(any(CourseCompletionCandidateEvent.class));
    }
}
