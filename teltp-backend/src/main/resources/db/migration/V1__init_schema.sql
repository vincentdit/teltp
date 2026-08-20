-- TeLTP initial schema. Flyway-owned; JPA runs in validate mode so this MUST match the entities.
-- Conventions: BaseEntity columns (id, uuid, created_at, updated_at, created_by, updated_by,
-- version, deleted) on every aggregate table except payments and audit_logs (which carry their own).
-- boolean -> bit, Instant -> datetime(6), LocalDate -> date, @Lob String -> longtext.

SET NAMES utf8mb4;

-- reference number sequence (driven by ReferenceNumberGenerator via raw JDBC)
CREATE TABLE reference_sequence (
    module        VARCHAR(20)  NOT NULL,
    ref_year      INT          NOT NULL,
    current_value BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (module, ref_year)
) ENGINE=InnoDB;

-- ---------- auth / organization ----------
CREATE TABLE organizations (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(36)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    version       BIGINT       DEFAULT 0,
    deleted       BIT          NOT NULL DEFAULT 0,
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(40)  NOT NULL,
    sub_type      VARCHAR(40),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(30),
    region        VARCHAR(100),
    district      VARCHAR(100),
    tin           VARCHAR(50),
    PRIMARY KEY (id),
    UNIQUE KEY uk_organizations_uuid (uuid)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT      DEFAULT 0,
    deleted     BIT         NOT NULL DEFAULT 0,
    name        VARCHAR(40) NOT NULL,
    description VARCHAR(200),
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_uuid (uuid),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB;

CREATE TABLE users (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    uuid                     VARCHAR(36)  NOT NULL,
    created_at               DATETIME(6)  NOT NULL,
    updated_at               DATETIME(6)  NOT NULL,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    version                  BIGINT       DEFAULT 0,
    deleted                  BIT          NOT NULL DEFAULT 0,
    username                 VARCHAR(100) NOT NULL,
    email                    VARCHAR(150) NOT NULL,
    password_hash            VARCHAR(255) NOT NULL,
    first_name               VARCHAR(100),
    last_name                VARCHAR(100),
    phone_number             VARCHAR(30),
    profession               VARCHAR(80),
    national_id              VARCHAR(40),
    nida_verified            BIT          NOT NULL DEFAULT 0,
    active                   BIT          NOT NULL DEFAULT 1,
    data_processing_consent  BIT          NOT NULL DEFAULT 0,
    organization_id          BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_uuid (uuid),
    UNIQUE KEY idx_users_email (email),
    UNIQUE KEY idx_users_username (username),
    CONSTRAINT fk_users_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

-- ---------- catalog ----------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       DEFAULT 0,
    deleted     BIT          NOT NULL DEFAULT 0,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(400),
    parent_id   BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_uuid (uuid),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE courses (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uuid              VARCHAR(36)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       DEFAULT 0,
    deleted           BIT          NOT NULL DEFAULT 0,
    reference_number  VARCHAR(30)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    description       LONGTEXT,
    category_id       BIGINT,
    delivery_mode     VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    duration_hours    INT,
    instructor_uuid   VARCHAR(36),
    pricing_plan_uuid VARCHAR(36),
    PRIMARY KEY (id),
    UNIQUE KEY uk_courses_uuid (uuid),
    UNIQUE KEY uk_courses_reference (reference_number),
    CONSTRAINT fk_courses_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE course_prerequisites (
    course_id       BIGINT NOT NULL,
    prerequisite_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_cp_prereq FOREIGN KEY (prerequisite_id) REFERENCES courses (id)
) ENGINE=InnoDB;

CREATE TABLE course_modules (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       DEFAULT 0,
    deleted     BIT          NOT NULL DEFAULT 0,
    course_id   BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    order_index INT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_modules_uuid (uuid),
    CONSTRAINT fk_modules_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB;

CREATE TABLE lessons (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uuid              VARCHAR(36)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       DEFAULT 0,
    deleted           BIT          NOT NULL DEFAULT 0,
    module_id         BIGINT       NOT NULL,
    title             VARCHAR(255) NOT NULL,
    content           LONGTEXT,
    order_index       INT          NOT NULL,
    estimated_minutes INT,
    mandatory         BIT          NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lessons_uuid (uuid),
    CONSTRAINT fk_lessons_module FOREIGN KEY (module_id) REFERENCES course_modules (id)
) ENGINE=InnoDB;

CREATE TABLE learning_materials (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT       DEFAULT 0,
    deleted          BIT          NOT NULL DEFAULT 0,
    lesson_uuid      VARCHAR(36)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    storage_key      VARCHAR(500) NOT NULL,
    size_bytes       BIGINT,
    mime_type        VARCHAR(100),
    scorm_package_id VARCHAR(40),
    PRIMARY KEY (id),
    UNIQUE KEY uk_materials_uuid (uuid)
) ENGINE=InnoDB;

-- ---------- enrollment ----------
CREATE TABLE cohorts (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    uuid       VARCHAR(36)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version    BIGINT       DEFAULT 0,
    deleted    BIT          NOT NULL DEFAULT 0,
    course_uuid VARCHAR(36) NOT NULL,
    name       VARCHAR(150) NOT NULL,
    start_date DATE,
    end_date   DATE,
    capacity   INT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cohorts_uuid (uuid)
) ENGINE=InnoDB;

CREATE TABLE enrollments (
    id                            BIGINT      NOT NULL AUTO_INCREMENT,
    uuid                          VARCHAR(36) NOT NULL,
    created_at                    DATETIME(6) NOT NULL,
    updated_at                    DATETIME(6) NOT NULL,
    created_by                    VARCHAR(100),
    updated_by                    VARCHAR(100),
    version                       BIGINT      DEFAULT 0,
    deleted                       BIT         NOT NULL DEFAULT 0,
    course_uuid                   VARCHAR(36) NOT NULL,
    student_uuid                  VARCHAR(36) NOT NULL,
    cohort_uuid                   VARCHAR(36),
    assigned_by_organization_uuid VARCHAR(36),
    status                        VARCHAR(20) NOT NULL,
    enrollment_date               DATETIME(6) NOT NULL,
    completed_date                DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollments_uuid (uuid),
    UNIQUE KEY uk_enrollment_course_student_cohort (course_uuid, student_uuid, cohort_uuid)
) ENGINE=InnoDB;

-- ---------- progress ----------
CREATE TABLE lesson_progress (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36) NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT      DEFAULT 0,
    deleted          BIT         NOT NULL DEFAULT 0,
    student_uuid     VARCHAR(36) NOT NULL,
    lesson_uuid      VARCHAR(36) NOT NULL,
    course_uuid      VARCHAR(36) NOT NULL,
    completed        BIT         NOT NULL DEFAULT 0,
    completed_at     DATETIME(6),
    percent_complete INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_progress_uuid (uuid),
    UNIQUE KEY uk_progress_student_lesson (student_uuid, lesson_uuid)
) ENGINE=InnoDB;

-- ---------- forum ----------
CREATE TABLE discussion_threads (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT       DEFAULT 0,
    deleted     BIT          NOT NULL DEFAULT 0,
    course_uuid VARCHAR(36)  NOT NULL,
    author_uuid VARCHAR(36)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        LONGTEXT,
    pinned      BIT          NOT NULL DEFAULT 0,
    locked      BIT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_threads_uuid (uuid)
) ENGINE=InnoDB;

CREATE TABLE discussion_posts (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT      DEFAULT 0,
    deleted     BIT         NOT NULL DEFAULT 0,
    thread_uuid VARCHAR(36) NOT NULL,
    author_uuid VARCHAR(36) NOT NULL,
    body        LONGTEXT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_posts_uuid (uuid)
) ENGINE=InnoDB;

-- ---------- assessment ----------
CREATE TABLE assessments (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    uuid               VARCHAR(36)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    version            BIGINT       DEFAULT 0,
    deleted            BIT          NOT NULL DEFAULT 0,
    reference_number   VARCHAR(30)  NOT NULL,
    course_uuid        VARCHAR(36)  NOT NULL,
    title              VARCHAR(255) NOT NULL,
    type               VARCHAR(10)  NOT NULL,
    pass_mark          INT          NOT NULL,
    time_limit_minutes INT,
    pricing_plan_uuid  VARCHAR(36),
    PRIMARY KEY (id),
    UNIQUE KEY uk_assessments_uuid (uuid),
    UNIQUE KEY uk_assessments_reference (reference_number)
) ENGINE=InnoDB;

CREATE TABLE questions (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(36) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    version       BIGINT      DEFAULT 0,
    deleted       BIT         NOT NULL DEFAULT 0,
    assessment_id BIGINT      NOT NULL,
    prompt        LONGTEXT    NOT NULL,
    type          VARCHAR(20) NOT NULL,
    points        INT         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_questions_uuid (uuid),
    CONSTRAINT fk_questions_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id)
) ENGINE=InnoDB;

CREATE TABLE answer_options (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)   NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT        DEFAULT 0,
    deleted     BIT           NOT NULL DEFAULT 0,
    question_id BIGINT        NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    correct     BIT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_options_uuid (uuid),
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB;

CREATE TABLE attempts (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    uuid            VARCHAR(36) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT      DEFAULT 0,
    deleted         BIT         NOT NULL DEFAULT 0,
    assessment_uuid VARCHAR(36) NOT NULL,
    student_uuid    VARCHAR(36) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    submitted_at    DATETIME(6),
    score_percent   INT,
    passed          BIT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempts_uuid (uuid)
) ENGINE=InnoDB;

CREATE TABLE attempt_answers (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    uuid                 VARCHAR(36) NOT NULL,
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT      DEFAULT 0,
    deleted              BIT         NOT NULL DEFAULT 0,
    attempt_id           BIGINT      NOT NULL,
    question_uuid        VARCHAR(36) NOT NULL,
    selected_option_uuid VARCHAR(36),
    response             LONGTEXT,
    awarded_points       INT,
    grader_feedback      LONGTEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempt_answers_uuid (uuid),
    CONSTRAINT fk_aa_attempt FOREIGN KEY (attempt_id) REFERENCES attempts (id)
) ENGINE=InnoDB;

-- ---------- certification ----------
CREATE TABLE certificates (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    uuid                VARCHAR(36)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT       DEFAULT 0,
    deleted             BIT          NOT NULL DEFAULT 0,
    reference_number    VARCHAR(30)  NOT NULL,
    verification_code   VARCHAR(24)  NOT NULL,
    student_uuid        VARCHAR(36)  NOT NULL,
    course_uuid         VARCHAR(36)  NOT NULL,
    recipient_name      VARCHAR(200) NOT NULL,
    course_title        VARCHAR(255) NOT NULL,
    issued_on           DATE         NOT NULL,
    expires_on          DATE,
    revoked             BIT          NOT NULL DEFAULT 0,
    accrediting_body    VARCHAR(200),
    accreditation_level VARCHAR(50),
    pdf_storage_key     VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE KEY uk_certificates_uuid (uuid),
    UNIQUE KEY uk_certificates_reference (reference_number),
    UNIQUE KEY uk_certificates_verification (verification_code)
) ENGINE=InnoDB;

-- ---------- schedule ----------
CREATE TABLE scheduled_events (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    uuid                VARCHAR(36)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT       DEFAULT 0,
    deleted             BIT          NOT NULL DEFAULT 0,
    reference_number    VARCHAR(30)  NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         LONGTEXT,
    course_uuid         VARCHAR(36),
    host_uuid           VARCHAR(36),
    starts_at           DATETIME(6)  NOT NULL,
    ends_at             DATETIME(6)  NOT NULL,
    capacity            INT,
    pricing_plan_uuid   VARCHAR(36),
    provider            VARCHAR(10)  NOT NULL,
    join_url            VARCHAR(500),
    external_meeting_id VARCHAR(100),
    recording_url       VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE KEY uk_events_uuid (uuid),
    UNIQUE KEY uk_events_reference (reference_number)
) ENGINE=InnoDB;

CREATE TABLE event_registrations (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36) NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT      DEFAULT 0,
    deleted          BIT         NOT NULL DEFAULT 0,
    event_uuid       VARCHAR(36) NOT NULL,
    participant_uuid VARCHAR(36) NOT NULL,
    confirmed        BIT         NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_reg_uuid (uuid),
    UNIQUE KEY uk_event_reg_event_participant (event_uuid, participant_uuid)
) ENGINE=InnoDB;

CREATE TABLE attendances (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36) NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT      DEFAULT 0,
    deleted          BIT         NOT NULL DEFAULT 0,
    event_uuid       VARCHAR(36) NOT NULL,
    participant_uuid VARCHAR(36) NOT NULL,
    present          BIT         NOT NULL DEFAULT 0,
    checked_in_at    DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_uuid (uuid),
    UNIQUE KEY uk_attendance_event_participant (event_uuid, participant_uuid)
) ENGINE=InnoDB;

