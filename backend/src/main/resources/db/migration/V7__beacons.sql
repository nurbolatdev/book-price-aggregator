-- Beacons replace price alerts and track an edition instead of a book.

CREATE TABLE beacons (
    id                  BIGSERIAL      PRIMARY KEY,
    user_id             BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    edition_id          BIGINT         NOT NULL REFERENCES editions (id) ON DELETE CASCADE,
    condition_type      VARCHAR(20)    NOT NULL,
    target_price        NUMERIC(10, 2),
    target_percent      SMALLINT,
    baseline_price      NUMERIC(10, 2),
    status              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    last_notified_price NUMERIC(10, 2),
    last_triggered_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_beacons_user_edition UNIQUE (user_id, edition_id),
    CONSTRAINT chk_beacons_condition_type
        CHECK (condition_type IN ('ANY_DROP', 'BELOW_PRICE', 'DISCOUNT_PERCENT')),
    CONSTRAINT chk_beacons_status
        CHECK (status IN ('ACTIVE', 'TRIGGERED', 'PAUSED')),
    CONSTRAINT chk_beacons_below_price
        CHECK (condition_type <> 'BELOW_PRICE' OR (target_price IS NOT NULL AND target_price > 0)),
    CONSTRAINT chk_beacons_discount_percent
        CHECK (condition_type <> 'DISCOUNT_PERCENT' OR (target_percent IS NOT NULL AND target_percent BETWEEN 1 AND 90))
);

CREATE INDEX idx_beacons_edition_active ON beacons (edition_id) WHERE status = 'ACTIVE';

-- After V5 every legacy book has exactly one edition; alerts with a non-positive target
-- could never have fired and would violate the BELOW_PRICE check, so they are skipped.
INSERT INTO beacons (user_id, edition_id, condition_type, target_price, status,
                     last_triggered_at, created_at, updated_at)
SELECT pa.user_id,
       e.id,
       'BELOW_PRICE',
       pa.target_price,
       CASE WHEN pa.active THEN 'ACTIVE' ELSE 'TRIGGERED' END,
       pa.triggered_at AT TIME ZONE 'UTC',
       pa.created_at AT TIME ZONE 'UTC',
       NOW()
FROM price_alerts pa
JOIN (SELECT book_id, MIN(id) AS id FROM editions GROUP BY book_id) e ON e.book_id = pa.book_id
WHERE pa.target_price > 0;

DROP TABLE price_alerts;
