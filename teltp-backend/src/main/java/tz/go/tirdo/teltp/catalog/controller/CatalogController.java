package tz.go.tirdo.teltp.catalog.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.catalog.dto.CatalogDtos.*;
import tz.go.tirdo.teltp.catalog.service.CategoryService;
import tz.go.tirdo.teltp.catalog.service.CourseService;
import tz.go.tirdo.teltp.catalog.service.CurriculumService;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.common.PageResponse;

import java.util.List;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CategoryService categories;
    private final CourseService courses;
    private final CurriculumService curriculum;

    public CatalogController(CategoryService categories, CourseService courses, CurriculumService curriculum) {
        this.categories = categories;
        this.courses = courses;
        this.curriculum = curriculum;
    }

    // --- categories ---
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        return ApiResponse.ok(categories.create(req));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categoryTree() {
        return ApiResponse.ok(categories.tree());
    }

    // --- courses (GET is public per SecurityConfig) ---
    @GetMapping("/courses")
    public ApiResponse<PageResponse<CourseResponse>> listCourses(Pageable pageable) {
        return ApiResponse.ok(courses.listPublished(pageable));
    }

    @GetMapping("/courses/{uuid}")
    public ApiResponse<CourseResponse> getCourse(@PathVariable String uuid) {
        return ApiResponse.ok(courses.get(uuid));
    }

    @GetMapping("/courses/{uuid}/curriculum")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseCurriculumResponse> curriculum(@PathVariable String uuid) {
        return ApiResponse.ok(curriculum.getCurriculum(uuid));
    }

    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest req) {
        return ApiResponse.ok("Course created", courses.create(req));
    }

    @PostMapping("/courses/{uuid}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<CourseResponse> transition(@PathVariable String uuid, @Valid @RequestBody TransitionRequest req) {
        return ApiResponse.ok(courses.transition(uuid, req));
    }

    // --- curriculum ---
    @PostMapping("/modules")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<ModuleResponse> addModule(@Valid @RequestBody ModuleRequest req) {
        return ApiResponse.ok(curriculum.addModule(req));
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<LessonResponse> addLesson(@Valid @RequestBody LessonRequest req) {
        return ApiResponse.ok(curriculum.addLesson(req));
    }
}
