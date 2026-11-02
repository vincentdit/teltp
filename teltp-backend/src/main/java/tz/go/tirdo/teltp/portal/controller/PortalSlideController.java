package tz.go.tirdo.teltp.portal.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.go.tirdo.teltp.common.ApiResponse;
import tz.go.tirdo.teltp.portal.dto.PortalDtos.SlideResponse;
import tz.go.tirdo.teltp.portal.entity.PortalSlide;
import tz.go.tirdo.teltp.portal.service.PortalSlideService;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/portal/slides")
public class PortalSlideController {

    private final PortalSlideService slides;

    public PortalSlideController(PortalSlideService slides) {
        this.slides = slides;
    }

    @GetMapping
    public ApiResponse<List<SlideResponse>> published() {
        return ApiResponse.ok(slides.published());
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SlideResponse>> manage() {
        return ApiResponse.ok(slides.manage());
    }

    @GetMapping("/{uuid}/image")
    public ResponseEntity<byte[]> image(@PathVariable String uuid) {
        PortalSlide slide = slides.image(uuid);
        MediaType type = slide.getImageMime() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(slide.getImageMime());
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(slides.bytes(slide));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SlideResponse> create(@RequestParam String title,
                                             @RequestParam(required = false) String body,
                                             @RequestParam(required = false) String linkUrl,
                                             @RequestParam(defaultValue = "0") int sortOrder,
                                             @RequestParam(defaultValue = "true") boolean active,
                                             @RequestParam(required = false) MultipartFile image) {
        return ApiResponse.ok("Slide added", slides.create(title, body, linkUrl, sortOrder, active, image));
    }

    @PutMapping(value = "/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SlideResponse> update(@PathVariable String uuid,
                                             @RequestParam String title,
                                             @RequestParam(required = false) String body,
                                             @RequestParam(required = false) String linkUrl,
                                             @RequestParam(defaultValue = "0") int sortOrder,
                                             @RequestParam(defaultValue = "true") boolean active,
                                             @RequestParam(defaultValue = "false") boolean removeImage,
                                             @RequestParam(required = false) MultipartFile image) {
        return ApiResponse.ok("Slide updated",
                slides.update(uuid, title, body, linkUrl, sortOrder, active, image, removeImage));
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String uuid) {
        slides.delete(uuid);
        return ApiResponse.ok("Slide removed", null);
    }
}
