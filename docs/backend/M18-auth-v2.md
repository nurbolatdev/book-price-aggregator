# M18 — Auth v2: подтверждение email, refresh-токены, сброс пароля, лимиты

**Ветка:** `feature/m18-auth-v2` · **Зависит от:** M17 · **Оценка:** 2–3 дня

## Цель
Надёжная авторизация: короткий access-токен + refresh с ротацией, подтверждение email (без него письма маячков не уходят), сброс пароля, роль ADMIN с безопасным созданием первого админа, ограничение частоты запросов. Здесь же появляется отправка писем через очередь.

## Перед началом прочитать
`docs/03-api-contract.md` (разделы 2–3), `docs/02-data-model.md` (`users`, `email_tokens`, `refresh_tokens`).

## Задачи

### 1. Миграция V11__auth_v2.sql
Фактически `V13__auth_v2.sql`: V11 и V12 заняты (M16 — выключение Wildberries, M17 — ShedLock).
- [x] `users`: `email_verified` (существующим → TRUE), `notify_frequency`, `weekly_digest`, `updated_at`.
  Заодно `users.created_at` переведён в `TIMESTAMPTZ` (было `TIMESTAMP`), `notify_frequency` ограничен значениями `INSTANT | DAILY`.
- [x] `email_tokens`, `refresh_tokens` + индексы.

### 2. Токены — `security/` и `auth/`
- [x] Access JWT: 15 мин (`app.jwt.access-ttl`), claims `sub` (userId), `email`, `role`. Перенести настройки из `security.jwt.*` в `app.jwt.*`.
  Фильтр больше не ходит в БД: пользователь берётся из claims. Истёкший access-токен → 401 `TOKEN_EXPIRED` (клиент понимает, что пора обновить пару), токены старого формата (до M18) не принимаются.
- [x] Refresh-токен: случайные 32 байта (Base64URL), в БД — SHA-256 хеш; TTL 30 дней; **ротация** при `/auth/refresh` (старый `revoked_at = now`); повторное использование отозванного токена → отозвать все токены пользователя (защита от кражи) и 401.
  Сделано мягче, чем в задаче: повтор отзывает не все токены пользователя, а только **семейство** украденного токена (см. «Сверх плана»). Неизвестный или отозванный refresh → 401 `UNAUTHORIZED`, истёкший → 401 `TOKEN_EXPIRED`. Строка токена блокируется (`SELECT … FOR UPDATE`), поэтому два одновременных обновления одним токеном не пройдут оба.
- [x] `/auth/logout` — отзыв переданного refresh.
- [x] `TokenHasher` (SHA-256 + сравнение за постоянное время).

### 3. Подтверждение email и сброс пароля
- [x] `EmailTokenService.issue(user, purpose, ttl)` → сырой токен (в письмо), в БД хеш. TTL: VERIFY_EMAIL 24 ч, RESET_PASSWORD 1 ч. Токен одноразовый (`used_at`).
- [x] Регистрация → пользователь с `email_verified = false` → письмо со ссылкой `${app.base-url}/verify-email?token=...`.
- [x] `/auth/verify-email`, `/auth/resend-verification` (не чаще 1 раза в минуту на пользователя), `/auth/forgot-password`, `/auth/reset-password` (отзывает все refresh-токены).
- [x] Ответы `resend` и `forgot-password` — всегда 204, независимо от существования email.
  Неизвестная, использованная или «чужая» (другого назначения) ссылка → 400 `TOKEN_INVALID`, истёкшая → 401 `TOKEN_EXPIRED`. Регистрация теперь отвечает 201 `{userId, emailVerificationRequired: true}` и не выдаёт токены — вход отдельным запросом. Сроки ссылок и интервал повторной отправки — `app.auth.*`.
- [x] Вход разрешён без подтверждения email (как в макете), но в ответе `user.emailVerified = false`.

### 4. Письма — `notification/` (базовая часть)
- [x] Зависимости `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`.
- [x] `EmailMessage` (record): `messageId, type (VERIFY_EMAIL | RESET_PASSWORD | PRICE_DROP | DIGEST), to, locale, model (Map)`.
- [x] `EmailPublisher` → очередь `kr.notify.email` (создана в M17).
  Внутри транзакции письмо уходит в очередь только после коммита (`TransactionSynchronization.afterCommit`): откат регистрации не отправит ссылку. Брокер недоступен — ошибка в лог, регистрация проходит, письмо можно запросить повторно.
- [x] `EmailListener` + `EmailSender` — рендер Thymeleaf-шаблона, отправка через `JavaMailSender`, `From: KitapRadar <no-reply@...>` из `app.mail.from`.
- [x] Шаблоны `templates/email/verify-email.html`, `reset-password.html` (+ текстовая версия) в стиле макета: логотип текстом, одна главная кнопка, понятная подпись. Ссылки — абсолютные от `app.base-url`.
  Общие шапка, кнопка и подвал — `templates/email/layout.html`. Текстовые версии (`*.txt`) рендерит отдельный TEXT-резолвер Thymeleaf. Шаблоны `price-drop` и `digest` — в M19.
