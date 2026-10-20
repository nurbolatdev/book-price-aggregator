# M13 — Модель данных v2: издания, магазины, маячки

**Ветка:** `feature/m13-data-model-v2` · **Зависит от:** M12 · **Оценка:** 2 дня

## Цель
Перейти от модели «книга → предложение» к «книга → **издание** → предложение в **магазине**», подготовить таблицу маячков. Это основа честного сравнения цен (сравниваем одно и то же издание).

## Перед началом прочитать
`docs/02-data-model.md` (полностью), `docs/adr/ADR-003-edition-model.md`.

## Задачи

### 1. Миграции (по порядку, каждая со своим кодом в одном коммите)
- [x] **V4__stores.sql** — таблица `stores`; seed:
  | code | name | base_url |
  |---|---|---|
  | KASPI | Kaspi.kz | https://kaspi.kz |
  | FLIP | Flip.kz | https://www.flip.kz |
  | MELOMAN | Meloman | https://www.meloman.kz |
  | WILDBERRIES | Wildberries | https://www.wildberries.kz |
  | OZON | Ozon | https://ozon.kz |
  Все `enabled = true`, `min_delay_ms = 2000`. (URL уточнить при реализации парсеров.)
- [x] **V5__editions.sql**
  - создать `editions`;
  - `INSERT INTO editions (book_id, isbn13, publisher, year, cover_url) SELECT id, <isbn только цифры, если 13 знаков>, publisher, EXTRACT(YEAR FROM published_at), cover_url FROM books;`
  - добавить в `books` колонки `title_norm`, `author_norm`, заполнить (`lower`, `replace('ё','е')`, `regexp_replace` пунктуации);
  - удалить из `books` колонки `isbn`, `publisher`, `published_at`, `cover_url` и индекс `idx_books_isbn`.
- [x] **V6__offers_v2.sql**
  - `DELETE FROM offers WHERE source IN ('kaspi','ozon','google_books');` (фейковые данные; вместе с ними каскадом уйдёт история);
  - `offers`: добавить `edition_id`, `store_id`, `external_id`, `title_raw`, `delivery_text`, `match_status`, `first_seen_at`, `last_seen_at`, `last_changed_at`; переименовать `original_price` → `old_price`; удалить `book_id`, `source`, `currency`, `fetched_at`;
  - `fetched_at` → `last_seen_at` (если строки остались);
  - типы времени → `TIMESTAMPTZ`;
  - `UNIQUE (store_id, external_id)`, индексы из `docs/02-data-model.md`;
  - `price_history`: добавить `old_price`, индекс `(offer_id, recorded_at DESC)`, удалить старые одиночные индексы при необходимости.
- [x] **V7__beacons.sql**
  - создать `beacons` с CHECK-ограничениями;
  - перенести `price_alerts` → `beacons` (`condition_type = 'BELOW_PRICE'`, `edition_id` = издание книги, `status` = `ACTIVE`/`TRIGGERED` по `active`);
  - `DROP TABLE price_alerts`.

### 2. Сущности и репозитории
- [x] `store/StoreCode` (enum), `store/Store` (entity), `StoreRepository`, `StoreService` (кэш справочника в памяти — магазинов 5, меняются редко; сброс при PATCH из админки в M20).
- [x] `edition/Binding` (enum), `edition/Edition`, `EditionRepository`.
- [x] `book/Book` — убрать перенесённые поля, добавить `titleNorm`, `authorNorm`, связь `@OneToMany editions` (LAZY).
- [x] `offer/Offer` — новые поля, `@ManyToOne Edition` (nullable), `@ManyToOne Store`; `offer/MatchStatus` enum.
- [x] `offer/PriceHistory` — `oldPrice`, `Instant recordedAt`.
- [x] `beacon/Beacon`, `beacon/BeaconCondition` (enum), `beacon/BeaconStatus` (enum), `BeaconRepository`. **API маячков не делаем** — это M19.
- [x] Удалить пакет `pricealert` целиком (сущность, сервис, контроллер, DTO).
- [x] `common/text/TextNormalizer` — статический метод `normalize(String)`: нижний регистр, `ё→е`, убрать кавычки «»"', знаки препинания, схлопнуть пробелы. Используется при сохранении `Book` (через `@PrePersist/@PreUpdate`) и в поиске.
- [x] `common/text/IsbnUtils` — `normalize(String)` → 13 цифр или `null`; конвертация ISBN-10 → 13; проверка контрольной цифры.

### 3. Адаптация существующего кода
- [x] `BookController`/сервисы: `GET /api/v1/books/{id}` возвращает книгу и список изданий (без цен — цены появятся в M14). Старые `/offers` и `/price-history` на уровне книги удалить (новые — на уровне издания в M14).
- [x] `BookSearchService` (временный, до M14): сохраняет результаты Google Books как `Book` + `Edition`.
- [x] `FavoriteService` продолжает работать на уровне `Book` (устаревшее, не трогать логику).
- [x] `GET /api/v1/stores` — список магазинов (`code`, `name`, `enabled`).

## Тесты
- [x] `MigrationIT` — на чистой БД применяются V1–V7; отдельный сценарий: вставить данные в состояние V3 (книга с ISBN, алерт), прогнать миграции до V7 → появились издание и маячок с правильными полями. (Flyway API `target` в тесте.)
- [x] `TextNormalizerTest`, `IsbnUtilsTest` (ISBN-10 → 13, неверная контрольная цифра → null, ISBN с дефисами).
- [x] `EditionRepositoryIT` — уникальность ISBN, каскадное удаление.
- [x] `BeaconRepositoryIT` — CHECK: `BELOW_PRICE` без `target_price` не сохраняется.

## Критерии приёмки
- [x] `docker compose down -v && docker compose up -d` — миграции проходят на чистой БД.
- [x] Миграции проходят и на БД с данными Фазы 1 (проверено тестом).
- [x] `ddl-auto: validate` не ругается — сущности соответствуют схеме.
- [x] Пакета `pricealert` нет, `/api/alerts` не существует.
- [x] `GET /api/v1/stores` возвращает 5 магазинов.
- [x] `./mvnw -q verify` зелёный.

## План коммитов
1. `feat: add stores reference table and entity`
2. `feat: add text normalizer and isbn utils`
3. `refactor: introduce editions and move isbn and publisher from books`
4. `refactor: rework offers around editions and stores`
5. `feat: add beacons table and migrate price alerts`
6. `refactor: remove price alert feature`
7. `test: add migration and repository integration tests`
8. `docs: update milestone status`

## Не входит
Поиск (M14), API маячков (M19), парсеры (M15).
