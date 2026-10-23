-- The nightly cleanup deletes tokens by expiry date
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_email_tokens_expires_at ON email_tokens (expires_at);
