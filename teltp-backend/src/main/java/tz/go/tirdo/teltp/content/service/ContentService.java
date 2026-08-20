package tz.go.tirdo.teltp.content.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.content.dto.ContentDtos.*;
import tz.go.tirdo.teltp.content.entity.LearningMaterial;
import tz.go.tirdo.teltp.content.entity.MaterialType;
import tz.go.tirdo.teltp.content.repository.LearningMaterialRepository;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class ContentService {

    private final LearningMaterialRepository repo;

    public ContentService(LearningMaterialRepository repo) {
        this.repo = repo;
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

    @Transactional(readOnly = true)
    public List<MaterialResponse> forLesson(String lessonUuid) {
        return repo.findByLessonUuid(lessonUuid).stream().map(this::toResponse).toList();
    }

    private MaterialResponse toResponse(LearningMaterial m) {
        return new MaterialResponse(m.getUuid(), m.getLessonUuid(), m.getTitle(), m.getType().name(),
                m.getStorageKey(), m.getSizeBytes(), m.getMimeType());
    }
}
