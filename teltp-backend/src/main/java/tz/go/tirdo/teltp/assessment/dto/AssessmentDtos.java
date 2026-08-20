package tz.go.tirdo.teltp.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class AssessmentDtos {
    private AssessmentDtos() {}

    public record CreateAssessmentRequest(
            @NotBlank String courseUuid, @NotBlank String title, @NotNull String type,
            int passMark, Integer timeLimitMinutes,
            Integer maxAttempts, Integer cooldownMinutes,
            String pricingPlanUuid) {}

    public record OptionRequest(@NotBlank String text, boolean correct) {}

    public record AddQuestionRequest(
            @NotBlank String assessmentUuid, @NotBlank String prompt,
            @NotNull String type, int points, List<OptionRequest> options) {}

    public record AssessmentResponse(String uuid, String referenceNumber, String courseUuid,
                                     String title, String type, int passMark, Integer timeLimitMinutes,
                                     Integer maxAttempts, Integer cooldownMinutes) {}

    // Student-facing question view never exposes which option is correct.
    public record OptionView(String uuid, String text) {}
    public record QuestionView(String uuid, String prompt, String type, int points, List<OptionView> options) {}
    public record AssessmentView(String uuid, String title, String type, int passMark,
                                 Integer timeLimitMinutes, List<QuestionView> questions) {}

    public record SubmittedAnswer(@NotBlank String questionUuid, String selectedOptionUuid, String response) {}
    public record SubmitAttemptRequest(@NotBlank String assessmentUuid, List<SubmittedAnswer> answers) {}

    public record GradeAnswerRequest(@NotBlank String attemptUuid, @NotBlank String questionUuid,
                                     int awardedPoints, String feedback) {}

    /** Attempt state returned on start/submit. {@code expiresAt} is null for untimed assessments. */
    public record AttemptResponse(String uuid, String assessmentUuid, String studentUuid,
                                  String status, Integer scorePercent, Boolean passed, Instant expiresAt) {}

    /** Whether a student may (re)take an assessment, and why not when they cannot. */
    public record AttemptEligibility(String assessmentUuid, boolean canAttempt, String reason,
                                     int attemptsUsed, Integer maxAttempts, Integer attemptsRemaining,
                                     boolean alreadyPassed, Instant cooldownUntil,
                                     boolean hasActiveAttempt, String activeAttemptUuid, Instant activeExpiresAt) {}

    // ---- student-facing results ----
    public record MyAnswerResult(String questionUuid, String prompt, String type, int maxPoints,
                                 String yourResponse, String yourSelectedOptionText,
                                 Integer awardedPoints, String feedback) {}
    public record MyAttemptResult(String uuid, String assessmentUuid, String assessmentTitle,
                                  String status, Integer scorePercent, Boolean passed, int passMark,
                                  Instant submittedAt, Instant expiresAt, List<MyAnswerResult> answers) {}
    public record MyAttemptSummary(String uuid, String assessmentUuid, String assessmentTitle,
                                   String type, String status, Integer scorePercent, Boolean passed,
                                   Instant submittedAt) {}

    // ---- instructor grading ----
    public record AttemptSummary(String uuid, String assessmentUuid, String assessmentTitle,
                                 String studentUuid, String status, java.time.Instant submittedAt) {}
    public record AnswerToGrade(String questionUuid, String prompt, String type, int maxPoints,
                                String response, String selectedOptionText,
                                Integer awardedPoints, String graderFeedback, boolean autoGraded) {}
    public record AttemptGradingView(String uuid, String assessmentUuid, String assessmentTitle,
                                     String studentUuid, String status, Integer scorePercent,
                                     Boolean passed, int passMark, List<AnswerToGrade> answers) {}
}
