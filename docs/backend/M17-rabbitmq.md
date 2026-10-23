# M17 — RabbitMQ: очереди парсинга и событий цен

**Ветка:** `feature/m17-rabbitmq` · **Зависит от:** M16 · **Оценка:** 2–3 дня

## Цель
Перевести всю фоновую работу на RabbitMQ: планировщик и поиск ставят задачи парсинга в очереди, парсеры работают как потребители, изменения цен публикуются событиями, кэш сбрасывается по событию. Пользователь никогда не ждёт сети.

## Перед началом прочитать
`docs/01-architecture.md` (разделы 3–4), `docs/adr/ADR-001-rabbitmq.md`, [Spring AMQP reference](https://docs.spring.io/spring-amqp/reference/).

## Задачи

### 1. Топология — `messaging/RabbitTopologyConfig`
- [x] Exchange `kr.events` (topic, durable), DLX `kr.dlx` (fanout) + очередь `kr.dead-letter`.
- [x] Очереди (durable, `x-dead-letter-exchange = kr.dlx`):
  - `kr.parse.<store>` для каждого `StoreCode`, binding `parse.request.<store>`;
  - `kr.price-changed.beacons`, `kr.price-changed.cache` — binding `price.changed`;
  - `kr.notify.email` — binding `notify.email` (потребитель появится в M18/M19).
- [x] `Jackson2JsonMessageConverter` с модулем JavaTime; `RabbitTemplate` с publisher confirms (`spring.rabbitmq.publisher-confirm-type: correlated`) и `mandatory`.
- [x] Listener container: `acknowledge-mode: auto` + stateless retry interceptor (3 попытки, backoff 1с→5с→15с), после — `RejectAndDontRequeueRecoverer` → DLQ. Concurrency — по таблице в `docs/01-architecture.md`.
- [x] Свойства в `app.messaging.*`.

### 2. Сообщения — `messaging/message/` (record)
```java
public record ParseRequestMessage(UUID messageId, StoreCode store, TriggerType trigger,
                                  List<Long> editionIds, // пусто = обычный обход
                                  Instant occurredAt) {}

public record PriceChangedMessage(UUID messageId, Long offerId, Long editionId, StoreCode store,
                                  BigDecimal oldPrice, BigDecimal newPrice, boolean inStock,
                                  Instant occurredAt) {}
```
- [x] Версия контракта — в заголовке `x-schema-version: 1`.

### 3. Публикация
- [x] `ParseJobPublisher.publish(store, trigger, editionIds)`.
- [x] `PriceChangedPublisher` — `@TransactionalEventListener(AFTER_COMMIT)` на `PriceChangedEvent` из M15 → отправка в `kr.events` с ключом `price.changed`. (Так событие уходит только если данные реально сохранены.)

### 4. Потребители
- [x] `ParseRequestListener` — по одному `@RabbitListener` на очередь магазина (или один listener с `queues` из свойств), вызывает `StoreCrawlService.crawl(...)`. Если магазин `enabled = false` или circuit breaker открыт — подтверждает сообщение и пишет INFO, не ретраит.
- [x] `EditionCacheEvictListener` (`kr.price-changed.cache`) — `EditionCacheService.evict(editionId)` + удаление связанных ключей поиска не нужно (TTL 10 мин достаточно).
- [x] `kr.price-changed.beacons` — пока заглушка-логгер, логика в M19.
- [x] **Идемпотентность:** перед обработкой `SETNX processed:v1:{messageId}` в Redis с TTL 24 ч; если ключ уже есть — пропустить.
  Если обработка упала, ключ снимается: иначе повторные попытки пропускались бы как дубликаты и сообщение не дошло бы до DLQ.

### 5. Планировщик — `scheduler/`
- [x] Зависимости `net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-jdbc-template`; миграция **V10__shedlock.sql**.
  Фактически `V12__shedlock.sql`: номера V10 и V11 заняты в M16 (выключение Kaspi и Wildberries).
- [x] `CrawlScheduler` — `@Scheduled(cron = "${app.scheduler.crawl-cron:0 0 */6 * * *}")` + `@SchedulerLock`: для каждого включённого магазина публикует `ParseRequestMessage(SCHEDULED)`.
- [x] `StaleOfferScheduler` — раз в сутки: предложения без `last_seen_at` за 7 дней → `in_stock = false`.
- [x] `DropsService` из M14 и чистка `search_log` (> 90 дней) — тоже под `@SchedulerLock`.
- [x] Часовой пояс расписаний — `Asia/Almaty` (`zone` в `@Scheduled`).

### 6. Обновление «по требованию» (stale-while-revalidate)
- [x] `RefreshOnDemandService.requestIfStale(editionId)` — вызывается из `EditionService.getDetail` и для первой страницы поиска (асинхронно, не блокирует ответ): если у издания нет предложений свежее 6 ч → для каждого включённого магазина публикует `ParseRequestMessage(ON_DEMAND, [editionId])`.
  Вызов — из `EditionController` и `SearchController`: `getDetail` закеширован, при попадании в кэш его тело не выполняется. Для страницы поиска — одна задача на магазин со всеми устаревшими изданиями страницы.
- [x] Защита от лавины: `SETNX parse:pending:v1:{store}:{editionId}` TTL 30 мин — повторно не ставим.

### 7. Ручной запуск
- [x] `POST /api/v1/admin/parsers/{storeCode}/run` теперь публикует задачу и отвечает **202 Accepted** `{ "queued": true }`.
  404 — у магазина нет парсера, 409 — магазин выключен, 503 `SERVICE_UNAVAILABLE` — очередь недоступна (новый код в `docs/03-api-contract.md`).

## Тесты
- [x] `RabbitTopologyIT` (Testcontainers RabbitMQ) — все очереди и привязки созданы.
- [x] `ParseRequestFlowIT` — опубликовать задачу → listener вызвал `StoreCrawlService` (подменённый) → при исключении 3 попытки → сообщение в `kr.dead-letter`.
- [x] `PriceChangedFlowIT` — `OfferIngestionService` меняет цену → после коммита в очереди `price-changed` есть сообщение; при откате транзакции — нет.
- [x] `IdempotencyIT` — одно сообщение дважды → обработано один раз.
- [x] `RefreshOnDemandServiceTest` — свежие данные → ничего; устаревшие → задачи для всех включённых магазинов; повтор в течение 30 мин → ничего.
- [x] `CrawlSchedulerTest` — выключенный магазин пропускается.

## Критерии приёмки
- [x] В RabbitMQ UI видны exchange, очереди, привязки, DLQ.
- [x] Ручной запуск возвращает 202, задача видна в очереди и обрабатывается.
- [x] Открытие карточки с устаревшими ценами ставит задачи, ответ при этом не замедляется.
- [x] После изменения цены карточка издания (кэш) обновляется без ожидания TTL.
- [x] Остановка RabbitMQ не роняет API: поиск и карточки работают, публикация логирует ошибку.
- [ ] `./mvnw -q verify` зелёный.
  Unit-тесты зелёные. Интеграционные прогнаны на локальных PostgreSQL 16, Redis и RabbitMQ 3.12 вместо Testcontainers (в среде разработки нет Docker): зелёные все, кроме `MigrationIT`, которому нужен контейнер.

## План коммитов
1. `feat: rabbitmq topology with dead letter queue and json converter`
2. `feat: parse request and price changed messages`
3. `feat: publish price changed after commit`
4. `feat: store parse queue listeners with retry and idempotency`
5. `feat: evict edition cache on price change`
6. `feat: shedlock scheduled crawl and stale offer cleanup`
7. `feat: on-demand refresh of stale editions`
8. `refactor: manual parser run goes through queue`
9. `test: rabbitmq flow integration tests`
10. `docs: update milestone status`

## Не входит
Проверка маячков и письма (M19), сбор статистики очередей для админки (M20).
