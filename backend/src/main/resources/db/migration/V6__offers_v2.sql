-- Offers belong to an edition (once matched) in a store, keyed by the store's product id.

-- Phase 1 mock offers are fake data; their price history is removed by cascade
DELETE FROM offers WHERE source IN ('kaspi', 'ozon', 'google_books');

ALTER TABLE offers
    ADD COLUMN edition_id      BIGINT REFERENCES editions (id) ON DELETE SET NULL,
    ADD COLUMN store_id        BIGINT REFERENCES stores (id),
    ADD COLUMN external_id     VARCHAR(255),
    ADD COLUMN title_raw       VARCHAR(500),
    ADD COLUMN delivery_text   VARCHAR(255),
    ADD COLUMN match_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    ADD COLUMN first_seen_at   TIMESTAMPTZ,
    ADD COLUMN last_seen_at    TIMESTAMPTZ,
    ADD COLUMN last_changed_at TIMESTAMPTZ;

-- Carry over any remaining rows: the source name maps to a store code,
-- the book has exactly one edition after V5. Rows of unknown stores cannot be kept.
UPDATE offers o SET store_id = s.id FROM stores s WHERE s.code = upper(o.source);
DELETE FROM offers WHERE store_id IS NULL;

UPDATE offers o
SET edition_id   = e.id,
    match_status = 'MATCHED'
FROM (SELECT book_id, MIN(id) AS id FROM editions GROUP BY book_id) e
WHERE e.book_id = o.book_id;

UPDATE offers
SET external_id     = 'legacy-' || id,
    first_seen_at   = fetched_at AT TIME ZONE 'UTC',
    last_seen_at    = fetched_at AT TIME ZONE 'UTC',
    last_changed_at = fetched_at AT TIME ZONE 'UTC';

ALTER TABLE offers RENAME COLUMN original_price TO old_price;

ALTER TABLE offers
    DROP COLUMN book_id,
    DROP COLUMN source,
    DROP COLUMN currency,
    DROP COLUMN fetched_at;

ALTER TABLE offers
    ALTER COLUMN store_id SET NOT NULL,
    ALTER COLUMN external_id SET NOT NULL,
    ALTER COLUMN first_seen_at SET NOT NULL,
    ALTER COLUMN first_seen_at SET DEFAULT NOW(),
    ALTER COLUMN last_seen_at SET NOT NULL,
    ALTER COLUMN last_seen_at SET DEFAULT NOW(),
    ALTER COLUMN last_changed_at SET NOT NULL,
    ALTER COLUMN last_changed_at SET DEFAULT NOW();

ALTER TABLE offers
    ADD CONSTRAINT uq_offers_store_external UNIQUE (store_id, external_id),
    ADD CONSTRAINT chk_offers_match_status CHECK (match_status IN ('MATCHED', 'PENDING_REVIEW', 'REJECTED'));

CREATE INDEX idx_offers_edition_id ON offers (edition_id);
CREATE INDEX idx_offers_edition_price_in_stock ON offers (edition_id, price) WHERE in_stock;
CREATE INDEX idx_offers_pending_review ON offers (match_status) WHERE match_status = 'PENDING_REVIEW';

-- Price history: keep the old (crossed-out) price too; history is read per offer, newest first
ALTER TABLE price_history
    ADD COLUMN old_price NUMERIC(10, 2),
    ALTER COLUMN recorded_at TYPE TIMESTAMPTZ USING recorded_at AT TIME ZONE 'UTC';

DROP INDEX IF EXISTS idx_price_history_offer_id;
DROP INDEX IF EXISTS idx_price_history_recorded_at;
CREATE INDEX idx_price_history_offer_recorded ON price_history (offer_id, recorded_at DESC);