- [x] Повторы и DLQ — по правилам M17.

### 5. Роли и первый админ
- [x] `AdminBootstrap` (`ApplicationRunner`): если заданы `APP_ADMIN_EMAIL` и `APP_ADMIN_PASSWORD` и такого пользователя нет — создать с ролью ADMIN и `email_verified = true`. Пароль в логах не печатать.
- [x] `SecurityConfig`: `/api/v1/admin/**` → `hasRole('ADMIN')`; actuator по `docs/03-api-contract.md` (раздел 6); `@EnableMethodSecurity`.
  Публичный `/actuator/health` отдаёт только `{"status": "UP"}`, подробности (`db`, `redis`, `rabbit`, `mail`) — админу; остальной actuator, включая `/actuator/info`, — только ADMIN.

### 6. Профиль — `user/`
- [x] `GET /api/v1/me`, `PATCH /api/v1/me` (`name`, `notifyFrequency`, `weeklyDigest`).
  Ответ — тот же `user`, что при входе: `id, email, name, role, emailVerified, notifyFrequency, weeklyDigest` (`id` и `role` добавлены для фронта).

### 7. Лимиты — `security/ratelimit/`
- [x] Bucket4j + Redis (`com.bucket4j:bucket4j_jdk17-lettuce` или актуальный артефакт для Lettuce).
  `bucket4j_jdk17-core` + `bucket4j_jdk17-lettuce` 8.14.0, ключи `rate:v1:{правило}:{ключ}`. Без Redis лимиты отключаются на 30 с с WARN в логе, API продолжает работать.
- [x] `RateLimitFilter` с правилами из `app.rate-limit.*`: login 5/мин (IP+email), register и forgot-password 3/мин (IP), search 60/мин (IP), остальное API 120/мин (IP или userId).
  Лимит поиска — на `GET /api/v1/search`; подсказки `/search/suggest` считаются по общему правилу 120/мин. Для входа email читается из тела запроса, и тело передаётся контроллеру повторно. В docker backend берёт адрес клиента из `X-Forwarded-For` (nginx фронтенда его передаёт, `server.forward-headers-strategy: native`).
- [x] Превышение → 429 `RATE_LIMITED` + заголовок `Retry-After`.

### 8. CORS
- [x] Разрешить origin из `app.cors.allowed-origins` (dev: `http://localhost:5173`, docker: `http://localhost`), методы GET/POST/PATCH/DELETE, заголовок `Authorization`.
  Переменная `APP_CORS_ALLOWED_ORIGINS` (через запятую). Наружу отданы заголовки `Retry-After` и `X-Request-Id`.

## Тесты
- [x] `AuthFlowIT` — регистрация → письмо с токеном отправлено → verify → `emailVerified = true`. В тестах SMTP — GreenMail (`com.icegreen:greenmail-junit5`), не Mailpit.
  Письма идут по-настоящему: регистрация → `kr.notify.email` → `EmailListener` → SMTP GreenMail (`support/AbstractMailIT`). Там же `PasswordResetIT`.
- [x] `RefreshTokenIT` — ротация; повтор отозванного токена отзывает все; истёкший → 401 `TOKEN_EXPIRED`.
- [x] `PasswordResetIT` — reset отзывает refresh-токены, старый пароль не работает, токен одноразовый.
- [x] `RateLimitIT` — 6-я попытка входа за минуту → 429.
- [x] `AdminBootstrapTest`, `SecurityIT` — USER получает 403 на `/api/v1/admin/**`.
- [x] `EmailSenderTest` — шаблон рендерится, ссылки абсолютные.
  Ещё unit-тесты: `JwtServiceTest`, `TokenHasherTest`; интеграционные: `ProfileControllerIT`, `CorsIT`. Исправлены тесты M17: `RabbitTopologyIT` ждал сообщение без паузы (гонка с асинхронной публикацией), в `AbstractListenerIT` отправка писем подменена, чтобы старые письма из очереди не падали в DLQ, которую читают эти тесты.

## Критерии приёмки
- [x] Регистрация отправляет письмо — оно видно в Mailpit (`http://localhost:8025`), ссылка подтверждает email.
- [x] Access живёт 15 мин, refresh обновляет пару, logout отзывает.
- [x] Сброс пароля работает end-to-end через Mailpit.
- [x] Первый админ создаётся из переменных окружения; USER не попадает в `/admin`.
- [x] Лимиты срабатывают с 429 и `Retry-After`.
- [ ] `./mvnw -q verify` зелёный.
  Unit-тесты зелёные (154). Интеграционные прогнаны на локальных PostgreSQL 16, Redis и RabbitMQ 3.12 вместо Testcontainers (в среде разработки нет Docker): зелёные все, кроме `MigrationIT`, которому нужен контейнер (131 из 134).

