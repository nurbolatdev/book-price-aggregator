# M14 — Каталог и быстрый поиск

**Ветка:** `feature/m14-catalog-search` · **Зависит от:** M13 · **Оценка:** 2–3 дня

## Цель
Поиск, который отвечает за десятки миллисекунд из своей БД и Redis: нечёткий поиск pg_trgm, фильтры, сортировка, подсказки, карточка издания с ценами и историей, блок «подешевели за неделю». Google Books — только для пополнения каталога метаданными.

## Перед началом прочитать
`docs/03-api-contract.md` (раздел 1), `docs/01-architecture.md` (разделы 2 и 5), `docs/02-data-model.md` (раздел 3).

## Задачи

### 1. Миграция V8__search_trgm.sql
- [x] `CREATE EXTENSION IF NOT EXISTS pg_trgm;`
- [x] GIN-индексы на `books.title_norm`, `books.author_norm`.
- [x] Таблица `search_log` + индекс по `created_at`.

### 2. Поиск — пакет `search/`
- [x] `SearchRequest` (record) — все параметры из контракта, валидация (`q` 2–200, `size` ≤ 50).
- [x] `SearchRepository` — native SQL (через `NamedParameterJdbcTemplate` или `@Query(nativeQuery)`):
  1. если `q` похож на ISBN (`IsbnUtils.normalize != null`) → точный поиск по `editions.isbn13`;
  2. иначе: кандидаты по `title_norm % :q OR author_norm % :q OR title_norm LIKE '%'||:q||'%'`;
  3. join `editions` + агрегат по `offers` (только `match_status = 'MATCHED'`): лучшая цена, число магазинов, `max(last_seen_at)`;
  4. фильтры по магазинам, цене, наличию, переплёту, языку — в `WHERE`/`HAVING`;
  5. сортировка: `RELEVANCE` = `GREATEST(similarity(title_norm,:q), similarity(author_norm,:q))`, `PRICE_ASC`, `DISCOUNT_DESC` (`1 - price/old_price`), `STORES_DESC`;
  6. пагинация `LIMIT/OFFSET` + отдельный `count`.
  Возвращает DTO-проекции, **не сущности**.
- [x] Предложения для чипов (до 5 на издание) — **одним** вторым запросом по списку `editionId` страницы (никакого N+1).
- [x] `SearchService` — нормализует `q` (`TextNormalizer`), вызывает репозиторий, собирает ответ, кэширует страницу в Redis (`search:v1:{hash}`; ключ — SHA-1 от нормализованного запроса + фильтров), TTL 10 мин.
- [x] Если результатов < 3 и это не ISBN → асинхронно (отдельный бин `CatalogSeeder`, `@Async`) запросить Google Books, сохранить новые книги/издания **без цен**. Ответ пользователю не ждёт этого.
- [x] `SearchLogWriter` — отдельный бин, `@Async` запись в `search_log` (запрос, нормализованный запрос, число результатов, userId если есть).
- [x] `GET /api/v1/search/suggest?q=` — до 8 вариантов «название — автор» по префиксу/сходству, кэш 10 мин; `q` ≥ 2 символов.
- [x] Удалить старые `BookSearchService`, пакет `source/` (кроме перенесённого клиента Google Books) и `GET /api/books/search`.

### 3. Метаданные — пакет `metadata/`
- [x] `GoogleBooksClient` (перенос из `source/GoogleBooksAdapter`): `searchByText(q)`, `findByIsbn(isbn13)` → `BookMetadata` (record). Через `RestClient` (Spring 6.1), таймаут 5 с, при ошибке — пустой результат + WARN.
- [x] Никаких цен из Google Books.

