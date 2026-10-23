-- History of beacon emails: what was sent to whom, its status and the error if it failed
CREATE TABLE notifications (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Kept after the beacon is deleted: the history stays, the link is cleared
    beacon_id  BIGINT      REFERENCES beacons (id) ON DELETE SET NULL,
    channel    VARCHAR(20) NOT NULL DEFAULT 'EMAIL' CHECK (channel IN ('EMAIL')),
    status     VARCHAR(20) NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'SENT', 'FAILED')),
    -- What the email shows: edition, prices, store; the daily digest is built from it
    payload    JSONB       NOT NULL,
    error      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at    TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_status ON notifications (status);

-- A triggered beacon keeps watching: a further drop sends another email
CREATE INDEX idx_beacons_edition_watching ON beacons (edition_id) WHERE status IN ('ACTIVE', 'TRIGGERED');
DROP INDEX idx_beacons_edition_active;
