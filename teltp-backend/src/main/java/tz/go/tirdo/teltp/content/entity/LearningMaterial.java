package tz.go.tirdo.teltp.content.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

/** Content is stored by reference (storage key), never as a DB blob. SCORM/xAPI is a documented seam, not built. */
@Getter
@Setter
@Entity
@Table(name = "learning_materials")
public class LearningMaterial extends BaseEntity {

    /** Lesson this asset belongs to (lesson uuid; resolved via catalog hook). */
    @Column(nullable = false, length = 36)
    private String lessonUuid;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaterialType type;

    /** Opaque storage key (filesystem path or S3 object key) — the storage backend is a seam. */
    @Column(nullable = false, length = 500)
    private String storageKey;

    private Long sizeBytes;

    @Column(length = 100)
    private String mimeType;

    /** Reserved for future SCORM/xAPI package metadata. Null in v1. */
    @Column(length = 40)
    private String scormPackageId;
}
