package tz.go.tirdo.teltp.portal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.content.service.ContentStorage;
import tz.go.tirdo.teltp.portal.dto.PortalDtos.SlideResponse;
import tz.go.tirdo.teltp.portal.entity.PortalSlide;
import tz.go.tirdo.teltp.portal.repository.PortalSlideRepository;

import java.util.List;
import java.util.Set;

@Service
public class PortalSlideService {

    private static final int MAX_SLIDES = 20;
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> IMAGES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PortalSlideRepository slides;
    private final ContentStorage storage;

    public PortalSlideService(PortalSlideRepository slides, ContentStorage storage) {
        this.slides = slides;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<SlideResponse> published() {
        return slides.findByDeletedFalseAndActiveTrueOrderBySortOrderAscTitleAsc().stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SlideResponse> manage() {
        return slides.findByDeletedFalseOrderBySortOrderAscTitleAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SlideResponse create(String title, String body, String linkUrl, int sortOrder, boolean active,
                                MultipartFile image) {
        if (slides.countByDeletedFalse() >= MAX_SLIDES) {
            throw new BusinessRuleException("You can publish up to 20 slides. Remove one before adding another.");
        }
        PortalSlide slide = new PortalSlide();
        apply(slide, title, body, linkUrl, sortOrder, active);
        storeImage(slide, image, false);
        return toResponse(slides.save(slide));
    }

    @Transactional
    public SlideResponse update(String uuid, String title, String body, String linkUrl, int sortOrder, boolean active,
                                MultipartFile image, boolean removeImage) {
        PortalSlide slide = require(uuid);
        apply(slide, title, body, linkUrl, sortOrder, active);
        if (removeImage) clearImage(slide);
        storeImage(slide, image, true);
        return toResponse(slides.save(slide));
    }

    @Transactional
    public void delete(String uuid) {
        PortalSlide slide = require(uuid);
        clearImage(slide);
        slide.setDeleted(true);
        slides.save(slide);
    }

    @Transactional(readOnly = true)
    public PortalSlide image(String uuid) {
        PortalSlide slide = require(uuid);
        if (slide.getImageKey() == null) throw new ResourceNotFoundException("Slide image", uuid);
        return slide;
    }

    public byte[] bytes(PortalSlide slide) {
        return storage.retrieve(slide.getImageKey());
    }

    private PortalSlide require(String uuid) {
        return slides.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Slide", uuid));
    }

    private void apply(PortalSlide slide, String title, String body, String linkUrl, int sortOrder, boolean active) {
        if (title == null || title.isBlank() || title.trim().length() > 160) {
            throw new BusinessRuleException("Title is required and must be 160 characters or fewer");
        }
        if (body != null && body.trim().length() > 500) {
            throw new BusinessRuleException("The message must be 500 characters or fewer");
        }
        slide.setTitle(title.trim());
        slide.setBody(body == null || body.isBlank() ? null : body.trim());
        slide.setLinkUrl(cleanLink(linkUrl));
        slide.setSortOrder(sortOrder);
        slide.setActive(active);
    }

    private String cleanLink(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String link = raw.trim();
        boolean sitePath = link.startsWith("/") && !link.startsWith("//");
        boolean web = link.startsWith("https://") || link.startsWith("http://");
        if (!sitePath && !web) {
            throw new BusinessRuleException("Link must start with http://, https://, or /");
        }
        if (link.length() > 500) throw new BusinessRuleException("Link is too long");
        return link;
    }

    private void storeImage(PortalSlide slide, MultipartFile image, boolean replacing) {
        if (image == null || image.isEmpty()) return;
        String mime = image.getContentType() == null ? "" : image.getContentType();
        if (!IMAGES.contains(mime)) throw new BusinessRuleException("Use a JPEG, PNG, or WebP image");
        if (image.getSize() > MAX_BYTES) throw new BusinessRuleException("Image must be 5 MB or smaller");
        if (replacing) clearImage(slide);
        try {
            String name = image.getOriginalFilename() == null ? "banner" : image.getOriginalFilename();
            slide.setImageKey(storage.store(name, image.getBytes(), mime));
            slide.setImageMime(mime);
        } catch (java.io.IOException e) {
            throw new BusinessRuleException("Could not read the image");
        }
    }

    private void clearImage(PortalSlide slide) {
        if (slide.getImageKey() != null) storage.delete(slide.getImageKey());
        slide.setImageKey(null);
        slide.setImageMime(null);
    }

    private SlideResponse toResponse(PortalSlide slide) {
        return new SlideResponse(
                slide.getUuid(),
                slide.getTitle(),
                slide.getBody(),
                slide.getLinkUrl(),
                slide.getSortOrder(),
                slide.isActive(),
                slide.getImageKey() != null,
                slide.getUpdatedAt() == null ? 0L : slide.getUpdatedAt().toEpochMilli());
    }
}