-- ---------- corporate ----------
CREATE TABLE training_contracts (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    uuid              VARCHAR(36)    NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT         DEFAULT 0,
    deleted           BIT            NOT NULL DEFAULT 0,
    reference_number  VARCHAR(30)    NOT NULL,
    organization_uuid VARCHAR(36)    NOT NULL,
    title             VARCHAR(255)   NOT NULL,
    scope             LONGTEXT,
    status            VARCHAR(20)    NOT NULL,
    amount            DECIMAL(14,2),
    currency          VARCHAR(3),
    participant_target INT,
    start_date        DATE,
    end_date          DATE,
    invoice_uuid      VARCHAR(36),
    PRIMARY KEY (id),
    UNIQUE KEY uk_contracts_uuid (uuid),
    UNIQUE KEY uk_contracts_reference (reference_number)
) ENGINE=InnoDB;

-- ---------- billing ----------
CREATE TABLE pricing_plans (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    uuid         VARCHAR(36)    NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    version      BIGINT         DEFAULT 0,
    deleted      BIT            NOT NULL DEFAULT 0,
    name         VARCHAR(150)   NOT NULL,
    charge_model VARCHAR(20)    NOT NULL,
    amount       DECIMAL(14,2),
    currency     VARCHAR(3),
    cycle_days   INT,
    active       BIT            NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pricing_plans_uuid (uuid)
) ENGINE=InnoDB;

