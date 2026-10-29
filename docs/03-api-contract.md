# 03. Контракт REST API v1

- Базовый путь: **`/api/v1`** (в M12 все существующие `/api/...` переносятся на `/api/v1/...`).
- Формат: JSON, UTF-8, даты ISO-8601 с часовым поясом (`2026-10-20T09:12:00Z`), цены — числа в тенге (`3150.00`).
- Авторизация: `Authorization: Bearer <accessToken>` (JWT, 15 мин). Обновление — через refresh-токен.
- Источник правды — Swagger (`/swagger-ui.html`), этот документ — договорённость, по которой Swagger строится.

## Общие форматы

**Страница (пагинация)**
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 134, "totalPages": 7 }
```

**Ошибка** (все ошибки, включая 401/403/404/422/429/500)
```json
{
  "timestamp": "2026-10-20T09:12:00Z",
  "status": 422,
  "code": "VALIDATION_FAILED",
  "message": "Запрос содержит ошибки",
  "path": "/api/v1/beacons",
  "fieldErrors": [ { "field": "targetPrice", "message": "должно быть больше 0" } ]
}
```
Коды: `VALIDATION_FAILED`, `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`, `EMAIL_NOT_VERIFIED`, `EMAIL_TAKEN`, `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `RATE_LIMITED`, `CONFLICT`, `SERVICE_UNAVAILABLE` (503: очередь задач недоступна), `INTERNAL_ERROR`.

## 1. Публичные эндпоинты

| Метод | Путь | Описание | Веха |
|---|---|---|---|
| GET | `/search` | Поиск изданий с фильтрами | M14 |
| GET | `/search/suggest?q=` | Подсказки (до 8), для автодополнения | M14 |
| GET | `/search/facets` | Счётчики для фильтров (магазины, переплёт, язык) | M22 |
| GET | `/search/popular?limit=6` | Популярные запросы для главной и пустого поиска | M22 |
| GET | `/books/{bookId}` | Книга и список её изданий с лучшей ценой | M14 |
| GET | `/editions/{id}` | Карточка издания: предложения, лучшая цена, оценка цены | M14 |
| GET | `/editions/{id}/similar?limit=4` | Похожие издания: сначала тот же автор | M22 |
| GET | `/editions/{id}/price-history?period=1M\|3M\|6M` | История минимальной цены + min/avg/max | M14 |
| GET | `/home/drops?limit=8` | «Сильнее всего подешевели за неделю» | M14 |
| GET | `/stores` | Список магазинов (код, название, включён) | M13 |

### GET /search — параметры
| Параметр | Тип | Пример | По умолчанию |
|---|---|---|---|
| `q` | string, 2–200 символов | `мастер маргарита` или ISBN | необязателен: без него (или пустой) — режим каталога |
| `stores` | список кодов | `FLIP,KASPI` | все |
| `minPrice`, `maxPrice` | число | `1000` | — |
| `inStock` | boolean | `true` | `true` |
| `binding` | список `HARDCOVER\|PAPERBACK` через запятую | `HARDCOVER,PAPERBACK` | все |
| `language` | список `ru\|kk\|en` через запятую | `ru,kk` | все |
| `sort` | `PRICE_ASC\|DISCOUNT_DESC\|RELEVANCE\|STORES_DESC` | | `RELEVANCE` с `q`, `STORES_DESC` без `q` |
| `page`, `size` | int | `0`, `20` (макс. 50) | `0`, `20` |

Режим каталога (нет `q`): в `search_log` не пишется, книги не подгружаются из внешних источников; `sort=RELEVANCE` без `q` → 422 с `fieldErrors[0].field = "sort"`. Недопустимое значение `language` или `binding` → 422.

Ответ (элемент `content`):
```json
{
  "editionId": 41,
  "bookId": 12,
  "title": "Мастер и Маргарита",
  "author": "Михаил Булгаков",
  "publisher": "АСТ",
  "year": 2023,
  "binding": "HARDCOVER",
  "coverUrl": "https://...",
  "bestPrice": { "store": "FLIP", "price": 3150.00, "oldPrice": 3850.00, "inStock": true },
  "offers": [
    { "store": "FLIP", "price": 3150.00 },
    { "store": "WILDBERRIES", "price": 3380.00 },
    { "store": "KASPI", "price": 3610.00 }
  ],
  "storesCount": 5,
  "updatedAt": "2026-10-20T07:00:00Z"
}
```

