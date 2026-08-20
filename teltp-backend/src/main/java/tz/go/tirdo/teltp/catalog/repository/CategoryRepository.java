package tz.go.tirdo.teltp.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.teltp.catalog.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByUuid(String uuid);
    List<Category> findByParentIsNull();
    List<Category> findByParentId(Long parentId);
}
