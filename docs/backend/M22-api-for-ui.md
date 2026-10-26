# M22 — API под макеты frontend

**Ветка:** `feature/m22-api-for-ui` · **Зависит от:** M21 · **Оценка:** 2–3 дня

## Цель
Закрыть пробелы между макетами (`docs/frontend/mockups/`, 23 экрана) и API: у каждой кнопки и каждого числа на экране должен быть источник данных. После M22 frontend F1–F7 не меняет backend.

Сверка макетов с API — `docs/frontend/SCREENS.md` (пометки **[M22]**).

## Перед началом прочитать
`docs/frontend/SCREENS.md`, `docs/03-api-contract.md` (разделы, помеченные «M22»), `docs/02-data-model.md`, ADR-004.

## Задачи

### 1. Поиск: каталог без строки и несколько значений фильтра — `search/`
- [ ] `GET /api/v1/search`: `q` необязателен. Без `q` — режим каталога: те же фильтры, сортировка по умолчанию `STORES_DESC`, `RELEVANCE` без `q` → 422. С `q` — как сейчас (2–200 символов). Без `q` в `search_log` не писать.
- [ ] `binding` и `language` принимают списки через запятую (`binding=HARDCOVER,PAPERBACK`, `language=ru,kk`), как `stores`. Одно значение — работает как раньше.
- [ ] Ключ кэша учитывает новые параметры (отсортированные списки).

### 2. Счётчики фильтров — `GET /api/v1/search/facets`
- [ ] Те же параметры, что у `/search`, кроме `sort/page/size`. Ответ:
  ```json
  { "stores": [ { "code": "FLIP", "count": 6 } ],
    "bindings": [ { "value": "HARDCOVER", "count": 7 } ],
    "languages": [ { "value": "ru", "count": 11 } ] }
  ```
- [ ] Каждый счётчик считается со всеми фильтрами, **кроме своего** (иначе выбранный магазин обнуляет остальные). Число изданий, не предложений. Только включённые магазины.
- [ ] Кэш Redis 60 с, ключ как у поиска; лимит запросов — как у `/search`.

### 3. Популярные запросы для главной — `GET /api/v1/search/popular?limit=6`
- [ ] Публичный. Топ `query_norm` из `search_log` за 7 дней, только запросы с `results_count > 0`; отдаётся исходная строка самого частого варианта написания.
- [ ] Меньше `limit` запросов в логе → дополнить списком из `app.home.popular-fallback` (application.yml), без повторов.
- [ ] Кэш 1 ч. `limit` 1–12.

### 4. Подборка «подешевели»: число магазинов
- [ ] `DropItem.storesCount` — число магазинов с предложением этого издания в наличии. Нужно для подписи «ещё в N магазинах».

### 5. Похожие книги — `GET /api/v1/editions/{id}/similar?limit=4`
- [ ] Ответ — список `{bookId, editionId, title, author, coverUrl, bestPrice}` (`editionId` — издание с лучшей ценой этой книги).
- [ ] Порядок: другие книги того же автора (по числу магазинов), затем книги на том же языке с наибольшим числом предложений; без текущей книги; только книги с ценой в наличии.
- [ ] `limit` 1–12; кэш 1 ч; неизвестное издание → 404.