CREATE TABLE invoices (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36)    NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT         DEFAULT 0,
    deleted          BIT            NOT NULL DEFAULT 0,
    reference_number VARCHAR(30)    NOT NULL,
    payer_uuid       VARCHAR(36)    NOT NULL,
    payer_type       VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    amount           DECIMAL(14,2),
    currency         VARCHAR(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoices_uuid (uuid),
    UNIQUE KEY uk_invoices_reference (reference_number)
) ENGINE=InnoDB;

CREATE TABLE invoice_line_items (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36)    NOT NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT         DEFAULT 0,
    deleted     BIT            NOT NULL DEFAULT 0,
    invoice_id  BIGINT         NOT NULL,
    description VARCHAR(255)   NOT NULL,
    item_type   VARCHAR(30)    NOT NULL,
    item_uuid   VARCHAR(36),
    quantity    INT            NOT NULL,
    amount      DECIMAL(14,2),
    currency    VARCHAR(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_line_items_uuid (uuid),
    CONSTRAINT fk_li_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB;

-- payments: append-only, carries only created_at (no BaseEntity audit/version/deleted)
CREATE TABLE payments (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    uuid               VARCHAR(36)    NOT NULL,
    reference_number   VARCHAR(30)    NOT NULL,
    invoice_uuid       VARCHAR(36)    NOT NULL,
    channel            VARCHAR(20)    NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    amount             DECIMAL(14,2),
    currency           VARCHAR(3),
    control_number     VARCHAR(60),
    provider_reference VARCHAR(60),
    created_at         DATETIME(6)    NOT NULL,
    confirmed_at       DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_uuid (uuid),
    UNIQUE KEY uk_payments_reference (reference_number),
    UNIQUE KEY uk_payments_control (control_number)
) ENGINE=InnoDB;

CREATE TABLE subscriptions (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    uuid               VARCHAR(36) NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    version            BIGINT      DEFAULT 0,
    deleted            BIT         NOT NULL DEFAULT 0,
    subscriber_uuid    VARCHAR(36) NOT NULL,
    pricing_plan_uuid  VARCHAR(36) NOT NULL,
    start_date         DATE        NOT NULL,
    current_period_end DATE        NOT NULL,
    active             BIT         NOT NULL DEFAULT 1,
    auto_renew         BIT         NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subscriptions_uuid (uuid)
) ENGINE=InnoDB;

-- ---------- marketplace ----------
CREATE TABLE marketplace_items (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uuid              VARCHAR(36)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       DEFAULT 0,
    deleted           BIT          NOT NULL DEFAULT 0,
    title             VARCHAR(255) NOT NULL,
    description       LONGTEXT,
    type              VARCHAR(30)  NOT NULL,
    pricing_plan_uuid VARCHAR(36)  NOT NULL,
    storage_key       VARCHAR(500),
    published         BIT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_marketplace_uuid (uuid)
) ENGINE=InnoDB;

-- ---------- notification ----------
CREATE TABLE notifications (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    uuid           VARCHAR(36)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT       DEFAULT 0,
    deleted        BIT          NOT NULL DEFAULT 0,
    recipient_uuid VARCHAR(36)  NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    body           LONGTEXT,
    is_read        BIT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_uuid (uuid)
) ENGINE=InnoDB;

-- ---------- audit (own columns; not BaseEntity) ----------
CREATE TABLE audit_logs (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(36) NOT NULL,
    actor       VARCHAR(100) NOT NULL,
    action      VARCHAR(80)  NOT NULL,
    target_type VARCHAR(60),
    target_uuid VARCHAR(36),
    detail      LONGTEXT,
    ip_address  VARCHAR(45),
    occurred_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_audit_logs_uuid (uuid),
    KEY idx_audit_actor (actor),
    KEY idx_audit_action (action)
) ENGINE=InnoDB;