Критерии проверены вживую: приложение (jar) + локальные PostgreSQL, Redis, RabbitMQ и Mailpit 1.21. Письма подтверждения и сброса видны в Mailpit, ссылки работают один раз; refresh выдаёт новую пару, повтор старого завершает сессию; после сброса пароля старый пароль и старые refresh-токены не работают; админ создаётся из `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD`, USER на `/admin` получает 403; шестая попытка входа → 429 с `Retry-After: 60`; при остановке Redis вход и API работают, после запуска лимиты возвращаются.

## Сверх плана (согласовано при приёмке)
Полный список отличий от задания и принятых решений — [M18-report.md](M18-report.md), архитектурное решение — [ADR-004](../adr/ADR-004-refresh-token-sessions.md).

- [x] **Семейства refresh-токенов** (`V14__refresh_token_families.sql`, колонка `family_id`). Вход начинает семейство, ротация его сохраняет. Повтор отозванного токена завершает только эту сессию (вор и владелец этого устройства входят заново), остальные устройства пользователя остаются в системе. Событие пишется в `system_events` (WARN, источник `auth`: `userId`, `familyId`, User-Agent до 255 символов; время — `created_at`). Ни токен, ни его хеш, ни IP-адрес в журнал не пишутся: IP — персональные данные. Сброс пароля по-прежнему завершает все сессии.
  Токены, выданные до V14, нельзя связать с их входом (цепочки не хранились), поэтому миграция объединяет все старые токены пользователя в одно семейство: повтор старого токена завершает все старые сессии пользователя, как до семейств. При выкатке V13 и V14 идут вместе, и таблица ещё пуста.
- [x] **Лимит сессий:** не больше `app.jwt.max-sessions` (по умолчанию 10, `JWT_MAX_SESSIONS`) активных семейств на пользователя; новый вход сверх лимита завершает сессию, которая дольше всех не обновлялась (время последнего обновления = время создания её действующего токена). Вход блокирует строку пользователя (`SELECT … FOR UPDATE`), поэтому одновременные входы одного пользователя не превысят лимит.
- [x] **Очистка токенов** (`TokenCleanupService`, вызывается из ночного `CleanupScheduler` под ShedLock). Refresh-токены удаляются через 30 дней после `expires_at`, отозванные или нет; ссылки из писем — через 7 дней после `expires_at`, использованные или нет (`app.auth.cleanup.*`). Отозванный, но ещё не истёкший токен не удаляется: без него не сработает обнаружение повтора. Удаление пакетами по 1000 строк, каждый пакет — своя транзакция; индексы по `expires_at` — `V15__auth_token_expiry_indexes.sql`. Аудит — в `system_events`, а не в таблицах токенов.
  Осознанный компромисс: отозванный токен хранится до 30 дней после истечения (всего до 60 дней от выдачи). Предъявленный позже, он уже «неизвестен», и повтор не обнаруживается; к этому времени истекает и цепочка, выданная по украденному токену, если её не обновляли.
- [x] Тесты: `RefreshTokenIT` (повтор завершает одну сессию и пишет событие без токена и с обрезанным User-Agent; одиннадцатый вход завершает самую давнюю по обновлению сессию; два одновременных входа при 9 сессиях оставляют ровно 10 — без блокировки тест падает с 11), `TokenCleanupServiceIT` (удаляются только записи старше срока хранения, пакетами; точная граница ±1 с при фиксированном времени; повтор отозванного токена после очистки по-прежнему ловится), `MigrationIT` (старые токены пользователя после V14 — одно семейство, у разных пользователей — разные; нужен Docker, вручную проверено на локальной базе). Обновление живой базы с токенами до семейств проверено: V14 выдала каждому токену своё семейство, обновление токена работает.

## План коммитов
1. `feat: auth v2 schema with email tokens and refresh tokens`
2. `feat: short access token and rotating refresh tokens`
3. `feat: email sending through rabbitmq with thymeleaf templates`
4. `feat: email verification flow`
5. `feat: password reset flow`
6. `feat: admin role bootstrap and admin route protection`
7. `feat: profile endpoints`
8. `feat: redis rate limiting with bucket4j`
9. `chore: cors configuration`
10. `test: auth v2 integration tests`
11. `docs: update milestone status`

Сверх плана: `feat: refresh token families and session limit`, `feat: scheduled cleanup of expired auth tokens`, `docs: describe token families and cleanup`, `fix: harden token families and session limit`.

## Не входит
Письма о снижении цены и дайджест (M19).
