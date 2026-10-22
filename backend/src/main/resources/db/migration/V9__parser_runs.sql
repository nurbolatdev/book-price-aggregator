-- One row per parser pass over a store; feeds the admin panel (success rate, timings)
CREATE TABLE parser_runs (
    id              BIGSERIAL   PRIMARY KEY,
    store_id        BIGINT      NOT NULL REFERENCES stores (id),
    trigger_type    VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    pages_ok        INT         NOT NULL DEFAULT 0,
    pages_failed    INT         NOT NULL DEFAULT 0,
    offers_upserted INT         NOT NULL DEFAULT 0,
    prices_changed  INT         NOT NULL DEFAULT 0,
    error           TEXT,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at     TIMESTAMPTZ,
    CONSTRAINT chk_parser_runs_trigger CHECK (trigger_type IN ('SCHEDULED', 'ON_DEMAND', 'MANUAL')),
    CONSTRAINT chk_parser_runs_status CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED'))
);

CREATE INDEX idx_parser_runs_store_started ON parser_runs (store_id, started_at DESC);

-- Events worth showing to an administrator (not a replacement for logs)
CREATE TABLE system_events (
    id         BIGSERIAL   PRIMARY KEY,
    level      VARCHAR(10) NOT NULL,
    source     VARCHAR(50) NOT NULL,
    message    TEXT        NOT NULL,
    details    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_system_events_level CHECK (level IN ('INFO', 'WARN', 'ERROR'))
);

CREATE INDEX idx_system_events_created ON system_events (created_at DESC);
