package tz.go.tirdo.teltp.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.auth.entity.Role;
import tz.go.tirdo.teltp.auth.entity.RoleName;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
