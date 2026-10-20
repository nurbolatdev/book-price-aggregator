-- Book -> edition model (ADR-003): isbn, publisher, year and cover move from books to editions.

CREATE TABLE editions (
    id         BIGSERIAL    PRIMARY KEY,
    book_id    BIGINT       NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    isbn13     VARCHAR(13)  UNIQUE,
    publisher  VARCHAR(255),
    year       SMALLINT,
    binding    VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    language   VARCHAR(5),
    pages      INT,
    cover_url  TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_editions_isbn13 CHECK (isbn13 ~ '^[0-9]{13}$'),
    CONSTRAINT chk_editions_binding CHECK (binding IN ('HARDCOVER', 'PAPERBACK', 'UNKNOWN'))
);

CREATE INDEX idx_editions_book_id ON editions (book_id);

-- One edition per existing book. ISBN is kept only when it has exactly 13 digits;
-- if two books normalize to the same ISBN, only the oldest keeps it.
WITH source AS (
    SELECT id, publisher, published_at, cover_url,
           regexp_replace(COALESCE(isbn, ''), '[^0-9]', '', 'g') AS digits
    FROM books
), ranked AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY digits ORDER BY id) AS rn
    FROM source
)
INSERT INTO editions (book_id, isbn13, publisher, year, cover_url)
SELECT id,
       CASE WHEN length(digits) = 13 AND rn = 1 THEN digits END,
       publisher,
       EXTRACT(YEAR FROM published_at)::SMALLINT,
       cover_url
FROM ranked;

-- Normalized strings for search; same rules as TextNormalizer
ALTER TABLE books
    ADD COLUMN title_norm  VARCHAR(500),
    ADD COLUMN author_norm VARCHAR(500);

UPDATE books
SET title_norm  = btrim(regexp_replace(regexp_replace(regexp_replace(
                      replace(lower(title), 'ё', 'е'),
                      '[«»"''„“”‘’`]', '', 'g'),
                      '[[:punct:]—–…№]', ' ', 'g'),
                      '\s+', ' ', 'g')),
    author_norm = btrim(regexp_replace(regexp_replace(regexp_replace(
                      replace(lower(author), 'ё', 'е'),
                      '[«»"''„“”‘’`]', '', 'g'),
                      '[[:punct:]—–…№]', ' ', 'g'),
                      '\s+', ' ', 'g'));

ALTER TABLE books ALTER COLUMN title_norm SET NOT NULL;

DROP INDEX IF EXISTS idx_books_isbn;

ALTER TABLE books
    DROP COLUMN isbn,
    DROP COLUMN publisher,
    DROP COLUMN published_at,
    DROP COLUMN cover_url;
