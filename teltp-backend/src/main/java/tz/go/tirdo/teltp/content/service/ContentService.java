package tz.go.tirdo.teltp.content.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.go.tirdo.teltp.catalog.entity.Lesson;
import tz.go.tirdo.teltp.catalog.repository.LessonRepository;
import tz.go.tirdo.teltp.content.dto.ContentDtos.*;
import tz.go.tirdo.teltp.content.entity.LearningMaterial;
import tz.go.tirdo.teltp.content.entity.MaterialType;
import tz.go.tirdo.teltp.content.repository.LearningMaterialRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.enrollment.entity.EnrollmentStatus;
import tz.go.tirdo.teltp.enrollment.repository.EnrollmentRepository;
import tz.go.tirdo.teltp.security.AccessGuard;

import java.util.List;
import java.util.Set;

@Service
public class ContentService {

    private static final long MAX_BYTES = 25L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf", "video/mp4", "image/png", "image/jpeg", "image/webp",
            "application/zip", "application/x-zip-compressed",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final LearningMaterialRepository repo;
    private final ContentStorage storage;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final AccessGuard access;

    public ContentService(LearningMaterialRepository repo, ContentStorage storage, LessonRepository lessons,
                          EnrollmentRepository enrollments, AccessGuard access) {
        this.repo = repo;
        this.storage = storage;
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.access = access;
    }

    @Transactional
    public MaterialResponse register(RegisterMaterialRequest req) {
        LearningMaterial m = new LearningMaterial();
        m.setLessonUuid(req.lessonUuid());
        m.setTitle(req.title());
        m.setType(MaterialType.valueOf(req.type()));
        m.setStorageKey(req.storageKey());
        m.setSizeBytes(req.sizeBytes());
        m.setMimeType(req.mimeType());
        return toResponse(repo.save(m));
    }

    @Transactional
    public MaterialResponse upload(String lessonUuid, String title, String type, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("Choose a file to upload");
        if (file.getSize() > MAX_BYTES) throw new BusinessRuleException("File exceeds the 25 MB limit");
        String mime = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED.contains(mime)) throw new BusinessRuleException("File type is not allowed");
        MaterialType materialType = MaterialType.valueOf(type);
        String safeName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        try {
            String key = storage.store(safeName, file.getBytes(), mime);
            LearningMaterial m = new LearningMaterial();
            m.setLessonUuid(lessonUuid);
            m.setTitle(title);
            m.setType(materialType);
            m.setStorageKey(key);
            m.setSizeBytes(file.getSize());
            m.setMimeType(mime);
            if (materialType == MaterialType.SCORM) m.setScormPackageId(key);
            return toResponse(repo.save(m));
        } catch (java.io.IOException e) {
            throw new BusinessRuleException("Could not read the uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public LearningMaterial loadForDownload(String uuid) {
        LearningMaterial material = repo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("LearningMaterial", uuid));
        if (access.hasAnyRole("ADMIN", "INSTRUCTOR")) return material;
        Lesson lesson = lessons.findByUuid(material.getLessonUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", material.getLessonUuid()));
        String courseUuid = lesson.getModule().getCourse().getUuid();
        String student = access.currentUserUuid();
        boolean enrolled = enrollments.findByCourseUuidAndStudentUuidAndStatus(courseUuid, student, EnrollmentStatus.ACTIVE).isPresent()
                || enrollments.findByCourseUuidAndStudentUuidAndStatus(courseUuid, student, EnrollmentStatus.COMPLETED).isPresent();
        if (!enrolled) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        return material;
    }

    public byte[] bytes(LearningMaterial material) {
        return storage.retrieve(material.getStorageKey());
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> forLesson(String lessonUuid) {
        return repo.findByLessonUuid(lessonUuid).stream().map(this::toResponse).toList();
    }

    private MaterialResponse toResponse(LearningMaterial m) {
        return new MaterialResponse(m.getUuid(), m.getLessonUuid(), m.getTitle(), m.getType().name(),
                m.getStorageKey(), m.getSizeBytes(), m.getMimeType(), m.getScormPackageId());
    }
}
