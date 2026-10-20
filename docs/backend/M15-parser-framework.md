# M15 — Каркас парсеров и первый магазин (Flip.kz)

**Ветка:** `feature/m15-parser-framework` · **Зависит от:** M14 · **Оценка:** 3 дня

## Цель
Построить общий каркас сбора цен (загрузка страниц с лимитами, запуски, запись предложений, история цены, события) и подключить первый реальный магазин — Flip.kz. После вехи в системе появляются **настоящие цены**.

## Перед началом прочитать
`docs/05-parsing-policy.md` (обязательно целиком), `docs/01-architecture.md` (раздел 3), `docs/adr/ADR-002-parsers-in-backend.md`.

## Задачи

### 1. Зависимости
- [ ] `org.jsoup:jsoup`
- [ ] `io.github.resilience4j:resilience4j-spring-boot3` + `spring-boot-starter-aop`
- [ ] test: `org.wiremock:wiremock-standalone`

### 2. Миграция V9__parser_runs.sql
- [ ] `parser_runs`, `system_events` по `docs/02-data-model.md`.

### 3. Ядро — `parser/core/`
- [ ] `StoreParser` (интерфейс), `ParsedOffer` (record), `Binding` переиспользовать из `edition`.
- [ ] `ParserProperties` (`app.parser.*`): `userAgent`, `timeout` (10s), `htmlCacheTtl` (1h), `maxEditionsPerRun` (200), `maxDiscoveryPerRun` (50).
- [ ] `HtmlFetcher`:
  - Jsoup `connect(url).userAgent(...).timeout(...)`;
  - перед запросом — проверка кэша Redis `parser:v1:html:{sha1(url)}`;
  - Resilience4j: `RateLimiter` на магазин (из `stores.min_delay_ms`), `Retry` (2 повтора на IOException/5xx, **не повторять 403/429**), `CircuitBreaker` на магазин;
  - 403/429/капча (эвристика: страница содержит форму капчи) → `StoreBlockedException` → открывает circuit breaker;
  - логирование с `store`, `url`, временем ответа.
- [ ] `RobotsTxtChecker` — загрузка и кэш `robots.txt` магазина на сутки; `HtmlFetcher` отказывается грузить запрещённый путь (`DisallowedByRobotsException`).
- [ ] `ParserRunService` — `start(store, trigger)` → `parser_runs` RUNNING; `finish(runId, stats)` → SUCCESS / PARTIAL (есть ошибки страниц) / FAILED; пишет `system_events` для WARN/ERROR.
- [ ] `SystemEventService` (пакет `admin/` или `common/`) — `info/warn/error(source, message, details)`.

### 4. Запись предложений — `offer/OfferIngestionService`
- [ ] `ingest(StoreCode store, List<ParsedOffer> items, Long runId)`, транзакция на пачку:
  1. upsert по `(store_id, external_id)`: новые поля, `last_seen_at = now`;
  2. если цена изменилась (или предложение новое) → запись в `price_history` (`price`, `old_price`), `last_changed_at = now`;
  3. сопоставление издания через `EditionMatcher` (только если `edition_id` ещё не задан);
  4. для каждого изменения цены у сопоставленного предложения — Spring `ApplicationEvent` `PriceChangedEvent(offerId, editionId, store, oldPrice, newPrice)`; публикация **после коммита** (`@TransactionalEventListener(phase = AFTER_COMMIT)` у слушателей). В M17 слушатель будет отправлять событие в RabbitMQ.
- [ ] Возвращает `IngestResult(upserted, priceChanged, matched, pendingReview)`.
- [ ] Предложения, не встреченные в магазине 7 дней (`last_seen_at`), помечаются `in_stock = false` — задача планировщика (M17), здесь — метод в сервисе.

### 5. Сопоставление v1 — `parser/matching/EditionMatcher`
- [ ] По ISBN → существующее издание или новое (метаданные из `GoogleBooksClient.findByIsbn`, иначе из самого `ParsedOffer`).
- [ ] Без ISBN → точное совпадение нормализованных названия и автора → `MATCHED`, иначе `PENDING_REVIEW`. (Нечёткое сопоставление — M16.)

