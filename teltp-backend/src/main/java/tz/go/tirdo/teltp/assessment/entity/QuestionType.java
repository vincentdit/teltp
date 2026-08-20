package tz.go.tirdo.teltp.assessment.entity;

/** Per Module 5. MCQ auto-grades; ESSAY/CASE_STUDY/PRACTICAL_TASK require manual grading. */
public enum QuestionType {
    MULTIPLE_CHOICE(true),
    ESSAY(false),
    CASE_STUDY(false),
    PRACTICAL_TASK(false);

    private final boolean autoGradable;
    QuestionType(boolean autoGradable) { this.autoGradable = autoGradable; }
    public boolean isAutoGradable() { return autoGradable; }
}
