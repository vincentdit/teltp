package tz.go.tirdo.teltp.forum.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

/** Discussion forum scoped to a course (LMS feature). */
@Getter
@Setter
@Entity
@Table(name = "discussion_threads")
public class DiscussionThread extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String courseUuid;

    @Column(nullable = false, length = 36)
    private String authorUuid;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String body;

    private boolean pinned = false;
    private boolean locked = false;
}
