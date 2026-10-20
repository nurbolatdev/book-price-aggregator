# M18 — Auth v2: подтверждение email, refresh-токены, сброс пароля, лимиты

**Ветка:** `feature/m18-auth-v2` · **Зависит от:** M17 · **Оценка:** 2–3 дня

## Цель
Надёжная авторизация: короткий access-токен + refresh с ротацией, подтверждение email (без него письма маячков не уходят), сброс пароля, роль ADMIN с безопасным созданием первого админа, ограничение частоты запросов. Здесь же появляется отправка писем через очередь.

## Перед началом прочитать
`docs/03-api-contract.md` (разделы 2–3), `docs/02-data-model.md` (`users`, `email_tokens`, `refresh_tokens`).

## Задачи

### 1. Миграция V11__auth_v2.sql
- [ ] `users`: `email_verified` (существующим → TRUE), `notify_frequency`, `weekly_digest`, `updated_at`.
- [ ] `email_tokens`, `refresh_tokens` + индексы.

### 2. Токены — `security/` и `auth/`
- [ ] Access JWT: 15 мин (`app.jwt.access-ttl`), claims `sub` (userId), `email`, `role`. Перенести настройки из `security.jwt.*` в `app.jwt.*`.
- [ ] Refresh-токен: случайные 32 байта (Base64URL), в БД — SHA-256 хеш; TTL 30 дней; **ротация** при `/auth/refresh` (старый `revoked_at = now`); повторное использование отозванного токена → отозвать все токены пользователя (защита от кражи) и 401.
- [ ] `/auth/logout` — отзыв переданного refresh.
- [ ] `TokenHasher` (SHA-256 + сравнение за постоянное время).

### 3. Подтверждение email и сброс пароля
- [ ] `EmailTokenService.issue(user, purpose, ttl)` → сырой токен (в письмо), в БД хеш. TTL: VERIFY_EMAIL 24 ч, RESET_PASSWORD 1 ч. Токен одноразовый (`used_at`).
- [ ] Регистрация → пользователь с `email_verified = false` → письмо со ссылкой `${app.base-url}/verify-email?token=...`.
- [ ] `/auth/verify-email`, `/auth/resend-verification` (не чаще 1 раза в минуту на пользователя), `/auth/forgot-password`, `/auth/reset-password` (отзывает все refresh-токены).
- [ ] Ответы `resend` и `forgot-password` — всегда 204, независимо от существования email.
- [ ] Вход разрешён без подтверждения email (как в макете), но в ответе `user.emailVerified = false`.

### 4. Письма — `notification/` (базовая часть)
- [ ] Зависимости `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`.
- [ ] `EmailMessage` (record): `messageId, type (VERIFY_EMAIL | RESET_PASSWORD | PRICE_DROP | DIGEST), to, locale, model (Map)`.
- [ ] `EmailPublisher` → очередь `kr.notify.email` (создана в M17).
- [ ] `EmailListener` + `EmailSender` — рендер Thymeleaf-шаблона, отправка через `JavaMailSender`, `From: KitapRadar <no-reply@...>` из `app.mail.from`.
- [ ] Шаблоны `templates/email/verify-email.html`, `reset-password.html` (+ текстовая версия) в стиле макета: логотип текстом, одна главная кнопка, понятная подпись. Ссылки — абсолютные от `app.base-url`.
- [ ] Повторы и DLQ — по правилам M17.

### 5. Роли и первый админ
- [ ] `AdminBootstrap` (`ApplicationRunner`): если заданы `APP_ADMIN_EMAIL` и `APP_ADMIN_PASSWORD` и такого пользователя нет — создать с ролью ADMIN и `email_verified = true`. Пароль в логах не печатать.
- [ ] `SecurityConfig`: `/api/v1/admin/**` → `hasRole('ADMIN')`; actuator по `docs/03-api-contract.md` (раздел 6); `@EnableMethodSecurity`.

### 6. Профиль — `user/`
- [ ] `GET /api/v1/me`, `PATCH /api/v1/me` (`name`, `notifyFrequency`, `weeklyDigest`).

### 7. Лимиты — `security/ratelimit/`
- [ ] Bucket4j + Redis (`com.bucket4j:bucket4j_jdk17-lettuce` или актуальный артефакт для Lettuce).
- [ ] `RateLimitFilter` с правилами из `app.rate-limit.*`: login 5/мин (IP+email), register и forgot-password 3/мин (IP), search 60/мин (IP), остальное API 120/мин (IP или userId).
- [ ] Превышение → 429 `RATE_LIMITED` + заголовок `Retry-After`.

### 8. CORS
- [ ] Разрешить origin из `app.cors.allowed-origins` (dev: `http://localhost:5173`, docker: `http://localhost`), методы GET/POST/PATCH/DELETE, заголовок `Authorization`.

## Тесты
- [ ] `AuthFlowIT` — регистрация → письмо с токеном отправлено → verify → `emailVerified = true`. В тестах SMTP — GreenMail (`com.icegreen:greenmail-junit5`), не Mailpit.
- [ ] `RefreshTokenIT` — ротация; повтор отозванного токена отзывает все; истёкший → 401 `TOKEN_EXPIRED`.
- [ ] `PasswordResetIT` — reset отзывает refresh-токены, старый пароль не работает, токен одноразовый.
- [ ] `RateLimitIT` — 6-я попытка входа за минуту → 429.
- [ ] `AdminBootstrapTest`, `SecurityIT` — USER получает 403 на `/api/v1/admin/**`.
- [ ] `EmailSenderTest` — шаблон рендерится, ссылки абсолютные.

## Критерии приёмки
- [ ] Регистрация отправляет письмо — оно видно в Mailpit (`http://localhost:8025`), ссылка подтверждает email.
- [ ] Access живёт 15 мин, refresh обновляет пару, logout отзывает.
- [ ] Сброс пароля работает end-to-end через Mailpit.
- [ ] Первый админ создаётся из переменных окружения; USER не попадает в `/admin`.
- [ ] Лимиты срабатывают с 429 и `Retry-After`.
- [ ] `./mvnw -q verify` зелёный.

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

## Не входит
Письма о снижении цены и дайджест (M19).
