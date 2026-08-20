package tz.go.tirdo.teltp.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private OrganizationSubType subType;

    @Column(length = 150)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String district;

    @Column(length = 50)
    private String tin;  // Tax Identification Number for B2B invoicing
}
