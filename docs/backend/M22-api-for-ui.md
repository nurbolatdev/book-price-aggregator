# M22 — API под макеты frontend

**Ветка:** `feature/m22-api-for-ui` · **Зависит от:** M21 · **Оценка:** 3 дня

## Цель
Закрыть пробелы между макетами (`docs/frontend/mockups/`, 23 экрана) и API: у каждой кнопки и каждого числа на экране должен быть источник данных. После M22 frontend F1–F7 не меняет backend.

Сверка макетов с API — `docs/frontend/SCREENS.md` (пометки **[M22]**).

## Перед началом прочитать
`docs/frontend/SCREENS.md`, `docs/03-api-contract.md` (разделы, помеченные «M22»), `docs/02-data-model.md`, ADR-004.

## Задачи

### 1. Поиск: каталог без строки и несколько значений фильтра — `search/`
- [x] `GET /api/v1/search`: `q` необязателен. Без `q` — режим каталога: те же фильтры, сортировка по умолчанию `STORES_DESC`, `RELEVANCE` без `q` → 422. С `q` — как сейчас (2–200 символов). Без `q` в `search_log` не писать.
- [x] `binding` и `language` принимают списки через запятую (`binding=HARDCOVER,PAPERBACK`, `language=ru,kk`), как `stores`. Одно значение — работает как раньше.
- [x] Ключ кэша учитывает новые параметры (отсортированные списки).

### 2. Счётчики фильтров — `GET /api/v1/search/facets`
- [x] Те же параметры, что у `/search`, кроме `sort/page/size`. Ответ:
  ```json
  { "stores": [ { "code": "FLIP", "count": 6 } ],
    "bindings": [ { "value": "HARDCOVER", "count": 7 } ],
    "languages": [ { "value": "ru", "count": 11 } ] }
  ```
- [x] Каждый счётчик считается со всеми фильтрами, **кроме своего** (иначе выбранный магазин обнуляет остальные). Число изданий, не предложений. Только включённые магазины.
- [x] Кэш Redis 60 с, ключ как у поиска; лимит запросов — как у `/search`.

### 3. Популярные запросы для главной — `GET /api/v1/search/popular?limit=6`
- [x] Публичный. Топ `query_norm` из `search_log` за 7 дней, только запросы с `results_count > 0`; отдаётся исходная строка самого частого варианта написания.
- [x] Меньше `limit` запросов в логе → дополнить списком из `app.home.popular-fallback` (application.yml), без повторов.
- [x] Кэш 1 ч. `limit` 1–12.

### 4. Подборка «подешевели»: число магазинов
- [x] `DropItem.storesCount` — число магазинов с предложением этого издания в наличии. Нужно для подписи «ещё в N магазинах».

### 5. Похожие книги — `GET /api/v1/editions/{id}/similar?limit=4`
- [x] Ответ — список `{bookId, editionId, title, author, coverUrl, bestPrice}` (`editionId` — издание с лучшей ценой этой книги).
- [x] Порядок: другие книги того же автора (по числу магазинов), затем книги на том же языке с наибольшим числом предложений; без текущей книги; только книги с ценой в наличии.
- [x] `limit` 1–12; кэш 1 ч; неизвестное издание → 404.

