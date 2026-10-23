-- How a notification is delivered is fixed when it is created: a later change of the user's setting does not
-- move it between instant emails and the digest. Publication to RabbitMQ is tracked apart from the email status:
-- only notifications that never reached the broker are published again.
ALTER TABLE notifications
    ADD COLUMN delivery     VARCHAR(10) NOT NULL DEFAULT 'INSTANT' CHECK (delivery IN ('INSTANT', 'DIGEST')),
    ADD COLUMN published_at TIMESTAMPTZ;

UPDATE notifications n SET delivery = 'DIGEST'
FROM users u
WHERE u.id = n.user_id AND u.notify_frequency = 'DAILY';

-- Finished notifications reached the broker (or never needed to)
UPDATE notifications SET published_at = COALESCE(sent_at, created_at) WHERE status <> 'QUEUED';

ALTER TABLE notifications ALTER COLUMN delivery DROP DEFAULT;

CREATE INDEX idx_notifications_unpublished ON notifications (delivery, created_at)
    WHERE status = 'QUEUED' AND published_at IS NULL;