### 4. Издание — пакет `edition/`
- [x] `EditionService.getDetail(id)` — издание + книга + предложения магазинов (`MATCHED`, сортировка: в наличии сначала, потом по цене) + другие издания книги с их лучшими ценами + `priceVerdict`. Загрузка за 2–3 запроса (fetch join/проекции).
- [x] `PriceVerdictCalculator` — средняя минимальная цена за 90 дней из `price_history`; если истории меньше 14 дней → `NOT_ENOUGH_DATA`; пороги ±5%.
- [x] `PriceHistoryService.getHistory(editionId, period)` — по дням: минимальная цена среди предложений издания на конец дня (SQL с `date_trunc('day', recorded_at)` + перенос последнего известного значения); плюс `min/avg/max` за период.
- [x] Кэш карточки: `edition:v1:{id}`, TTL 30 мин. Метод сброса `EditionCacheService.evict(editionId)` (будет вызываться на `price.changed` в M17).
- [x] `GET /api/v1/editions/{id}`, `GET /api/v1/editions/{id}/price-history`, `GET /api/v1/books/{bookId}` — по контракту.

### 5. Главная — `home/` (или внутри `search/`)
- [x] `DropsService` — издания с наибольшим снижением лучшей цены за 7 дней (по `price_history`), только в наличии, топ N. Считается **по расписанию** раз в 15 мин и кладётся в Redis `home:v1:drops-week`; эндпоинт читает из Redis. (Расписание — `@Scheduled` здесь; ShedLock добавится в M17.)
- [x] `GET /api/v1/home/drops?limit=8`.

### 6. Конфигурация кэшей
- [x] Именованные кэши с разными TTL в `RedisConfig` (`RedisCacheManagerBuilderCustomizer`): `search` 10 мин, `suggest` 10 мин, `edition` 30 мин, `drops` 15 мин.
- [x] Включить gzip-сжатие ответов: `server.compression.enabled: true`, `mime-types: application/json`, `min-response-size: 1KB`.

### 7. Тестовые данные
- [x] `src/test/resources/sql/catalog-seed.sql` — 30–50 изданий с предложениями и историей цен (реалистичные названия, цены — тестовые). Используется в `*IT`.
- [x] Профиль `dev`: опциональный `DevDataLoader` (вкл. `app.dev.seed=true`) грузит тот же набор для ручной проверки и фронтенда до появления парсеров.

## Тесты
- [x] `SearchRepositoryIT` — опечатка «мастер маргарта» находит «Мастер и Маргарита»; поиск по автору; поиск по ISBN с дефисами; фильтр по магазину и цене; все сортировки; пагинация.
- [x] `SearchServiceTest` — кэш-ключ одинаков для «Мастер  и Маргарита» и «мастер и маргарита»; при < 3 результатах вызывается `CatalogSeeder`.
- [x] `EditionServiceIT` — карточка, другие издания, verdict для 3 случаев.
- [x] `PriceHistoryServiceIT` — дневные минимумы, перенос значения на дни без изменений.
- [x] `SearchControllerIT` — 400/422 на короткий `q`, формат ответа.
- [x] Проверка «нет N+1»: в `SearchServiceIT` включить статистику Hibernate или считать SQL-запросы (datasource-proxy) — на одну страницу поиска ≤ 3 запросов.

## Критерии приёмки
- [x] Поиск с опечаткой находит книгу; ISBN ищется точно.
- [x] Запрос страницы поиска выполняет ≤ 3 SQL-запроса; повторный такой же запрос идёт из Redis.
- [x] `EXPLAIN ANALYZE` поискового запроса на seed-данных использует GIN-индекс (приложить вывод в PR).
- [x] Ответ поиска на тёплом кэше < 50 мс локально (приложить замер curl `-w "%{time_total}"`).
- [x] Google Books не возвращает цены и не блокирует ответ поиска.
- [x] `./mvnw -q verify` зелёный.

## План коммитов
1. `feat: enable pg_trgm and add search log table`
2. `refactor: move google books to metadata client without prices`
3. `feat: fuzzy search with filters, sorting and pagination`
4. `feat: search suggestions endpoint`
5. `feat: async catalog seeding and search logging`
6. `feat: edition detail with offers and price verdict`
7. `feat: price history by day with min avg max`
8. `feat: weekly price drops for home page`
9. `perf: named redis caches and gzip responses`
10. `test: catalog seed data and search integration tests`
11. `docs: update milestone status`

## Не входит
Сбор цен (M15–M16), сброс кэша по событиям (M17), маячки (M19).
