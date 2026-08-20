package tz.go.tirdo.teltp.forum.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "discussion_posts")
public class DiscussionPost extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String threadUuid;

    @Column(nullable = false, length = 36)
    private String authorUuid;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String body;
}
