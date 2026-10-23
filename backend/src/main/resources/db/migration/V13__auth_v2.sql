-- Auth v2: confirmed email, notification preferences, one-time email tokens and rotating refresh tokens

ALTER TABLE users
    ADD COLUMN email_verified   BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN notify_frequency VARCHAR(20) NOT NULL DEFAULT 'INSTANT',
    ADD COLUMN weekly_digest    BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD CONSTRAINT chk_users_notify_frequency CHECK (notify_frequency IN ('INSTANT', 'DAILY'));

ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ;

-- Accounts made before email confirmation existed keep signing in and getting emails
UPDATE users SET email_verified = TRUE;

-- Only the SHA-256 hash of a token is stored; the token itself is in the email link
CREATE TABLE email_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    purpose    VARCHAR(20) NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD', 'UNSUBSCRIBE')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_tokens_user_purpose ON email_tokens (user_id, purpose, created_at DESC);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    user_agent VARCHAR(255)
);

CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
