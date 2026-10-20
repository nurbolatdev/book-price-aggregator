# M17 — RabbitMQ: очереди парсинга и событий цен

**Ветка:** `feature/m17-rabbitmq` · **Зависит от:** M16 · **Оценка:** 2–3 дня

## Цель
Перевести всю фоновую работу на RabbitMQ: планировщик и поиск ставят задачи парсинга в очереди, парсеры работают как потребители, изменения цен публикуются событиями, кэш сбрасывается по событию. Пользователь никогда не ждёт сети.

## Перед началом прочитать
`docs/01-architecture.md` (разделы 3–4), `docs/adr/ADR-001-rabbitmq.md`, [Spring AMQP reference](https://docs.spring.io/spring-amqp/reference/).

## Задачи

### 1. Топология — `messaging/RabbitTopologyConfig`
- [ ] Exchange `kr.events` (topic, durable), DLX `kr.dlx` (fanout) + очередь `kr.dead-letter`.
- [ ] Очереди (durable, `x-dead-letter-exchange = kr.dlx`):
  - `kr.parse.<store>` для каждого `StoreCode`, binding `parse.request.<store>`;
  - `kr.price-changed.beacons`, `kr.price-changed.cache` — binding `price.changed`;
  - `kr.notify.email` — binding `notify.email` (потребитель появится в M18/M19).
- [ ] `Jackson2JsonMessageConverter` с модулем JavaTime; `RabbitTemplate` с publisher confirms (`spring.rabbitmq.publisher-confirm-type: correlated`) и `mandatory`.
- [ ] Listener container: `acknowledge-mode: auto` + stateless retry interceptor (3 попытки, backoff 1с→5с→15с), после — `RejectAndDontRequeueRecoverer` → DLQ. Concurrency — по таблице в `docs/01-architecture.md`.
- [ ] Свойства в `app.messaging.*`.

### 2. Сообщения — `messaging/message/` (record)
```java
public record ParseRequestMessage(UUID messageId, StoreCode store, TriggerType trigger,
                                  List<Long> editionIds, // пусто = обычный обход
                                  Instant occurredAt) {}

public record PriceChangedMessage(UUID messageId, Long offerId, Long editionId, StoreCode store,
                                  BigDecimal oldPrice, BigDecimal newPrice, boolean inStock,
                                  Instant occurredAt) {}
```
- [ ] Версия контракта — в заголовке `x-schema-version: 1`.

### 3. Публикация
- [ ] `ParseJobPublisher.publish(store, trigger, editionIds)`.
- [ ] `PriceChangedPublisher` — `@TransactionalEventListener(AFTER_COMMIT)` на `PriceChangedEvent` из M15 → отправка в `kr.events` с ключом `price.changed`. (Так событие уходит только если данные реально сохранены.)

### 4. Потребители
- [ ] `ParseRequestListener` — по одному `@RabbitListener` на очередь магазина (или один listener с `queues` из свойств), вызывает `StoreCrawlService.crawl(...)`. Если магазин `enabled = false` или circuit breaker открыт — подтверждает сообщение и пишет INFO, не ретраит.
- [ ] `EditionCacheEvictListener` (`kr.price-changed.cache`) — `EditionCacheService.evict(editionId)` + удаление связанных ключей поиска не нужно (TTL 10 мин достаточно).
- [ ] `kr.price-changed.beacons` — пока заглушка-логгер, логика в M19.
- [ ] **Идемпотентность:** перед обработкой `SETNX processed:v1:{messageId}` в Redis с TTL 24 ч; если ключ уже есть — пропустить.

### 5. Планировщик — `scheduler/`
- [ ] Зависимости `net.javacrumbs.shedlock:shedlock-spring` + `shedlock-provider-jdbc-template`; миграция **V10__shedlock.sql**.
- [ ] `CrawlScheduler` — `@Scheduled(cron = "${app.scheduler.crawl-cron:0 0 */6 * * *}")` + `@SchedulerLock`: для каждого включённого магазина публикует `ParseRequestMessage(SCHEDULED)`.
- [ ] `StaleOfferScheduler` — раз в сутки: предложения без `last_seen_at` за 7 дней → `in_stock = false`.
- [ ] `DropsService` из M14 и чистка `search_log` (> 90 дней) — тоже под `@SchedulerLock`.
- [ ] Часовой пояс расписаний — `Asia/Almaty` (`zone` в `@Scheduled`).

### 6. Обновление «по требованию» (stale-while-revalidate)
- [ ] `RefreshOnDemandService.requestIfStale(editionId)` — вызывается из `EditionService.getDetail` и для первой страницы поиска (асинхронно, не блокирует ответ): если у издания нет предложений свежее 6 ч → для каждого включённого магазина публикует `ParseRequestMessage(ON_DEMAND, [editionId])`.
- [ ] Защита от лавины: `SETNX parse:pending:v1:{store}:{editionId}` TTL 30 мин — повторно не ставим.

### 7. Ручной запуск
- [ ] `POST /api/v1/admin/parsers/{storeCode}/run` теперь публикует задачу и отвечает **202 Accepted** `{ "queued": true }`.

## Тесты
- [ ] `RabbitTopologyIT` (Testcontainers RabbitMQ) — все очереди и привязки созданы.
- [ ] `ParseRequestFlowIT` — опубликовать задачу → listener вызвал `StoreCrawlService` (подменённый) → при исключении 3 попытки → сообщение в `kr.dead-letter`.
- [ ] `PriceChangedFlowIT` — `OfferIngestionService` меняет цену → после коммита в очереди `price-changed` есть сообщение; при откате транзакции — нет.
- [ ] `IdempotencyIT` — одно сообщение дважды → обработано один раз.
- [ ] `RefreshOnDemandServiceTest` — свежие данные → ничего; устаревшие → задачи для всех включённых магазинов; повтор в течение 30 мин → ничего.
- [ ] `CrawlSchedulerTest` — выключенный магазин пропускается.

## Критерии приёмки
- [ ] В RabbitMQ UI видны exchange, очереди, привязки, DLQ.
- [ ] Ручной запуск возвращает 202, задача видна в очереди и обрабатывается.
- [ ] Открытие карточки с устаревшими ценами ставит задачи, ответ при этом не замедляется.
- [ ] После изменения цены карточка издания (кэш) обновляется без ожидания TTL.
- [ ] Остановка RabbitMQ не роняет API: поиск и карточки работают, публикация логирует ошибку.
- [ ] `./mvnw -q verify` зелёный.

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
