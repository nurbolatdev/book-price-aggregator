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
| GET | `/books/{bookId}` | Книга и список её изданий с лучшей ценой | M14 |
| GET | `/editions/{id}` | Карточка издания: предложения, лучшая цена, оценка цены | M14 |
| GET | `/editions/{id}/price-history?period=1M\|3M\|6M` | История минимальной цены + min/avg/max | M14 |
| GET | `/home/drops?limit=8` | «Сильнее всего подешевели за неделю» | M14 |
| GET | `/stores` | Список магазинов (код, название, включён) | M13 |

### GET /search — параметры
| Параметр | Тип | Пример | По умолчанию |
|---|---|---|---|
| `q` | string, 2–200 символов | `мастер маргарита` или ISBN | обязателен |
| `stores` | список кодов | `FLIP,KASPI` | все |
| `minPrice`, `maxPrice` | число | `1000` | — |
| `inStock` | boolean | `true` | `true` |
| `binding` | `HARDCOVER\|PAPERBACK` | | все |
| `language` | `ru\|kk\|en` | | все |
| `sort` | `PRICE_ASC\|DISCOUNT_DESC\|RELEVANCE\|STORES_DESC` | | `RELEVANCE` |
| `page`, `size` | int | `0`, `20` (макс. 50) | `0`, `20` |

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
| POST | `/auth/login` | `{email, password}` | 200 `{accessToken, refreshToken, expiresIn, user}` |
| POST | `/auth/refresh` | `{refreshToken}` | 200 новая пара (старый refresh отзывается); повтор отозванного → 401 и завершение этой сессии |
| POST | `/auth/logout` | `{refreshToken}` | 204 |
| POST | `/auth/verify-email` | `{token}` | 204 |
| POST | `/auth/resend-verification` | `{email}` | 204 (всегда, даже если email нет — не раскрываем) |
| POST | `/auth/forgot-password` | `{email}` | 204 (всегда) |
| POST | `/auth/reset-password` | `{token, newPassword}` | 204, все refresh-токены отзываются |

Лимиты (Bucket4j): `login` — 5 в минуту на IP+email, `register`/`forgot-password` — 3 в минуту на IP, `GET /search` — 60 в минуту на IP, остальное API — 120 в минуту на пользователя (без входа — на IP). Превышение → 429 `RATE_LIMITED` и заголовок `Retry-After` (секунды).

Сессий (устройств) на пользователя — не больше 10: вход сверх лимита завершает ту, что дольше всех не обновлялась.
`user` в ответе входа и `GET /me`: `{id, email, name, role, emailVerified, notifyFrequency, weeklyDigest}`; `expiresIn` — секунды жизни access-токена (900).
Ошибки токенов: истёкший access-токен на любом защищённом пути → 401 `TOKEN_EXPIRED` (обновить пару через `/auth/refresh`); неизвестный или отозванный refresh → 401 `UNAUTHORIZED`, истёкший → 401 `TOKEN_EXPIRED`; ссылка из письма неизвестная, использованная или другого назначения → 400 `TOKEN_INVALID`, истёкшая → 401 `TOKEN_EXPIRED`.

## 3. Пользователь (роль USER)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/me` | профиль: имя, email, emailVerified, notifyFrequency, weeklyDigest |
| PATCH | `/me` | `{name?, notifyFrequency?, weeklyDigest?}` |

## 4. Маячки (роль USER, M19)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/beacons?status=ACTIVE\|TRIGGERED\|PAUSED` | мои маячки с текущей лучшей ценой и разницей до цели |
| POST | `/beacons` | создать |
| PATCH | `/beacons/{id}` | изменить условие или статус (`ACTIVE`/`PAUSED`) |
| DELETE | `/beacons/{id}` | удалить |
| POST | `/beacons/unsubscribe` | `{token}` из письма, **без авторизации** — ставит маячок на паузу; 200 `{beaconId, editionId, title, status}`, неверный токен → 400 `TOKEN_INVALID`, истёкший (90 дней) → 401 `TOKEN_EXPIRED` |

