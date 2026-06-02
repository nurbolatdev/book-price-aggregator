CREATE TABLE books (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(500) NOT NULL,
    author       VARCHAR(500),
    isbn         VARCHAR(20)  UNIQUE,
    publisher    VARCHAR(255),
    published_at DATE,
    cover_url    TEXT,
    description  TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_books_isbn ON books (isbn);
CREATE INDEX idx_books_title ON books (title);

CREATE TABLE offers (
    id           BIGSERIAL      PRIMARY KEY,
    book_id      BIGINT         NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    source       VARCHAR(100)   NOT NULL,
    price        NUMERIC(10, 2) NOT NULL,
    original_price NUMERIC(10, 2),
    currency     VARCHAR(10)    NOT NULL DEFAULT 'KZT',
    in_stock     BOOLEAN        NOT NULL DEFAULT TRUE,
    url          TEXT           NOT NULL,
    fetched_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offers_book_id ON offers (book_id);
CREATE INDEX idx_offers_source ON offers (source);
