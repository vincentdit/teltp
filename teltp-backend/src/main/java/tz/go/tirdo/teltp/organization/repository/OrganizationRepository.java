package tz.go.tirdo.teltp.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.organization.entity.Organization;
import tz.go.tirdo.teltp.organization.entity.OrganizationType;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByUuid(String uuid);
    List<Organization> findByType(OrganizationType type);
}
