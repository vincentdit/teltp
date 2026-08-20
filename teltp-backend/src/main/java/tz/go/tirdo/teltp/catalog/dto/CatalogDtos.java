package tz.go.tirdo.teltp.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record CreateCategoryRequest(@NotBlank String name, String description, String parentUuid) {}
    public record CategoryResponse(String uuid, String name, String description,
                                   String parentUuid, List<CategoryResponse> children) {}

    public record CreateCourseRequest(
            @NotBlank String title,
            String description,
            String categoryUuid,
            @NotNull String deliveryMode,
            Integer durationHours,
            String instructorUuid,
            String pricingPlanUuid,
            Set<String> prerequisiteUuids) {}

    public record CourseResponse(
            String uuid,
            String referenceNumber,
            String title,
            String description,
            String categoryUuid,
            String deliveryMode,
            String status,
            Integer durationHours,
            String instructorUuid,
            String pricingPlanUuid,
            Set<String> prerequisiteUuids) {}

    public record ModuleRequest(@NotBlank String courseUuid, @NotBlank String title, int orderIndex) {}
    public record ModuleResponse(String uuid, String title, int orderIndex, List<LessonResponse> lessons) {}

    public record LessonRequest(@NotBlank String moduleUuid, @NotBlank String title,
                                String content, int orderIndex, Integer estimatedMinutes, boolean mandatory) {}
    public record LessonResponse(String uuid, String title, int orderIndex,
                                 Integer estimatedMinutes, boolean mandatory) {}

    public record TransitionRequest(@NotNull String targetStatus) {}

    // ---- curriculum read model (for the learner course-player) ----
    public record CurriculumLesson(String uuid, String title, int orderIndex,
                                   Integer estimatedMinutes, boolean mandatory, String content) {}
    public record CurriculumModule(String uuid, String title, int orderIndex,
                                   List<CurriculumLesson> lessons) {}
    public record CourseCurriculumResponse(String courseUuid, String title,
                                           List<CurriculumModule> modules) {}
}
