-- Refresh tokens of one sign-in share a family: rotation keeps it, a reused token revokes only its family
ALTER TABLE refresh_tokens ADD COLUMN family_id UUID;

-- Tokens issued before families existed cannot be traced back to their sign-in, so all tokens of a user form
-- one family: a reused old token then ends every old session of that user, as it did before families
UPDATE refresh_tokens SET family_id = md5('legacy-refresh-family:' || user_id)::uuid;

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
