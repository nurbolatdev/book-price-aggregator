-- Refresh tokens of one sign-in share a family: rotation keeps it, a reused token revokes only its family
ALTER TABLE refresh_tokens ADD COLUMN family_id UUID;

-- Each token issued before families existed counts as its own session
UPDATE refresh_tokens SET family_id = gen_random_uuid();

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
