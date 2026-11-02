package tz.go.tirdo.teltp.portal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "portal_slides")
public class PortalSlide extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(length = 255)
    private String imageKey;

    @Column(length = 80)
    private String imageMime;

    @Column(length = 500)
    private String linkUrl;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;
}
