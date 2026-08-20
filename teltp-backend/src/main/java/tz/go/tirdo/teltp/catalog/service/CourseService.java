package tz.go.tirdo.teltp.catalog.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.catalog.dto.CatalogDtos.*;
import tz.go.tirdo.teltp.catalog.entity.*;
import tz.go.tirdo.teltp.catalog.repository.CourseRepository;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courses;
    private final CategoryService categories;
    private final ReferenceNumberGenerator refGen;

    public CourseService(CourseRepository courses, CategoryService categories, ReferenceNumberGenerator refGen) {
        this.courses = courses;
        this.categories = categories;
        this.refGen = refGen;
    }

    @Transactional
    public CourseResponse create(CreateCourseRequest req) {
        Course c = new Course();
        c.setReferenceNumber(refGen.next("CRS"));
        c.setTitle(req.title());
        c.setDescription(req.description());
        c.setDeliveryMode(DeliveryMode.valueOf(req.deliveryMode()));
        c.setDurationHours(req.durationHours());
        c.setInstructorUuid(req.instructorUuid());
        c.setPricingPlanUuid(req.pricingPlanUuid());
        if (req.categoryUuid() != null) c.setCategory(categories.getEntity(req.categoryUuid()));
        if (req.prerequisiteUuids() != null) {
            Set<Course> prereqs = req.prerequisiteUuids().stream().map(this::require).collect(Collectors.toSet());
            c.setPrerequisites(prereqs);
        }
        return toResponse(courses.save(c));
    }

    @Transactional
    public CourseResponse transition(String uuid, TransitionRequest req) {
        Course c = require(uuid);
        CourseStatus target = CourseStatus.valueOf(req.targetStatus());
        if (!c.getStatus().canTransitionTo(target))
            throw new BusinessRuleException("Illegal status transition: " + c.getStatus() + " -> " + target);
        if (target == CourseStatus.PUBLISHED && c.getModules().isEmpty())
            throw new BusinessRuleException("Cannot publish a course with no modules");
        c.setStatus(target);
        return toResponse(courses.save(c));
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> listPublished(Pageable pageable) {
        return PageResponse.from(courses.findByStatus(CourseStatus.PUBLISHED, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse get(String uuid) {
        return toResponse(require(uuid));
    }

    public Course getEntity(String uuid) { return require(uuid); }

    private Course require(String uuid) {
        return courses.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Course", uuid));
    }

    private CourseResponse toResponse(Course c) {
        Set<String> prereqs = c.getPrerequisites().stream().map(Course::getUuid).collect(Collectors.toCollection(HashSet::new));
        return new CourseResponse(c.getUuid(), c.getReferenceNumber(), c.getTitle(), c.getDescription(),
                c.getCategory() == null ? null : c.getCategory().getUuid(),
                c.getDeliveryMode().name(), c.getStatus().name(), c.getDurationHours(),
                c.getInstructorUuid(), c.getPricingPlanUuid(), prereqs);
    }
}
