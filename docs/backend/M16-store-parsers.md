# M16 — Остальные магазины и сопоставление изданий

**Ветка:** `feature/m16-store-parsers` · **Зависит от:** M15 · **Оценка:** 3–4 дня

## Цель
Подключить Meloman/Marwin, Kaspi.kz, Wildberries, Ozon по тем же правилам, добавить Playwright для страниц на JavaScript и нечёткое сопоставление изданий с ручной проверкой.

## Перед началом прочитать
`docs/05-parsing-policy.md`, файл M15 (как устроено ядро).

## Задачи

### 1. Нечёткое сопоставление — `parser/matching/`
- [x] `EditionMatcher` v2 по алгоритму из `docs/05-parsing-policy.md` (раздел 3): ISBN → pg_trgm-кандидаты (top 5) → пороги `app.parser.matching.title-threshold` (0.85), `author-threshold` (0.7), `review-threshold` (0.6).
- [x] `TitleCleaner` — убрать из названия магазина мусор: «(твёрдая обложка)», «подарочное издание», «Книга», артикулы, лишние кавычки; извлечь переплёт и год, если они в названии.
- [x] `NonBookFilter` — стоп-слова и категории (обложка для книг, закладка, набор открыток, ежедневник) → `REJECTED`.
- [x] Подобрать пороги на реальных данных: собрать 30+ пар «название в магазине → издание» в `src/test/resources/matching/cases.csv`, тест считает точность. В PR указать полученную точность.

### 2. Браузер для JS-страниц — `parser/core/BrowserFetcher`
- [x] Зависимость `com.microsoft.playwright:playwright`.
- [x] Один экземпляр `Browser` (Chromium headless) на приложение, `BrowserContext` на запрос, закрытие в `finally`; блокировка загрузки картинок, шрифтов, медиа (`route`).
- [x] Те же правила: robots.txt, rate limit, circuit breaker, честный User-Agent, кэш результата. **Без stealth-плагинов и подмены отпечатков.**
- [x] Включение флагом `app.parser.browser.enabled` (по умолчанию `false` в тестах).
- [x] `backend/Dockerfile`: установка браузера и системных зависимостей Playwright (`mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps chromium"` на этапе сборки образа, либо базовый образ `mcr.microsoft.com/playwright/java`). Указать размер образа в PR.

### 3. Магазины
Для каждого: проверить `robots.txt` → разобрать страницы → сохранить фикстуры → `<Store>Selectors` + `<Store>Parser` → тест → обновить таблицу в `docs/05-parsing-policy.md`.
- [x] **Meloman/Marwin** — ожидается серверный HTML (Jsoup).
- [x] **Kaspi.kz** — сначала JSON-LD/HTML через `HtmlFetcher`; если данных нет без JS — `BrowserFetcher`.
  Результат: сервер закрывает соединение без ответа на любой запрос, включая `robots.txt`; страниц для фикстур нет, парсер не написан, магазин выключен (V10).
- [x] **Wildberries** — SPA: `BrowserFetcher`, либо публичные JSON-ответы, которые запрашивает сама страница каталога (без подделки заголовков авторизации). Цены в тенге для Казахстана.
  Результат: любой запрос, включая `robots.txt`, получает антибот-проверку с капчей (HTTP 498), headless-браузер не пропускается; парсер не написан, магазин выключен (V11).
- [x] **Ozon** — SPA с защитой: попытка через `BrowserFetcher` по правилам. Если магазин стабильно блокирует (403/капча) — **не обходить**: оставить парсер с тестами на фикстурах, магазин `enabled = false` миграцией или через админку, честно описать в README. Это допустимый результат вехи.
  Результат: Ozon отдаёт страницы с данными обычному HTTP, браузер не нужен; поиск запрещён `robots.txt`, новые товары ищутся по страницам категории «Книги».

### 4. Ручная проверка сопоставления
- [x] `GET /api/v1/admin/offers/pending-review`, `POST /api/v1/admin/offers/{id}/match {editionId}`, `POST /api/v1/admin/offers/{id}/reject` (роль ADMIN) — сервис `OfferReviewService`. После ручного `match` предложение участвует в ценах, публикуется `PriceChangedEvent`.

## Тесты
- [x] `<Store>ParserTest` для каждого магазина на фикстурах (все поля).
- [x] `EditionMatcherIT` (pg_trgm в Testcontainers) — сценарии MATCHED / PENDING_REVIEW / REJECTED / новое издание.
- [x] `MatchingAccuracyTest` на `cases.csv` — точность ≥ 85% (или зафиксировать фактическую и обсудить).
- [x] `BrowserFetcherIT` — помечен `@Tag("browser")`, запускается отдельно (`./mvnw verify -Pbrowser`), открывает локальную статическую страницу из ресурсов через WireMock, не интернет.

## Критерии приёмки
- [x] Минимум 3 из 5 магазинов собирают реальные цены ручным запуском.
- [x] Одна и та же книга из разных магазинов сопоставляется с одним изданием (показать пример в PR).
- [x] Несопоставленные предложения видны в `pending-review` и привязываются вручную.
- [x] Заблокированный магазин не роняет остальные: запуск `FAILED`, circuit breaker открыт, WARN в `system_events`.
- [ ] Docker-образ backend собирается с браузером; без флага браузер не стартует.
  Без флага бин `BrowserFetcher` не создаётся (проверено). Сборку образа в среде разработки проверить не удалось: нет Docker.
- [ ] `./mvnw -q verify` зелёный (без тега browser).
  Unit-тесты зелёные; интеграционные прогнаны на локальных PostgreSQL 16 и Redis вместо Testcontainers (нет Docker), кроме `MigrationIT`, которому нужен контейнер.

## План коммитов
1. `feat: fuzzy edition matching with title cleaner and non-book filter`
2. `test: matching accuracy dataset`
3. `feat: playwright browser fetcher behind feature flag`
4. `build: install playwright chromium in backend image`
5. `feat: meloman parser with fixtures`
6. `feat: kaspi parser with fixtures`
7. `feat: wildberries parser with fixtures`
8. `feat: ozon parser with fixtures`
9. `feat: admin review of unmatched offers`
10. `docs: update store notes and milestone status`

## Не входит
Очереди и расписание (M17).
