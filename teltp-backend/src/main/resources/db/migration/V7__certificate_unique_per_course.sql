-- One certificate per (student, course). The application already checks this before issuing,
-- but that check-then-insert has a race window (a manual issue racing the auto-issue listener,
-- or exam-pass and last-lesson completion committing near-simultaneously). This constraint makes
-- the single-issue rule DB-enforced: the losing insert fails, and the auto-issue listener already
-- logs-and-ignores such failures.
ALTER TABLE certificates
    ADD CONSTRAINT uq_cert_student_course UNIQUE (student_uuid, course_uuid);
