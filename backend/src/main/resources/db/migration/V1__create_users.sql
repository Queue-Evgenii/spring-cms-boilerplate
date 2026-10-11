CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT uuidv7(),
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    middle_name           VARCHAR(100),
    phone                 VARCHAR(20),
    email                 VARCHAR(255) NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    must_change_password  BOOLEAN      NOT NULL DEFAULT TRUE,
    password_changed_at   TIMESTAMPTZ,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    totp_secret_encrypted TEXT,
    totp_enabled_at       TIMESTAMPTZ,
    totp_last_used_step   BIGINT,
    last_login_at         TIMESTAMPTZ,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT users_email_lowercase_check
        CHECK (email = lower(email)),
    CONSTRAINT users_status_check
        CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED')),
    CONSTRAINT users_failed_attempts_check
        CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX users_email_unique
    ON users (email)
    WHERE deleted_at IS NULL;