### GET /search/facets
Параметры те же, что у `/search`, кроме `sort`, `page`, `size`. Каждый счётчик — число **изданий**, которое вернул бы `/search` с теми же фильтрами и значением этого счётчика вместо собственного фильтра (поэтому выбранный магазин не обнуляет остальные). В списках всегда все включённые магазины, `HARDCOVER`, `PAPERBACK`, `ru`, `kk`, `en` — с нулями. Пустой (из одной пунктуации) `q` → все нули. Кэш 60 с; лимит `facets` — как у поиска, отдельное ведро.
```json
{
  "stores": [ { "code": "FLIP", "count": 34 } ],
  "bindings": [ { "value": "HARDCOVER", "count": 12 } ],
  "languages": [ { "value": "ru", "count": 40 } ]
}
```

### GET /search/popular
`limit` 1–12 (по умолчанию 6), иначе 422. Публично, кэш 1 ч. Ответ — массив строк: `["Мастер и Маргарита", "Атомные привычки"]`. Топ запросов за 7 дней с непустой выдачей (запросы из одних цифр, то есть ISBN, исключены), дополняется списком `app.home.popular-fallback`; дубликаты по нормализованному тексту убираются, коротких ответов избегает только список из настроек.

### GET /home/drops
Элемент: `{editionId, title, author, coverUrl, storeName, price, previousPrice, dropPercent, storesCount, ...}`; `storesCount` — число включённых магазинов с подходящим предложением в наличии.

### GET /editions/{id}/similar
`limit` 1–12 (по умолчанию 4). Неизвестное издание → 404. Сначала другие книги того же автора (по числу магазинов в наличии), затем книги на том же языке (по числу предложений); `editionId` — самое дешёвое издание книги в наличии. Пустой массив, если ничего не нашлось. Кэш 1 ч.
```json
[ { "bookId": 15, "editionId": 52, "title": "Белая гвардия", "author": "Михаил Булгаков", "coverUrl": "https://...", "bestPrice": 2100.00 } ]
```

### GET /editions/{id} — ответ
```json
{
  "editionId": 41,
  "book": { "id": 12, "title": "Мастер и Маргарита", "author": "Михаил Булгаков", "description": "..." },
  "isbn13": "9785170906304",
  "publisher": "АСТ", "year": 2023, "binding": "HARDCOVER", "language": "ru", "pages": 480,
  "coverUrl": "https://...",
  "otherEditions": [ { "editionId": 42, "label": "Азбука 2022 · мягкий", "bestPrice": 1890.00 } ],
  "bestPrice": { "store": "FLIP", "price": 3150.00, "deliveryText": "1–2 дня", "url": "https://flip.kz/..." },
  "priceVerdict": { "avg90d": 3580.00, "diffPercent": -12, "label": "BELOW_AVERAGE" },
  "offers": [
    {
      "offerId": 501, "store": "FLIP", "storeName": "Flip.kz",
      "price": 3150.00, "oldPrice": 3850.00, "inStock": true,
      "deliveryText": "1–2 дня", "url": "https://flip.kz/...",
      "lastSeenAt": "2026-10-20T07:00:00Z"
    }
  ]
}
```
`priceVerdict.label`: `BELOW_AVERAGE` (≤ −5%), `AVERAGE`, `ABOVE_AVERAGE` (≥ +5%), `NOT_ENOUGH_DATA` (истории меньше 14 дней).

## 2. Аутентификация (M12 перенос, M18 расширение)
| Метод | Путь | Тело | Ответ |
|---|---|---|---|
| POST | `/auth/register` | `{name, email, password}` | 201 `{userId, emailVerificationRequired: true}` + письмо |
| POST | `/auth/login` | `{email, password}` | 200 `{accessToken, refreshToken, expiresIn, user}` + cookie `kr_refresh` |
| POST | `/auth/refresh` | cookie `kr_refresh` (или `{refreshToken}`, переходный режим) | 200 новая пара (старый refresh отзывается) + новая cookie; повтор отозванного вне окна 10 с → 401 и завершение этой сессии |
| POST | `/auth/logout` | cookie (или `{refreshToken}`) | 204, завершает всю сессию (семейство токенов) и очищает cookie; без токена тоже 204 |
| POST | `/auth/logout-all` | — (нужен access-токен) | 204, отзывает все сессии, очищает cookie; без токена → 401 |
| POST | `/auth/verify-email` | `{token}` | 204 |
| POST | `/auth/resend-verification` | `{email}` | 204 (всегда, даже если email нет — не раскрываем) |
| POST | `/auth/forgot-password` | `{email}` | 204 (всегда) |
| POST | `/auth/reset-password` | `{token, newPassword}` | 204, все refresh-токены отзываются, cookie очищается |

