-- When the beacon last changed status (pause, resume, trigger): "paused since ..."
ALTER TABLE beacons ADD COLUMN status_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
UPDATE beacons SET status_changed_at = updated_at;

-- Weekly deals digest: a notification without a beacon, at most one per user and ISO week
ALTER TABLE notifications DROP CONSTRAINT notifications_delivery_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_delivery_check
    CHECK (delivery IN ('INSTANT', 'DIGEST', 'WEEKLY'));
CREATE UNIQUE INDEX ux_notifications_weekly_user_week
    ON notifications (user_id, (payload ->> 'isoWeek')) WHERE delivery = 'WEEKLY';

-- When the notification was queued or queued again by an admin; the relay window counts from it
ALTER TABLE notifications ADD COLUMN queued_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
UPDATE notifications SET queued_at = created_at;
DROP INDEX idx_notifications_unpublished;
CREATE INDEX idx_notifications_unpublished ON notifications (delivery, queued_at)
    WHERE status = 'QUEUED' AND published_at IS NULL;