### 6. Маячки: поиск по изданию и новые поля — `beacon/`
- [x] `GET /api/v1/beacons?editionId=41` — фильтр по изданию (0 или 1 элемент): карточка книги показывает «Маячок стоит · изменить».
- [x] В `BeaconResponse` добавить: `currentBestUrl` (ссылка лучшего предложения), `createdAt`, `statusChangedAt`.
- [x] Миграция **V19**: `beacons.status_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, заполнить существующие из `updated_at` (или `created_at`). Обновлять при каждой смене статуса (пауза, возобновление, срабатывание).

### 7. Профиль: пароль, выход везде, удаление — `user/`, `auth/`
- [x] `POST /api/v1/me/password {currentPassword, newPassword, refreshToken}` → 204. Неверный текущий пароль → 422 с `fieldErrors[currentPassword]`. Отзываются все семейства refresh-токенов, кроме семейства переданного `refreshToken` (текущее устройство остаётся в системе; ADR-004). Письмо «Пароль изменён» — по желанию.
- [x] `POST /api/v1/auth/logout-all` (авторизованный) → 204, отзывает все семейства пользователя.
- [x] `DELETE /api/v1/me {password}` → 204. Неверный пароль → 422. Удаляются пользователь и всё связанное (маячки, уведомления, токены — каскадом в БД, проверить внешние ключи). В `system_events` — `USER_DELETED` без email (только id). Пользователь с ролью ADMIN себя удалить не может → 409.
- [x] Лимит запросов для `me/password` и `DELETE /me` — как у `login` (5 в минуту).

### 8. Еженедельная подборка скидок
Флаг `users.weekly_digest` хранится с M18, но письма нет — экран профиля и «Мои маячки» обещают «Еженедельную подборку».
- [x] `WeeklyDigestScheduler` — по понедельникам в 10:00 Asia/Almaty (`app.scheduler.weekly-digest-cron`), ShedLock.
- [x] Получатели: `weekly_digest = true` и `email_verified = true`. Содержание: до 8 изданий из подборки «подешевели за неделю» (тот же сервис, что `/home/drops`); если подборка пуста — письмо не отправляется.
- [x] Письмо идёт через ту же надёжную доставку, что письма маячков (таблица `notifications` и RabbitMQ). Миграция **V19** (та же или следующая): `notifications.delivery` допускает `WEEKLY`, `beacon_id` уже необязателен. Не больше одного письма в неделю на пользователя (уникальность по пользователю и ISO-неделе).
- [x] Шаблон `weekly-digest.html` — по макету `docs/frontend/mockups/24-email-weekly-digest.html` (стиль письма 06): заголовок, карточки книг с ценой и скидкой, кнопка «Все скидки недели» → `/deals`; внизу ссылка «Отключить подборку» → `/profile`.

### 9. Админка: поиск пользователей и журнал писем — `admin/`
- [x] `GET /api/v1/admin/users?q=&verified=&page=&size=` — `q` ищет по email и имени (ILIKE), `verified` — `true|false`.
- [x] `GET /api/v1/admin/notifications?status=&delivery=&page=&size=` — страница `{id, createdAt, sentAt, userEmail, title, delivery, status, error}`; `title` — название книги из `payload` или «Сводка за день» / «Подборка недели»; новые сверху.
- [x] `POST /api/v1/admin/notifications/{id}/retry` — только для `FAILED`: статус `QUEUED`, `published_at = NULL`, ошибка очищается; доставка подхватит сама. Не `FAILED` → 409. Запись в `system_events`.

### 10. Удаление старого избранного
Избранное заменено маячками (решение фазы 2); новый frontend его не использует.
- [x] Удалить пакет `favorite/`, эндпоинты `/api/v1/favorites/**`, тесты.
- [x] Миграция **V20**: `DROP TABLE favorites`.
- [x] Старый React-фронтенд фазы 1 перестанет показывать избранное — он полностью заменяется в F1.

### 11. Правила пароля и тексты ошибок валидации
Макеты и `SCREENS.md` обещают «Минимум 8 символов», backend сейчас принимает от 6 и отвечает по-английски — frontend показывает `fieldErrors` прямо под полями.
- [x] `RegisterRequest`, `ResetPasswordRequest`, новый запрос смены пароля: пароль 8–100 символов, сообщение «Минимум 8 символов». Вход (`/auth/login`) длину не проверяет — пользователи со старыми короткими паролями входят как раньше.
- [x] Все сообщения валидации в DTO — на русском, как у маячков («должно быть больше 0»); проверить `RegisterRequest`, `ResetPasswordRequest`, `EmailRequest`, `UpdateProfileRequest`.
- [x] Описание `conditionType` в `CreateBeaconRequest`: «ANY_DROP, BELOW_PRICE или DISCOUNT_PERCENT» (сейчас `ANY_DROP` не упомянут, хотя поддерживается).

### 12. Refresh-токен в cookie `HttpOnly` — `auth/`
Сейчас refresh-токен приходит в теле ответа и frontend хранит его в `localStorage`, откуда его может прочитать любой внедрённый скрипт (XSS). В cookie `HttpOnly` скрипт его не видит.
- [x] `login`, `refresh` ставят cookie `kr_refresh`: `HttpOnly`, `Secure` (кроме профиля `dev`), `SameSite=Strict`, `Path=/api/v1/auth`, `Max-Age` = срок жизни refresh-токена. `logout`, `logout-all`, смена и сброс пароля, удаление аккаунта — очищают cookie (`Max-Age=0`).
- [x] `refresh` и `logout` берут токен из cookie; если cookie нет — из тела (переходный режим, чтобы frontend F1 работал до F4). В ответе `refreshToken` в теле пока остаётся, помечен в Swagger `deprecated`; убрать в F4 одновременно с переходом frontend.
- [x] `POST /me/password` определяет текущую сессию по cookie (поле `refreshToken` в теле — только в переходном режиме).
- [x] CSRF: cookie уходит только на `/api/v1/auth/**` и только со своего сайта (`SameSite=Strict`, frontend и API на одном домене через nginx); дополнительно `refresh`/`logout` принимают запрос только с `Origin`, совпадающим с `app.cors.allowed-origins` (иначе 403).
- [x] CORS: `allowCredentials=true` для разрешённых origin.

### 13. Две вкладки обновляют токен одновременно — окно повтора
Проблема: в двух открытых вкладках истекает access-токен, обе почти одновременно шлют `POST /auth/refresh` с одним и тем же refresh-токеном. Первая ротирует его, вторая приходит со «старым» — backend считает это кражей (ADR-004) и завершает всю сессию: пользователь вылетает в обеих вкладках.
- [x] Окно повтора `app.jwt.refresh-reuse-grace` (по умолчанию 10 с, `JWT_REFRESH_REUSE_GRACE`): если отозванный **ротацией** токен предъявлен в течение окна после ротации, — не завершать сессию, а выдать текущий действующий токен этого семейства (тот же, что получила первая вкладка) и новый access-токен. Событие `system_events` INFO, не WARN.
- [x] Вне окна, а также для токена, отозванного выходом, сменой пароля или лимитом сессий, — поведение прежнее (кража → сессия завершается).
- [x] Ротация и выдача — в одной транзакции с блокировкой строки семейства (`SELECT … FOR UPDATE`), чтобы два параллельных запроса не создали два новых токена.
- [x] ADR-005 «Refresh-токен в cookie и окно повтора» в `docs/adr/` (что меняется относительно ADR-004 и почему); frontend-часть (Web Locks между вкладками) — в F4.

### 14. Документация
- [x] `docs/03-api-contract.md`: снять пометку «план M22» у сделанного, описать форматы ответов.
- [x] Обновить `docs/api/openapi.json` (тест `OpenApiSpecIT`), Swagger-описания на русском, как у остальных.
- [x] `docs/02-data-model.md`: новые колонки, удалённая таблица.

## Тесты
- [x] `SearchCatalogModeIT` — поиск без `q`, списки `binding/language`, 422 для `RELEVANCE` без `q`.
- [x] `SearchFacetsIT` — счётчик фильтра не зависит от собственного значения фильтра.
- [x] `PopularQueriesServiceTest` — отбор, дополнение запасным списком, без повторов.
- [x] `SimilarEditionsIT` — порядок (тот же автор первым), без текущей книги.
- [x] `BeaconControllerIT` — фильтр `editionId`, новые поля, `statusChangedAt` меняется при паузе.
- [x] `ProfileSecurityIT` — смена пароля оставляет текущее семейство и отзывает остальные; `logout-all`; удаление аккаунта (каскад, 409 для ADMIN).
- [x] `WeeklyDigestIT` — получатели, одно письмо в неделю, пустая подборка — без письма.
- [x] `AdminNotificationsIT` — фильтры, повтор только для FAILED.
- [x] `RefreshCookieIT` — cookie ставится при входе и обновлении с нужными атрибутами, очищается при выходе; refresh из cookie и из тела (переходный режим); чужой `Origin` → 403.
- [x] `RefreshReuseGraceIT` — два параллельных refresh одним токеном → оба успешны, один новый токен, сессия жива; повтор через 11 с → сессия завершена; повтор токена после logout в окне → сессия завершена.
- [x] `PasswordRulesIT` — 7 символов → 422 с русским сообщением у регистрации, сброса и смены пароля; вход со старым 6-символьным паролем работает.

## Критерии приёмки
- [x] Каждая пометка **[M22]** из `docs/frontend/SCREENS.md` закрыта работающим эндпоинтом или полем.
- [x] Все новые эндпоинты в Swagger и в `docs/api/openapi.json`.
- [x] Миграции применяются на чистую БД (`docker compose down -v && up`).
- [x] `./mvnw -q verify` зелёный, покрытие JaCoCo не ниже порога M21.

## План коммитов
1. `feat: catalog mode and multi-value filters in search`
2. `feat: search facets endpoint`
3. `feat: popular queries and stores count in drops`
4. `feat: similar editions endpoint`
5. `feat: beacon edition filter and status timestamps`
6. `feat: password change, logout everywhere and account deletion`
7. `feat: weekly digest email`
8. `feat: admin user search and notification log with retry`
9. `refactor: remove legacy favorites`
10. `fix: password length and russian validation messages`
11. `feat: refresh token in httponly cookie`
12. `fix: refresh reuse grace window for parallel tabs`
13. `docs: api contract, openapi and adr-005 for m22`
