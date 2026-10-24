# M20 — Admin API и наблюдаемость

**Ветка:** `feature/m20-admin-observability` · **Зависит от:** M19 · **Оценка:** 2 дня

## Цель
Дать админ-панели (макет экрана 9) все данные: KPI, здоровье сервисов, парсеры, очереди, активность, журнал, топ запросов, пользователи. Добавить метрики, по которым видно, что система работает.

## Перед началом прочитать
`docs/03-api-contract.md` (раздел 5), макет «Админ-панель — обзор».

## Задачи

### 1. Статистика — `admin/AdminStatsService`
- [x] `GET /api/v1/admin/stats?period=24h|7d|30d` — один SQL на группу показателей (CTE), результат в Redis `admin:v1:stats:{period}` на 1 мин:
  - пользователи: всего, новые за период, подтвердившие email;
  - активные за период (уникальные `user_id` в `search_log` + входы — достаточно `search_log` и `refresh_tokens.created_at`);
  - маячки: активные, сработавшие за период;
  - письма: SENT / FAILED за период;
  - поиски за период; **cache hit %** — из метрик Micrometer (см. п. 5);
  - книги / издания / предложения (MATCHED) / на проверке (PENDING_REVIEW).
- [x] `GET /api/v1/admin/activity?days=14` — по дням: поиски, регистрации, новые маячки, письма (`generate_series` по датам, чтобы были нулевые дни).

### 2. Здоровье — `admin/AdminHealthService`
- [x] `GET /api/v1/admin/health` — на основе `HealthEndpoint` Spring Boot: `db`, `redis`, `rabbit`, `mail` + свой `HealthIndicator` `scheduler` (время последнего и следующего запуска обхода из ShedLock/свойств) + метрики: p95 HTTP (`http.server.requests`), подключения пула Hikari, память Redis (`INFO memory`).
- [x] Включить `management.health.mail.enabled`.

### 3. Парсеры — `admin/AdminParserService`
- [x] `GET /api/v1/admin/parsers` — для каждого магазина: `enabled`, последний запуск (статус, время, длительность), % успешных страниц за 24 ч (`pages_ok / (pages_ok + pages_failed)`), предложений MATCHED, среднее время запуска, состояние circuit breaker (`CircuitBreakerRegistry`).
- [x] `PATCH /api/v1/admin/stores/{code} {enabled}` — с записью в `system_events`, сбросом кэша справочника.
- [x] Ручной запуск уже есть (M17).

### 4. Очереди — `admin/RabbitAdminClient`
- [x] Клиент RabbitMQ Management HTTP API через `RestClient`: `GET /api/queues/%2F` → по очередям `kr.*`: `messages_ready`, `messages_unacknowledged`, `consumers`, `message_stats.ack_details.rate`. Адрес и учётные данные — `app.rabbit-admin.*`, переменные окружения.
- [x] `GET /api/v1/admin/queues`, кэш 15 с.
- [x] `POST /api/v1/admin/queues/dead-letter/requeue` — перекладывает сообщения из `kr.dead-letter` в исходный exchange (по заголовку `x-death`), до 500 за вызов, возвращает количество.

### 5. Метрики — Micrometer
- [x] Зависимость `io.micrometer:micrometer-registry-prometheus`; `/actuator/prometheus` только для ADMIN.
- [x] Свои метрики:
  - `kr.search.requests` (counter, тег `cache=hit|miss`);
  - `kr.parser.run.duration` (timer, теги `store`, `status`);
  - `kr.parser.offers.upserted`, `kr.parser.prices.changed` (counter, тег `store`);
  - `kr.beacon.triggered` (counter);
  - `kr.mail.sent` (counter, теги `type`, `result`).
- [x] Cache hit % в KPI считается из `kr.search.requests`.

### 6. Журнал, поиск, пользователи
- [x] `GET /api/v1/admin/events?level=&page=` — `system_events`, новые сверху.
- [x] `GET /api/v1/admin/search/top?period=` — топ-20 по `query_norm`; `GET /api/v1/admin/search/zero-results?period=` — запросы с `results_count = 0`.
- [x] `GET /api/v1/admin/users?page=` — email (полностью — это админка), имя, дата регистрации, verified, число маячков.
- [x] Проверить, что ключевые события пишутся в `system_events`: запуск/ошибка парсера, circuit breaker открыт/закрыт, сообщения в DLQ, ошибки отправки писем, изменение `enabled` магазина.

### 7. (по желанию) Prometheus + Grafana
- [ ] Отдельный профиль `docker compose --profile monitoring up`: `prometheus` (scrape backend с токеном админа или отдельным ключом) и `grafana` с одним дашбордом (`infra/grafana/dashboards/kitapradar.json`). Не обязательно для приёмки.

## Тесты
- [ ] `AdminStatsServiceIT` — на seed-данных значения совпадают с ожидаемыми.
- [x] `AdminParserServiceIT` — расчёт % успеха и состояния.
- [x] `RabbitAdminClientTest` (WireMock) — разбор ответа Management API.
- [ ] `DeadLetterRequeueIT` — сообщение из DLQ возвращается в рабочую очередь.
- [ ] `AdminSecurityIT` — все `/api/v1/admin/**` → 403 для USER, 200 для ADMIN.

## Критерии приёмки
- [ ] Все эндпоинты раздела 5 контракта работают и описаны в Swagger.
- [ ] `/api/v1/admin/stats` отвечает < 100 мс на тёплом кэше.
- [ ] Метрики видны в `/actuator/prometheus`.
- [ ] Остановка Redis или RabbitMQ отражается в `/admin/health` как DOWN, API не падает.
- [ ] `./mvnw -q verify` зелёный.

## План коммитов
1. `feat: admin stats and daily activity`
2. `feat: admin health with custom scheduler indicator`
3. `feat: admin parser status and store toggle`
4. `feat: rabbitmq management client and queue stats`
5. `feat: dead letter requeue for admins`
6. `feat: custom micrometer metrics and prometheus endpoint`
7. `feat: admin events, search analytics and users list`
8. `test: admin api integration tests`
9. `docs: update milestone status`