### 6. Маячки: поиск по изданию и новые поля — `beacon/`
- [ ] `GET /api/v1/beacons?editionId=41` — фильтр по изданию (0 или 1 элемент): карточка книги показывает «Маячок стоит · изменить».
- [ ] В `BeaconResponse` добавить: `currentBestUrl` (ссылка лучшего предложения), `createdAt`, `statusChangedAt`.
- [ ] Миграция **V19**: `beacons.status_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, заполнить существующие из `updated_at` (или `created_at`). Обновлять при каждой смене статуса (пауза, возобновление, срабатывание).

### 7. Профиль: пароль, выход везде, удаление — `user/`, `auth/`
- [ ] `POST /api/v1/me/password {currentPassword, newPassword, refreshToken}` → 204. Неверный текущий пароль → 422 с `fieldErrors[currentPassword]`. Отзываются все семейства refresh-токенов, кроме семейства переданного `refreshToken` (текущее устройство остаётся в системе; ADR-004). Письмо «Пароль изменён» — по желанию.
- [ ] `POST /api/v1/auth/logout-all` (авторизованный) → 204, отзывает все семейства пользователя.
- [ ] `DELETE /api/v1/me {password}` → 204. Неверный пароль → 422. Удаляются пользователь и всё связанное (маячки, уведомления, токены — каскадом в БД, проверить внешние ключи). В `system_events` — `USER_DELETED` без email (только id). Пользователь с ролью ADMIN себя удалить не может → 409.
- [ ] Лимит запросов для `me/password` и `DELETE /me` — как у `login` (5 в минуту).

### 8. Еженедельная подборка скидок
Флаг `users.weekly_digest` хранится с M18, но письма нет — экран профиля и «Мои маячки» обещают «Еженедельную подборку».
- [ ] `WeeklyDigestScheduler` — по понедельникам в 10:00 Asia/Almaty (`app.scheduler.weekly-digest-cron`), ShedLock.
- [ ] Получатели: `weekly_digest = true` и `email_verified = true`. Содержание: до 8 изданий из подборки «подешевели за неделю» (тот же сервис, что `/home/drops`); если подборка пуста — письмо не отправляется.
- [ ] Письмо идёт через ту же надёжную доставку, что письма маячков (таблица `notifications` и RabbitMQ). Миграция **V19** (та же или следующая): `notifications.delivery` допускает `WEEKLY`, `beacon_id` уже необязателен. Не больше одного письма в неделю на пользователя (уникальность по пользователю и ISO-неделе).
- [ ] Шаблон `weekly-digest.html` — по макету `docs/frontend/mockups/24-email-weekly-digest.html` (стиль письма 06): заголовок, карточки книг с ценой и скидкой, кнопка «Все скидки недели» → `/deals`; внизу ссылка «Отключить подборку» → `/profile`.

### 9. Админка: поиск пользователей и журнал писем — `admin/`
- [ ] `GET /api/v1/admin/users?q=&verified=&page=&size=` — `q` ищет по email и имени (ILIKE), `verified` — `true|false`.
- [ ] `GET /api/v1/admin/notifications?status=&delivery=&page=&size=` — страница `{id, createdAt, sentAt, userEmail, title, delivery, status, error}`; `title` — название книги из `payload` или «Сводка за день» / «Подборка недели»; новые сверху.
- [ ] `POST /api/v1/admin/notifications/{id}/retry` — только для `FAILED`: статус `QUEUED`, `published_at = NULL`, ошибка очищается; доставка подхватит сама. Не `FAILED` → 409. Запись в `system_events`.

### 10. Удаление старого избранного
Избранное заменено маячками (решение фазы 2); новый frontend его не использует.
- [ ] Удалить пакет `favorite/`, эндпоинты `/api/v1/favorites/**`, тесты.
- [ ] Миграция **V20**: `DROP TABLE favorites`.
- [ ] Старый React-фронтенд фазы 1 перестанет показывать избранное — он полностью заменяется в F1.

### 11. Правила пароля и тексты ошибок валидации
Макеты и `SCREENS.md` обещают «Минимум 8 символов», backend сейчас принимает от 6 и отвечает по-английски — frontend показывает `fieldErrors` прямо под полями.
- [ ] `RegisterRequest`, `ResetPasswordRequest`, новый запрос смены пароля: пароль 8–100 символов, сообщение «Минимум 8 символов». Вход (`/auth/login`) длину не проверяет — пользователи со старыми короткими паролями входят как раньше.
- [ ] Все сообщения валидации в DTO — на русском, как у маячков («должно быть больше 0»); проверить `RegisterRequest`, `ResetPasswordRequest`, `EmailRequest`, `UpdateProfileRequest`.
- [ ] Описание `conditionType` в `CreateBeaconRequest`: «ANY_DROP, BELOW_PRICE или DISCOUNT_PERCENT» (сейчас `ANY_DROP` не упомянут, хотя поддерживается).

### 12. Документация
- [ ] `docs/03-api-contract.md`: снять пометку «план M22» у сделанного, описать форматы ответов.
- [ ] Обновить `docs/api/openapi.json` (тест `OpenApiSpecIT`), Swagger-описания на русском, как у остальных.
- [ ] `docs/02-data-model.md`: новые колонки, удалённая таблица.

## Тесты
- [ ] `SearchCatalogModeIT` — поиск без `q`, списки `binding/language`, 422 для `RELEVANCE` без `q`.
- [ ] `SearchFacetsIT` — счётчик фильтра не зависит от собственного значения фильтра.
- [ ] `PopularQueriesServiceTest` — отбор, дополнение запасным списком, без повторов.
- [ ] `SimilarEditionsIT` — порядок (тот же автор первым), без текущей книги.
- [ ] `BeaconControllerIT` — фильтр `editionId`, новые поля, `statusChangedAt` меняется при паузе.
- [ ] `ProfileSecurityIT` — смена пароля оставляет текущее семейство и отзывает остальные; `logout-all`; удаление аккаунта (каскад, 409 для ADMIN).
- [ ] `WeeklyDigestIT` — получатели, одно письмо в неделю, пустая подборка — без письма.
- [ ] `AdminNotificationsIT` — фильтры, повтор только для FAILED.
- [ ] `PasswordRulesIT` — 7 символов → 422 с русским сообщением у регистрации, сброса и смены пароля; вход со старым 6-символьным паролем работает.

## Критерии приёмки
- [ ] Каждая пометка **[M22]** из `docs/frontend/SCREENS.md` закрыта работающим эндпоинтом или полем.
- [ ] Все новые эндпоинты в Swagger и в `docs/api/openapi.json`.
- [ ] Миграции применяются на чистую БД (`docker compose down -v && up`).
- [ ] `./mvnw -q verify` зелёный, покрытие JaCoCo не ниже порога M21.

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
11. `docs: api contract and openapi for m22`
