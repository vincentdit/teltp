-- Account lockout, email verification, and remember-me on refresh sessions.
-- Existing accounts stay verified so current users can still sign in.

ALTER TABLE users
    ADD COLUMN email_verified BIT NOT NULL DEFAULT 1,
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL;

ALTER TABLE refresh_tokens
    ADD COLUMN persistent BIT NOT NULL DEFAULT 1;

CREATE TABLE email_verification_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    username VARCHAR(100) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_verify_hash (token_hash),
    KEY idx_verify_user (username)
);
