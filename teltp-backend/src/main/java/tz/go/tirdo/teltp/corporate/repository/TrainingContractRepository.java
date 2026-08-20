package tz.go.tirdo.teltp.corporate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.corporate.entity.TrainingContract;

import java.util.Optional;

public interface TrainingContractRepository extends JpaRepository<TrainingContract, Long> {
    Optional<TrainingContract> findByUuid(String uuid);
    Page<TrainingContract> findByOrganizationUuid(String organizationUuid, Pageable pageable);
}