Создание:
```json
{ "editionId": 41, "conditionType": "BELOW_PRICE", "targetPrice": 3200 }
```
Ответ элемента списка:
```json
{
  "id": 7, "editionId": 41, "title": "Мастер и Маргарита", "author": "Михаил Булгаков", "coverUrl": "...",
  "conditionType": "BELOW_PRICE", "targetPrice": 3200.00, "targetPercent": null,
  "status": "TRIGGERED", "currentBestPrice": 3150.00, "currentBestStore": "FLIP",
  "baselinePrice": 3610.00, "lastTriggeredAt": "2026-10-20T09:12:00Z"
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
| GET | `/admin/users?page=` | пользователи (email, дата, verified, число маячков) |
| GET | `/admin/offers/pending-review?page=` | несопоставленные предложения |
| POST | `/admin/offers/{offerId}/match` | `{editionId}` — привязать вручную |
| POST | `/admin/offers/{offerId}/reject` | отклонить (не книга / мусор) |

## 6. Служебные
| Путь | Доступ |
|---|---|
| `/actuator/health` | публично (только UP/DOWN; подробности — ADMIN) |
| `/actuator/health/**`, `/actuator/metrics`, `/actuator/prometheus` | ADMIN |
| `/swagger-ui.html`, `/v3/api-docs` | публично в dev, отключаемо в prod |

## 7. Устаревшие (удаляются)
| Путь | Судьба |
|---|---|
| `/api/alerts/**` | удаляется в M13 (заменён маячками) |
| `/api/v1/favorites/**` | перенесён в M12, **удаляется в M22** (таблица `favorites` — миграцией V20); новый frontend избранное не использует |
| `/api/books/search` | заменяется `/api/v1/search` в M14 |

## 8. План M22 — API под макеты frontend
Источник требований — `docs/frontend/SCREENS.md` (пометки **[M22]**), задание — `docs/backend/M22-api-for-ui.md`. После M22 пометка «план» снимается, описания переносятся в разделы выше.

| Метод | Путь | Что меняется / ответ |
|---|---|---|
| GET | `/search` | `q` необязателен: без `q` — режим каталога (сортировка по умолчанию `STORES_DESC`, `RELEVANCE` без `q` → 422, в `search_log` не пишется); `binding` и `language` принимают списки через запятую |
| GET | `/search/facets` | те же фильтры, что у `/search`; `{stores:[{code,count}], bindings:[{value,count}], languages:[{value,count}]}`; каждый счётчик — без учёта собственного фильтра; число изданий |
| GET | `/search/popular?limit=6` | `["Мастер и Маргарита", …]` — топ запросов за 7 дней с результатами, дополняется списком из настроек; публично, кэш 1 ч |
| GET | `/home/drops` | в элементе добавляется `storesCount` |
| GET | `/editions/{id}/similar?limit=4` | `[{bookId, editionId, title, author, coverUrl, bestPrice}]`; сначала тот же автор |
| GET | `/beacons?editionId=41` | фильтр по изданию (0 или 1 элемент); в элементе добавляются `currentBestUrl`, `createdAt`, `statusChangedAt` |
| POST | `/me/password` | `{currentPassword, newPassword, refreshToken}` → 204; неверный текущий → 422 `fieldErrors.currentPassword`; отзываются все сессии, кроме текущей |
| POST | `/auth/logout-all` | авторизованный → 204, отзывает все сессии |
| DELETE | `/me` | `{password}` → 204, каскадное удаление; ADMIN → 409 |
| GET | `/admin/users?q=&verified=&page=` | поиск по email/имени, фильтр подтверждения |
| GET | `/admin/notifications?status=&delivery=&page=` | страница `{id, createdAt, sentAt, userEmail, title, delivery, status, error}` |
| POST | `/admin/notifications/{id}/retry` | только `FAILED` → снова `QUEUED`; иначе 409 |

Плюс: еженедельное письмо «Подборка недели» (по флагу `weeklyDigest`, макет `docs/frontend/mockups/24-email-weekly-digest.html`); пароль при регистрации, сбросе и смене — 8–100 символов, сообщения валидации на русском.
