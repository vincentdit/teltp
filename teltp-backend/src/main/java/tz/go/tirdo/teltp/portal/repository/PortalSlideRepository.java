package tz.go.tirdo.teltp.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.portal.entity.PortalSlide;

import java.util.List;
import java.util.Optional;

public interface PortalSlideRepository extends JpaRepository<PortalSlide, Long> {
    Optional<PortalSlide> findByUuidAndDeletedFalse(String uuid);

    List<PortalSlide> findByDeletedFalseAndActiveTrueOrderBySortOrderAscTitleAsc();

    List<PortalSlide> findByDeletedFalseOrderBySortOrderAscTitleAsc();

    long countByDeletedFalse();
}
