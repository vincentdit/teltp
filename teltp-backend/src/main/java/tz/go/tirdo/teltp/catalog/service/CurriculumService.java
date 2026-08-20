package tz.go.tirdo.teltp.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.catalog.dto.CatalogDtos.*;
import tz.go.tirdo.teltp.catalog.entity.CourseModule;
import tz.go.tirdo.teltp.catalog.entity.Lesson;
import tz.go.tirdo.teltp.catalog.repository.CourseRepository;
import tz.go.tirdo.teltp.catalog.repository.LessonRepository;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;

import java.util.List;

/** Manages modules and lessons within a course's curriculum. */
@Service
public class CurriculumService {

    private final CourseService courseService;
    private final CourseRepository courses;
    private final LessonRepository lessons;

    public CurriculumService(CourseService courseService, CourseRepository courses, LessonRepository lessons) {
        this.courseService = courseService;
        this.courses = courses;
        this.lessons = lessons;
    }

    @Transactional
    public ModuleResponse addModule(ModuleRequest req) {
        var course = courseService.getEntity(req.courseUuid());
        CourseModule m = new CourseModule();
        m.setCourse(course);
        m.setTitle(req.title());
        m.setOrderIndex(req.orderIndex());
        course.getModules().add(m);
        courses.save(course);
        return new ModuleResponse(m.getUuid(), m.getTitle(), m.getOrderIndex(), List.of());
    }

    @Transactional
    public LessonResponse addLesson(LessonRequest req) {
        var course = courses.findAll().stream()
                .flatMap(c -> c.getModules().stream())
                .filter(mod -> mod.getUuid().equals(req.moduleUuid()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CourseModule", req.moduleUuid()));
        Lesson l = new Lesson();
        l.setModule(course);
        l.setTitle(req.title());
        l.setContent(req.content());
        l.setOrderIndex(req.orderIndex());
        l.setEstimatedMinutes(req.estimatedMinutes());
        l.setMandatory(req.mandatory());
        Lesson saved = lessons.save(l);
        return new LessonResponse(saved.getUuid(), saved.getTitle(), saved.getOrderIndex(),
                saved.getEstimatedMinutes(), saved.isMandatory());
    }

    public Lesson getLesson(String uuid) {
        return lessons.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Lesson", uuid));
    }

    /** Full curriculum (modules + lessons, ordered) for the course-player. */
    @Transactional(readOnly = true)
    public CourseCurriculumResponse getCurriculum(String courseUuid) {
        var course = courseService.getEntity(courseUuid);
        var modules = course.getModules().stream()
                .map(m -> new CurriculumModule(
                        m.getUuid(), m.getTitle(), m.getOrderIndex(),
                        m.getLessons().stream()
                                .map(l -> new CurriculumLesson(
                                        l.getUuid(), l.getTitle(), l.getOrderIndex(),
                                        l.getEstimatedMinutes(), l.isMandatory(), l.getContent()))
                                .toList()))
                .toList();
        return new CourseCurriculumResponse(course.getUuid(), course.getTitle(), modules);
    }
}
