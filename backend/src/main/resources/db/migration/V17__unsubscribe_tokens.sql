-- An unsubscribe link belongs to one beacon: the token says which one to pause
ALTER TABLE email_tokens ADD COLUMN beacon_id BIGINT REFERENCES beacons (id) ON DELETE CASCADE;

ALTER TABLE email_tokens ADD CONSTRAINT chk_email_tokens_unsubscribe_beacon
    CHECK (purpose <> 'UNSUBSCRIBE' OR beacon_id IS NOT NULL);
