-- Assessment/certificate flow extensions:
--   * attempt limits & retake policy (max_attempts, cooldown_minutes)
--   * server-side time-limit enforcement (attempts.expires_at + EXPIRED status)
--   * auto-issue of certificates is behaviour-only (no schema change)

ALTER TABLE assessments
    ADD COLUMN max_attempts     INT NULL AFTER time_limit_minutes,
    ADD COLUMN cooldown_minutes INT NULL AFTER max_attempts;

ALTER TABLE attempts
    ADD COLUMN expires_at DATETIME(6) NULL AFTER status;

-- Give the seeded demo exam a concrete retake policy so the new rules are demonstrable:
-- up to 3 attempts, with a 10-minute cooldown between them.
UPDATE assessments
   SET max_attempts = 3, cooldown_minutes = 10
 WHERE reference_number = 'TELTP-ASMT-2026-09001';
