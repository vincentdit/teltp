-- Refresh sessions (hashed) and password-reset tokens. MFA columns on users.

ALTER TABLE users
    ADD COLUMN mfa_secret VARCHAR(64) NULL,
    ADD COLUMN mfa_enabled BIT NOT NULL DEFAULT 0;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    username VARCHAR(100) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_hash (token_hash),
    KEY idx_refresh_user (username)
);

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    username VARCHAR(100) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reset_hash (token_hash),
    KEY idx_reset_user (username)
);
