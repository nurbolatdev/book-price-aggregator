# M21 — Качество, CI, README и релиз backend

**Ветка:** `feature/m21-quality-release` · **Зависит от:** M20 · **Оценка:** 1–2 дня

## Цель
Довести backend до состояния «не стыдно показать на собеседовании»: CI на каждый PR, покрытие тестами, понятный README со схемой архитектуры, безопасный Docker-образ, демо-данные, замер производительности.

## Задачи

### 1. Покрытие и качество
- [ ] JaCoCo (`jacoco-maven-plugin`): отчёт в `verify`, порог 70% строк для пакетов `*.service`/сервисных классов (исключить `config`, `dto`, `*Application`).
- [ ] Дописать тесты, где покрытие ниже порога.
- [ ] Пройтись по коду: неиспользуемые классы и зависимости, TODO, предупреждения компилятора.

### 2. CI — `.github/workflows/backend.yml`
- [ ] Триггеры: PR и push в `main`, только при изменениях в `backend/**` или самом workflow.
- [ ] Шаги: checkout → `actions/setup-java` (Temurin 21, кэш Maven) → `./mvnw -B verify` (Testcontainers работает на `ubuntu-latest`) → загрузить отчёт JaCoCo как артефакт.
- [ ] Отдельная job: `docker build ./backend` (проверка, что образ собирается).
- [ ] Бейдж статуса CI в README.

### 3. Docker
- [ ] `backend/Dockerfile` — multi-stage (сборка Maven → runtime JRE 21), запуск **не от root**, `HEALTHCHECK` на `/actuator/health`, `JAVA_TOOL_OPTIONS` с разумной памятью (`-XX:MaxRAMPercentage=75`).
- [ ] `docker-compose.yml` — полный стек одной командой; healthcheck у всех сервисов; переменные — только из `.env`.
- [ ] Профиль `demo`: `app.dev.seed=true` + включённые парсеры для 1–2 магазинов.

### 4. API-документация
- [ ] Swagger: описания (`@Operation`, `@Schema`) и примеры для всех публичных эндпоинтов, группы `public` / `user` / `admin`.
- [ ] Экспорт `docs/api/openapi.json` (скрипт или плагин `springdoc-openapi-maven-plugin`) — пригодится фронтенду для генерации типов.

### 5. Производительность (замер, не оптимизация вслепую)
- [ ] Скрипт k6 `infra/k6/search.js`: 20 виртуальных пользователей, 1 минута, смесь запросов поиска и карточек.
- [ ] Записать в `docs/performance.md`: p50/p95 поиска и карточки на тёплом и холодном кэше, параметры машины. Сравнить с целями из `docs/01-architecture.md`.

### 6. README (корень репозитория)
Переписать на русском (можно с английской версией `README.en.md`):
- [ ] Что это и зачем (2–3 предложения), скриншоты/GIF (появятся после фронтенда — оставить место).
- [ ] Схема архитектуры (mermaid из `docs/01-architecture.md`).
- [ ] Стек с иконками/списком.
- [ ] Ключевые технические решения со ссылками на ADR: модульный монолит, RabbitMQ, издания, вежливый парсинг.
- [ ] Таблица магазинов: какой работает, каким способом, ограничения (честно).
- [ ] Запуск за 3 команды, адреса UI (API, Swagger, RabbitMQ, Mailpit), учётка демо-админа.
- [ ] Ссылка на `docs/README.md` и статус вех.
- [ ] Пометка «учебный проект, некоммерческий».

### 7. Релиз
- [ ] Обновить статусы всех вех в `docs/README.md`.
- [ ] Тег `v2.0.0-backend` после мерджа (описание релиза — список вех M12–M21).

## Критерии приёмки
- [ ] CI зелёный на PR, бейдж в README.
- [ ] Покрытие сервисного слоя ≥ 70% (отчёт JaCoCo).
- [ ] `git clone` → `cp .env.example .env` → `docker compose up -d` → работающий backend с демо-данными на чистой машине.
- [ ] `docs/performance.md` с реальными цифрами.
- [ ] README понятен человеку, который видит проект впервые.

## План коммитов
1. `build: jacoco coverage with threshold`
2. `test: raise service coverage`
3. `ci: backend workflow with tests and docker build`
4. `build: hardened multi-stage backend dockerfile`
5. `docs: openapi descriptions and exported spec`
6. `perf: k6 load test and performance report`
7. `docs: rewrite readme with architecture and run guide`
8. `docs: update milestone status`
