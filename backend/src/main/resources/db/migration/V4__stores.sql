CREATE TABLE stores (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(30)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    base_url     VARCHAR(255) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    min_delay_ms INT          NOT NULL DEFAULT 2000,
    CONSTRAINT chk_stores_min_delay_ms CHECK (min_delay_ms >= 0)
);

-- Base URLs are refined when the store parsers are implemented
INSERT INTO stores (code, name, base_url) VALUES
    ('KASPI',       'Kaspi.kz',    'https://kaspi.kz'),
    ('FLIP',        'Flip.kz',     'https://www.flip.kz'),
    ('MELOMAN',     'Meloman',     'https://www.meloman.kz'),
    ('WILDBERRIES', 'Wildberries', 'https://www.wildberries.kz'),
    ('OZON',        'Ozon',        'https://ozon.kz');
