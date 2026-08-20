package tz.go.tirdo.teltp.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AssessmentDtos {
    private AssessmentDtos() {}

    public record CreateAssessmentRequest(
            @NotBlank String courseUuid, @NotBlank String title, @NotNull String type,
            int passMark, Integer timeLimitMinutes, String pricingPlanUuid) {}

    public record OptionRequest(@NotBlank String text, boolean correct) {}

    public record AddQuestionRequest(
            @NotBlank String assessmentUuid, @NotBlank String prompt,
            @NotNull String type, int points, List<OptionRequest> options) {}

    public record AssessmentResponse(String uuid, String referenceNumber, String courseUuid,
                                     String title, String type, int passMark, Integer timeLimitMinutes) {}

    // Student-facing question view never exposes which option is correct.
    public record OptionView(String uuid, String text) {}
    public record QuestionView(String uuid, String prompt, String type, int points, List<OptionView> options) {}
    public record AssessmentView(String uuid, String title, String type, int passMark,
                                 Integer timeLimitMinutes, List<QuestionView> questions) {}

    public record SubmittedAnswer(@NotBlank String questionUuid, String selectedOptionUuid, String response) {}
    public record SubmitAttemptRequest(@NotBlank String assessmentUuid, List<SubmittedAnswer> answers) {}

    public record GradeAnswerRequest(@NotBlank String attemptUuid, @NotBlank String questionUuid,
                                     int awardedPoints, String feedback) {}

    public record AttemptResponse(String uuid, String assessmentUuid, String studentUuid,
                                  String status, Integer scorePercent, Boolean passed) {}
}