### 6. Что обходит парсер за один запуск — `parser/core/StoreCrawlService`
- [ ] **Обновление известных:** для предложений магазина, отсортированных по `last_seen_at` (старые сначала), `fetchOffer(url)`; лимит `maxEditionsPerRun`. Приоритет — издания с активными маячками.
- [ ] **Поиск новых:** для изданий без предложения в этом магазине (сначала популярные по `search_log` и с маячками) — `search(isbn или "название автор")`, лимит `maxDiscoveryPerRun`.
- [ ] Ошибка на одной странице не останавливает запуск — считается в `pages_failed`.
- [ ] Метод `crawl(StoreCode, TriggerType, Optional<List<Long>> editionIds)`.

### 7. Flip.kz — `parser/flip/`
- [ ] Перед кодом: проверить `robots.txt`, вручную разобрать страницы поиска и товара, найти цену, старую цену, наличие, ISBN, издательство, год, переплёт. Если есть JSON-LD — использовать его.
- [ ] Сохранить 4–5 фикстур в `src/test/resources/fixtures/flip/` (поиск, карточка, скидка, нет в наличии, без ISBN).
- [ ] `FlipSelectors` (константы), `FlipParser implements StoreParser`.
- [ ] Обновить таблицу магазинов в `docs/05-parsing-policy.md` фактическими данными.

### 8. Временный ручной запуск
- [ ] `POST /api/v1/admin/parsers/{storeCode}/run` (роль ADMIN) — пока **синхронно** вызывает `StoreCrawlService.crawl(..., MANUAL)` и возвращает `IngestResult`. В M17 станет постановкой задачи в очередь (202).
- [ ] Чтобы проверить локально: временно назначить себе роль ADMIN SQL-запросом (нормальный bootstrap админа — M18). Описать это в PR.

## Тесты
- [ ] `FlipParserTest` — на каждой фикстуре проверены все поля `ParsedOffer`.
- [ ] `HtmlFetcherTest` (WireMock) — кэш (второй вызов не идёт в сеть), retry на 503, **нет** retry на 429, circuit breaker открывается, User-Agent корректный, robots.txt запрещает путь.
- [ ] `OfferIngestionServiceIT` — новое предложение → история + событие; та же цена → без истории и без события; новая цена → история + событие; upsert не создаёт дубликатов.
- [ ] `EditionMatcherTest` — по ISBN, по точному названию, иначе PENDING_REVIEW.
- [ ] `StoreCrawlServiceIT` — с подменённым `StoreParser`: ошибка одной страницы → `PARTIAL`, подсчёт статистики.

## Критерии приёмки
- [ ] Ручной запуск для FLIP создаёт реальные предложения; они видны в `GET /api/v1/editions/{id}` и в поиске.
- [ ] Повторный запуск без изменений цен не создаёт записей `price_history`.
- [ ] В `parser_runs` есть запись с корректной статистикой.
- [ ] Частота запросов к магазину не выше настроенной (видно по логам с временем).
- [ ] Тесты не ходят в интернет; `./mvnw -q verify` зелёный.

## План коммитов
1. `build: add jsoup, resilience4j and wiremock`
2. `feat: add parser runs and system events tables`
3. `feat: html fetcher with cache, rate limit, retry and circuit breaker`
4. `feat: robots.txt checker for store fetcher`
5. `feat: offer ingestion with price history on change and price changed event`
6. `feat: edition matcher by isbn and exact title`
7. `feat: store crawl service with refresh and discovery passes`
8. `feat: flip.kz parser with fixtures`
9. `feat: manual parser run endpoint for admins`
10. `test: parser framework tests`
11. `docs: update parsing notes and milestone status`

## Не входит
Остальные магазины и нечёткое сопоставление (M16), очереди и расписание (M17).
