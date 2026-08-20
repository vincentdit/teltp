package tz.go.tirdo.teltp.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.catalog.dto.CatalogDtos.*;
import tz.go.tirdo.teltp.catalog.entity.Category;
import tz.go.tirdo.teltp.catalog.repository.CategoryRepository;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class CategoryService {

    private static final int MAX_DEPTH = 3;  // guard; today's data is 2 levels
    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest req) {
        Category c = new Category();
        c.setName(req.name());
        c.setDescription(req.description());
        if (req.parentUuid() != null) {
            Category parent = require(req.parentUuid());
            if (parent.depth() + 1 > MAX_DEPTH)
                throw new BusinessRuleException("Category nesting exceeds maximum depth of " + MAX_DEPTH);
            c.setParent(parent);
        }
        return toResponse(repo.save(c), false);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> tree() {
        return repo.findByParentIsNull().stream().map(c -> toResponse(c, true)).toList();
    }

    public Category getEntity(String uuid) { return require(uuid); }

    private Category require(String uuid) {
        return repo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Category", uuid));
    }

    private CategoryResponse toResponse(Category c, boolean withChildren) {
        List<CategoryResponse> children = withChildren
                ? c.getChildren().stream().map(ch -> toResponse(ch, true)).toList()
                : List.of();
        return new CategoryResponse(c.getUuid(), c.getName(), c.getDescription(),
                c.getParent() == null ? null : c.getParent().getUuid(), children);
    }
}