Лимиты (Bucket4j): `login` — 5 в минуту на IP+email, `register`/`forgot-password` — 3 в минуту на IP, `GET /search` — 60 в минуту на IP, `GET /search/facets` — 60 в минуту на IP (отдельное ведро), `account` (`POST /me/password`, `DELETE /me`) — 5 в минуту на пользователя, остальное API — 120 в минуту на пользователя (без входа — на IP). Превышение → 429 `RATE_LIMITED` и заголовок `Retry-After` (секунды).

**Refresh-токен в cookie `kr_refresh`:** `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=2592000` (`Secure` управляется `REFRESH_COOKIE_SECURE`, в профиле `local` выключен). `refresh` и `logout` берут токен из cookie, иначе из тела. Проверка `Origin`: заголовок есть и не входит в `app.cors.allowed-origins` → 403 `FORBIDDEN`; заголовка нет, а токен из cookie → 403; заголовка нет и токен из тела → разрешено (curl, тесты, переходный режим). CORS отдаёт `Access-Control-Allow-Credentials: true`. Поле `refreshToken` в теле запроса и ответе помечено `deprecated` и исчезнет в F4. Без токена `refresh` → 401 `UNAUTHORIZED`.

**Окно повтора:** повтор только что ротированного токена в течение 10 с (`JWT_REFRESH_REUSE_GRACE`) не завершает сессию — две вкладки получают один и тот же новый токен; вне окна, после выхода или смены пароля — прежнее поведение (семейство отзывается, 401). Подробно — [ADR-005](adr/ADR-005-refresh-cookie-and-reuse-grace.md).

Сессий (устройств) на пользователя — не больше 10: вход сверх лимита завершает ту, что дольше всех не обновлялась.
`user` в ответе входа и `GET /me`: `{id, email, name, role, emailVerified, notifyFrequency, weeklyDigest}`; `expiresIn` — секунды жизни access-токена (900).
Ошибки токенов: истёкший access-токен на любом защищённом пути → 401 `TOKEN_EXPIRED` (обновить пару через `/auth/refresh`); неизвестный или отозванный refresh → 401 `UNAUTHORIZED`, истёкший → 401 `TOKEN_EXPIRED`; ссылка из письма неизвестная, использованная или другого назначения → 400 `TOKEN_INVALID`, истёкшая → 401 `TOKEN_EXPIRED`.

## 3. Пользователь (роль USER)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/me` | профиль: имя, email, emailVerified, notifyFrequency, weeklyDigest |
| PATCH | `/me` | `{name?, notifyFrequency?, weeklyDigest?}` |
| POST | `/me/password` | `{currentPassword, newPassword, refreshToken?}` → 204. Неверный текущий → 422 `fieldErrors[currentPassword]` «Неверный пароль». Текущая сессия (по claim `sid` access-токена) остаётся, остальные отзываются; cookie не очищается |
| DELETE | `/me` | `{password}` → 204, каскадное удаление данных, cookie очищается. Неверный пароль → 422 `fieldErrors[password]`; ADMIN → 409 `CONFLICT` |

Пароль при регистрации, сбросе и смене — 8–100 символов («Минимум 8 символов», «Не больше 100 символов»); при входе длина не проверяется (старые пароли работают). Сообщения валидации — на русском.

Раз в неделю (понедельник 10:00 Asia/Almaty) пользователям с подтверждённым email и `weeklyDigest = true` уходит письмо «Подборка недели» (до 8 самых подешевевших книг, не больше одного на ISO-неделю).

## 4. Маячки (роль USER, M19)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/beacons?status=ACTIVE\|TRIGGERED\|PAUSED&editionId=41` | мои маячки с текущей лучшей ценой и разницей до цели; `editionId` — фильтр по изданию (0 или 1 элемент) |
| POST | `/beacons` | создать |
| PATCH | `/beacons/{id}` | изменить условие или статус (`ACTIVE`/`PAUSED`) |
| DELETE | `/beacons/{id}` | удалить |
| POST | `/beacons/unsubscribe` | `{token}` из письма, **без авторизации** — ставит маячок на паузу; 200 `{beaconId, editionId, title, status}`, неверный токен → 400 `TOKEN_INVALID`, истёкший (90 дней) → 401 `TOKEN_EXPIRED` |

