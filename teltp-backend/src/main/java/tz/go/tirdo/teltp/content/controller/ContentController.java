package tz.go.tirdo.teltp.content.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.content.dto.ContentDtos.*;
import tz.go.tirdo.teltp.content.entity.LearningMaterial;
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

    @PostMapping(value = "/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ApiResponse<MaterialResponse> upload(@RequestParam String lessonUuid,
                                                @RequestParam String title,
                                                @RequestParam String type,
                                                @RequestParam MultipartFile file) {
        return ApiResponse.ok("Material uploaded", service.upload(lessonUuid, title, type, file));
    }

    @GetMapping("/materials/{uuid}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@PathVariable String uuid) {
        LearningMaterial material = service.loadForDownload(uuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getTitle().replace("\"", "") + "\"")
                .contentType(MediaType.parseMediaType(
                        material.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : material.getMimeType()))
                .body(service.bytes(material));
    }

    @GetMapping("/lessons/{lessonUuid}/materials")
    public ApiResponse<List<MaterialResponse>> forLesson(@PathVariable String lessonUuid) {
        return ApiResponse.ok(service.forLesson(lessonUuid));
    }
}
