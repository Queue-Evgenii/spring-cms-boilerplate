ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';

UPDATE users SET user_type = 'ADMIN';

ALTER TABLE users
    ADD CONSTRAINT users_type_check
    CHECK (user_type IN ('ADMIN', 'CUSTOMER'));

CREATE TABLE sessions (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id         UUID        NOT NULL,
    token_hash      VARCHAR(64) NOT NULL,
    login_realm     VARCHAR(20) NOT NULL,
    ip              VARCHAR(45),
    user_agent      TEXT,
    device          VARCHAR(30),
    os              VARCHAR(50),
    browser         VARCHAR(50),
    country         VARCHAR(2),
    city            VARCHAR(100),
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    revoked_reason  VARCHAR(30),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT sessions_login_realm_check
        CHECK (login_realm IN ('ADMIN', 'PUBLIC'))
);

CREATE UNIQUE INDEX sessions_token_hash_unique
    ON sessions (token_hash);

CREATE INDEX idx_sessions_active
    ON sessions (user_id, revoked_at, expires_at);

CREATE TABLE auth_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         UUID,
    email_attempted VARCHAR(255),
    event_type      VARCHAR(30) NOT NULL,
    reason          VARCHAR(50),
    session_id      UUID,
    ip              VARCHAR(45),
    user_agent      TEXT,
    country         VARCHAR(2),
    city            VARCHAR(100),
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_auth_events_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_auth_events_session FOREIGN KEY (session_id)
        REFERENCES sessions (id) ON DELETE SET NULL,
    CONSTRAINT auth_events_type_check
        CHECK (event_type IN (
            'LOGIN_SUCCESS', 'LOGIN_FAILED', 'TOTP_FAILED',
            'TOTP_SUCCESS', 'LOGOUT', 'SESSION_KICKED',
            'ACCOUNT_LOCKED', 'PASSWORD_CHANGED', 'PASSWORD_RESET_REQUESTED'
        ))
);

CREATE INDEX idx_auth_events_user_created
    ON auth_events (user_id, created_at DESC);

CREATE INDEX idx_auth_events_ip_created
    ON auth_events (ip, created_at DESC);

CREATE INDEX idx_auth_events_created_at
    ON auth_events (created_at DESC);

CREATE INDEX idx_auth_events_type_created
    ON auth_events (event_type, created_at DESC);

CREATE TABLE audit_logs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   VARCHAR(64) NOT NULL,
    action      VARCHAR(20) NOT NULL,
    actor_type  VARCHAR(10) NOT NULL,
    actor_id    UUID,
    old_values  JSONB,
    new_values  JSONB,
    ip          VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT audit_logs_action_check
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'RESTORE')),
    CONSTRAINT audit_logs_actor_type_check
        CHECK (actor_type IN ('USER', 'SYSTEM'))
);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id, created_at DESC);

CREATE INDEX idx_audit_logs_actor_created
    ON audit_logs (actor_id, created_at DESC);

CREATE INDEX idx_audit_logs_created
    ON audit_logs (created_at DESC);