Создание:
```json
{ "editionId": 41, "conditionType": "BELOW_PRICE", "targetPrice": 3200 }
```
Ответ элемента списка (`statusChangedAt` — когда маячок последний раз менял статус: пауза, возобновление, срабатывание):
```json
{
  "id": 7, "editionId": 41, "title": "Мастер и Маргарита", "author": "Михаил Булгаков", "coverUrl": "...",
  "conditionType": "BELOW_PRICE", "targetPrice": 3200.00, "targetPercent": null,
  "status": "TRIGGERED", "currentBestPrice": 3150.00, "currentBestStore": "FLIP",
  "baselinePrice": 3610.00, "lastTriggeredAt": "2026-10-20T09:12:00Z",
  "currentBestUrl": "https://flip.kz/...", "createdAt": "2026-10-18T10:00:00Z", "statusChangedAt": "2026-10-20T09:12:00Z"
}
```
Правила: один маячок на издание у пользователя (повтор → 409 `CONFLICT`); чужой маячок → 404; условие: `BELOW_PRICE` требует `targetPrice > 0`, `DISCOUNT_PERCENT` — `targetPercent` 1–90, `ANY_DROP` — без параметров (иначе 422 с `fieldErrors`). `PATCH`: новое условие — целиком (тип + параметр), `status` — только `ACTIVE`/`PAUSED`; возврат в `ACTIVE` и смена условия берут текущую лучшую цену как новый `baselinePrice`. Сработавший маячок продолжает следить: о каждом новом минимуме — новое письмо, о той же или более высокой цене — нет. Список: сработавшие сверху, затем новые; без подтверждённого email маячок создаётся, но письма не уходят (поле `emailVerified` в `/me` подсказывает фронту показать предупреждение).

## 5. Админка (роль ADMIN, M20)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/admin/stats?period=24h\|7d\|30d` | KPI: пользователи (всего, новые), активные, маячки, письма (sent/failed), поиски, cache hit %, книги/издания/предложения |
| GET | `/admin/health` | статусы компонентов под именами Spring Boot Actuator (`db`, `redis`, `rabbit`, `mail`, `diskSpace`, `ping`, `scheduler`; набор зависит от окружения) и метрики: `httpP95Millis` — максимум p95 по эндпоинтам приложения (без `/actuator/**`), пул БД, память Redis |
| GET | `/admin/parsers` | по магазину: enabled, статус последнего запуска, % успеха, предложений, ср. время, circuit breaker |
| POST | `/admin/parsers/{storeCode}/run` | ручной запуск (кладёт задачу в очередь), 202 `{ "queued": true }`; тело `{editionIds}` необязательно; 404 — у магазина нет парсера, 409 — магазин выключен, 503 — очередь недоступна |
| PATCH | `/admin/stores/{storeCode}` | `{enabled}` |
| GET | `/admin/queues` | по очереди: ready, unacked, consumers, скорость |
| POST | `/admin/queues/dead-letter/requeue` | вернуть сообщения из DLQ в работу |
| GET | `/admin/activity?days=14` | по дням: поиски, регистрации, новые маячки, письма |
| GET | `/admin/events?level=&page=` | журнал `system_events` |
| GET | `/admin/search/top?period=24h` | топ запросов |
| GET | `/admin/search/zero-results?period=7d` | запросы без результатов |
| GET | `/admin/users?q=&verified=&page=` | пользователи (email, дата, verified, число маячков); `q` (до 100 символов) — поиск по email и имени без учёта регистра, `%` и `_` ищутся буквально; `verified` — фильтр подтверждения |
| GET | `/admin/notifications?status=&delivery=&page=` | журнал писем, новые сверху: `{id, createdAt, sentAt, userEmail, title, delivery, status, error}`; `delivery` — `INSTANT`, `DIGEST` или `WEEKLY` |
| POST | `/admin/notifications/{id}/retry` | только `FAILED` → снова `QUEUED` (доставку подхватит сама), 200 с обновлённой записью; не `FAILED` → 409; email пользователя не подтверждён → 409; нет записи → 404 |
| GET | `/admin/offers/pending-review?page=` | несопоставленные предложения |
| POST | `/admin/offers/{offerId}/match` | `{editionId}` — привязать вручную |
| POST | `/admin/offers/{offerId}/reject` | отклонить (не книга / мусор) |

## 6. Служебные
| Путь | Доступ |
|---|---|
| `/actuator/health` | публично (только UP/DOWN; подробности — ADMIN) |
| `/actuator/health/**`, `/actuator/metrics`, `/actuator/prometheus` | ADMIN |
| `/swagger-ui.html`, `/v3/api-docs` | публично в dev, отключаемо в prod |

## 7. Устаревшие (удалены)
| Путь | Судьба |
|---|---|
| `/api/alerts/**` | удаляется в M13 (заменён маячками) |
| `/api/v1/favorites/**` | удалён в M22 (таблица `favorites` — миграцией V20); заменён маячками |
| `/api/books/search` | заменяется `/api/v1/search` в M14 |
