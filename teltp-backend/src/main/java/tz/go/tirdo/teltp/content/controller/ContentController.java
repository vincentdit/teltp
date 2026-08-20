package tz.go.tirdo.teltp.content.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.content.dto.ContentDtos.*;
import tz.go.tirdo.teltp.content.service.ContentService;

import java.util.List;

@RestController
@RequestMapping("/content")
public class ContentController {

    private final ContentService service;

    public ContentController(ContentService service) {
        this.service = service;
    }

    @PostMapping("/materials")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<MaterialResponse> register(@Valid @RequestBody RegisterMaterialRequest req) {
        return ApiResponse.ok("Material registered", service.register(req));
    }

    @GetMapping("/lessons/{lessonUuid}/materials")
    public ApiResponse<List<MaterialResponse>> forLesson(@PathVariable String lessonUuid) {
        return ApiResponse.ok(service.forLesson(lessonUuid));
    }
}
