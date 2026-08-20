package tz.go.tirdo.teltp.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.auth.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(String uuid);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);
}
