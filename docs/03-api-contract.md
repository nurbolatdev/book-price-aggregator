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
Коды: `VALIDATION_FAILED`, `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`, `EMAIL_NOT_VERIFIED`, `EMAIL_TAKEN`, `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `RATE_LIMITED`, `CONFLICT`, `INTERNAL_ERROR`.

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
| POST | `/auth/refresh` | `{refreshToken}` | 200 новая пара (старый refresh отзывается) |
| POST | `/auth/logout` | `{refreshToken}` | 204 |
| POST | `/auth/verify-email` | `{token}` | 204 |
| POST | `/auth/resend-verification` | `{email}` | 204 (всегда, даже если email нет — не раскрываем) |
| POST | `/auth/forgot-password` | `{email}` | 204 (всегда) |
| POST | `/auth/reset-password` | `{token, newPassword}` | 204, все refresh-токены отзываются |

Лимиты (Bucket4j): `login` — 5 в минуту на IP+email, `register`/`forgot-password` — 3 в минуту на IP.

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
| POST | `/beacons/unsubscribe` | `{token}` из письма, **без авторизации** — ставит маячок на паузу |

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
Правила: один маячок на издание у пользователя (повтор → 409 `CONFLICT`); без подтверждённого email маячок создаётся, но письма не уходят (поле `emailVerified` в `/me` подсказывает фронту показать предупреждение).

## 5. Админка (роль ADMIN, M20)
| Метод | Путь | Описание |
|---|---|---|
| GET | `/admin/stats?period=24h\|7d\|30d` | KPI: пользователи (всего, новые), активные, маячки, письма (sent/failed), поиски, cache hit %, книги/издания/предложения |
| GET | `/admin/health` | статусы: db, redis, rabbitmq, mail, scheduler (+ метрики) |
| GET | `/admin/parsers` | по магазину: enabled, статус последнего запуска, % успеха, предложений, ср. время, circuit breaker |
| POST | `/admin/parsers/{storeCode}/run` | ручной запуск (кладёт задачу в очередь), 202 |
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
| `/actuator/health` | публично (только UP/DOWN) |
| `/actuator/health/**`, `/actuator/metrics`, `/actuator/prometheus` | ADMIN |
| `/swagger-ui.html`, `/v3/api-docs` | публично в dev, отключаемо в prod |

## 7. Устаревшие (удаляются)
| Путь | Судьба |
|---|---|
| `/api/alerts/**` | удаляется в M13 (заменён маячками) |
| `/api/favorites/**` | переносится на `/api/v1/favorites` в M12, удаляется в F3 вместе с переходом фронта |
| `/api/books/search` | заменяется `/api/v1/search` в M14 |
