package tz.go.tirdo.teltp.catalog.entity;

/** Publish/draft state machine: DRAFT -> PUBLISHED -> ARCHIVED (and PUBLISHED -> DRAFT for unpublish). */
public enum CourseStatus {
    DRAFT, PUBLISHED, ARCHIVED;

    public boolean canTransitionTo(CourseStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED || target == ARCHIVED;
            case PUBLISHED -> target == DRAFT || target == ARCHIVED;
            case ARCHIVED -> target == DRAFT;
        };
    }
}
