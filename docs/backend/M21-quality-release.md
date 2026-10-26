# M21 — Качество, CI, README и релиз backend

**Ветка:** `feature/m21-quality-release` · **Зависит от:** M20 · **Оценка:** 1–2 дня

## Цель
Довести backend до состояния «не стыдно показать на собеседовании»: CI на каждый PR, покрытие тестами, понятный README со схемой архитектуры, безопасный Docker-образ, демо-данные, замер производительности.

## Задачи

### 1. Покрытие и качество
- [x] JaCoCo (`jacoco-maven-plugin`): отчёт в `verify`, порог 70% строк для пакетов `*.service`/сервисных классов (исключить `config`, `dto`, `*Application`).
- [x] Дописать тесты, где покрытие ниже порога.
- [x] Пройтись по коду: неиспользуемые классы и зависимости, TODO, предупреждения компилятора.

### 2. CI — `.github/workflows/backend.yml`
- [x] Триггеры: PR и push в `main`, только при изменениях в `backend/**` или самом workflow.
- [x] Шаги: checkout → `actions/setup-java` (Temurin 21, кэш Maven) → `./mvnw -B verify` (Testcontainers работает на `ubuntu-latest`) → загрузить отчёт JaCoCo как артефакт.
- [x] Отдельная job: `docker build ./backend` (проверка, что образ собирается).
- [x] Бейдж статуса CI в README.

### 3. Docker
- [x] `backend/Dockerfile` — multi-stage (сборка Maven → runtime JRE 21), запуск **не от root**, `HEALTHCHECK` на `/actuator/health/liveness` (не корневой `/actuator/health`: он уходит в 503 при сбое почты или планировщика, решение M20), `JAVA_TOOL_OPTIONS` с разумной памятью (`-XX:MaxRAMPercentage=75`).
- [x] `docker-compose.yml` — полный стек одной командой; healthcheck у всех сервисов; переменные — только из `.env`.
- [x] Профиль `demo`: `app.dev.seed=true` + включённые парсеры для 1–2 магазинов.

### 4. API-документация
- [x] Swagger: описания (`@Operation`, `@Schema`) и примеры для всех публичных эндпоинтов, группы `public` / `user` / `admin`.
- [x] Экспорт `docs/api/openapi.json` (скрипт или плагин `springdoc-openapi-maven-plugin`) — пригодится фронтенду для генерации типов.

### 5. Производительность (замер, не оптимизация вслепую)
- [x] Скрипт k6 `infra/k6/search.js`: 20 виртуальных пользователей, 1 минута, смесь запросов поиска и карточек.
- [x] Записать в `docs/performance.md`: p50/p95 поиска и карточки на тёплом и холодном кэше, параметры машины. Сравнить с целями из `docs/01-architecture.md`.

### 6. README (корень репозитория)
Переписать на русском (английская версия `README.en.md` не делалась, необязательно):
- [x] Что это и зачем (2–3 предложения), скриншоты/GIF (появятся после фронтенда — оставить место).
- [x] Схема архитектуры (mermaid из `docs/01-architecture.md`).
- [x] Стек с иконками/списком.
- [x] Ключевые технические решения со ссылками на ADR: модульный монолит, RabbitMQ, издания, вежливый парсинг.
- [x] Таблица магазинов: какой работает, каким способом, ограничения (честно).
- [x] Запуск за 3 команды, адреса UI (API, Swagger, RabbitMQ, Mailpit), учётка демо-админа.
- [x] Ссылка на `docs/README.md` и статус вех.
- [x] Пометка «учебный проект, некоммерческий».

### 7. Релиз
- [x] Обновить статусы всех вех (M12–M20 ✅, M21 🟡 до мерджа) в `docs/README.md`.
- [ ] Тег `v2.0.0-backend` после мерджа (описание релиза — список вех M12–M21).

## Критерии приёмки
- [ ] CI зелёный на PR (проверить после открытия PR), бейдж в README есть.
- [x] Покрытие сервисного слоя ≥ 70% (отчёт JaCoCo).
- [ ] `git clone` → `cp .env.example .env` → `docker compose up -d` → работающий backend с демо-данными на чистой машине (проверено на существующих томах; прогон с `down -v` не делался, нужно согласие автора).
- [x] `docs/performance.md` с реальными цифрами.
- [x] README понятен человеку, который видит проект впервые.

## План коммитов
1. `build: jacoco coverage with threshold`
2. `test: raise service coverage`
3. `ci: backend workflow with tests and docker build`
4. `build: hardened multi-stage backend dockerfile`
5. `docs: openapi descriptions and exported spec`
6. `perf: k6 load test and performance report`
7. `docs: rewrite readme with architecture and run guide`
8. `docs: update milestone status`
