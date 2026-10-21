-- Fuzzy search over normalized titles and authors (see docs/02-data-model.md, section 3)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_books_title_trgm ON books USING GIN (title_norm gin_trgm_ops);
CREATE INDEX idx_books_author_trgm ON books USING GIN (author_norm gin_trgm_ops);

-- Every search request, written asynchronously; feeds admin statistics
CREATE TABLE search_log (
    id            BIGSERIAL    PRIMARY KEY,
    query         VARCHAR(300) NOT NULL,
    query_norm    VARCHAR(300),
    results_count INT          NOT NULL,
    user_id       BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_log_created_at ON search_log (created_at);
