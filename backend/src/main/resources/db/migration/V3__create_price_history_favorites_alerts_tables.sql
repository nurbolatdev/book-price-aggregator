CREATE TABLE price_history (
    id         BIGSERIAL      PRIMARY KEY,
    offer_id   BIGINT         NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    price      NUMERIC(10, 2) NOT NULL,
    recorded_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_history_offer_id ON price_history (offer_id);
CREATE INDEX idx_price_history_recorded_at ON price_history (recorded_at);

CREATE TABLE favorites (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    book_id    BIGINT    NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);

CREATE INDEX idx_favorites_user_id ON favorites (user_id);

CREATE TABLE price_alerts (
    id             BIGSERIAL      PRIMARY KEY,
    user_id        BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    book_id        BIGINT         NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    target_price   NUMERIC(10, 2) NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    triggered_at   TIMESTAMP,
    UNIQUE (user_id, book_id)
);

CREATE INDEX idx_price_alerts_user_id ON price_alerts (user_id);
CREATE INDEX idx_price_alerts_active ON price_alerts (active);
