<img src="brand/logo-full.svg" alt="KitapRadar" width="240">

# Документация KitapRadar

**Где остановились — [PROGRESS.md](PROGRESS.md)** (читать первым в начале работы).

Порядок чтения: `00` → `01` → `02` → `03` → `04` → `05` → задания вех.

| Документ | О чём |
|---|---|
| [00-product-overview.md](00-product-overview.md) | Продукт, персоны, функции по версиям, принятые решения |
| [01-architecture.md](01-architecture.md) | Компоненты, потоки данных, RabbitMQ, кэш, структура пакетов |
| [02-data-model.md](02-data-model.md) | ER-диаграмма и таблицы |
| [03-api-contract.md](03-api-contract.md) | REST API v1 |
| [04-conventions.md](04-conventions.md) | Код, ошибки, логи, тесты, Git, коммиты |
| [05-parsing-policy.md](05-parsing-policy.md) | Как парсим магазины и сопоставляем издания |
| [performance.md](performance.md) | Замер производительности k6: поиск и карточка на холодном и тёплом кэше, параметры машины |
| [api/openapi.json](api/openapi.json) | Экспортированная спецификация OpenAPI (проверяется тестом `OpenApiSpecIT`) |
| [adr/](adr/) | Архитектурные решения |
| [brand/](brand/README.md) | Логотип, иконки, цвета, правила использования |
| [frontend/ROADMAP.md](frontend/ROADMAP.md) | План frontend-вех, стек, дизайн-токены, приёмы скорости |
| [frontend/SCREENS.md](frontend/SCREENS.md) | Каждый экран: маршрут, элементы, кнопки → эндпоинты, ошибки и состояния |
| [frontend/mockups/](frontend/mockups/README.md) | 26 макетов UI (HTML + PNG) и правила адаптивности |
| [backend/M19-report.md](backend/M19-report.md) | Итоги M19: как работают маячки и письма, гарантии доставки, решения и итоги ревью |
| [backend/M18-report.md](backend/M18-report.md) | Итоги M18: что сделано сверх плана и иначе, чем в задании, принятые решения, результаты ревью |
| [backend/parsing-report-M15-M16.md](backend/parsing-report-M15-M16.md) | Итоги M15–M16: как работает парсинг, какие магазины работают и почему остальные нет, что проверено |

## Статус вех

Фаза 1 (готово): M0–M11 — каркас, домен, поиск, кэш, JWT, избранное, алерты, история цен, React, Docker.

**Порядок строгий:** сначала все backend-вехи M12 → M22, затем frontend F1 → F7, затем v2. В работе одновременно только одна веха; следующая начинается после мерджа предыдущей.

Фаза 2 — backend:

| Веха | Название | Ветка | Статус |
|---|---|---|---|
| [M12](backend/M12-cleanup-infra.md) | Чистка, баги, инфраструктура (RabbitMQ, Mailpit, Testcontainers) | `feature/m12-cleanup-infra` | ✅ |
| [M13](backend/M13-data-model-v2.md) | Модель данных v2: издания, магазины, маячки | `feature/m13-data-model-v2` | ✅ |
| [M14](backend/M14-catalog-search.md) | Каталог и быстрый поиск (pg_trgm, фильтры, кэш) | `feature/m14-catalog-search` | ✅ |
| [M15](backend/M15-parser-framework.md) | Каркас парсеров + первый магазин (Flip.kz) | `feature/m15-parser-framework` | ✅ |
| [M16](backend/M16-store-parsers.md) | Остальные магазины + сопоставление изданий | `feature/m16-store-parsers` | ✅ |
| [M17](backend/M17-rabbitmq.md) | RabbitMQ: очереди парсинга и событий цен | `feature/m17-rabbitmq` | ✅ |
| [M18](backend/M18-auth-v2.md) | Auth v2: подтверждение email, refresh, сброс пароля, лимиты | `feature/m18-auth-v2` | ✅ |
| [M19](backend/M19-beacons-notifications.md) | Маячки и email-уведомления | `feature/m19-beacons-notifications` | ✅ |
| [M20](backend/M20-admin-observability.md) | Admin API и наблюдаемость | `feature/m20-admin-observability` | ✅ |
| [M21](backend/M21-quality-release.md) | Качество, CI, README, релиз backend | `feature/m21-quality-release` | ✅ |
| [M22](backend/M22-api-for-ui.md) | API под макеты frontend: каталог, счётчики фильтров, похожие, профиль, подборка недели, журнал писем | `feature/m22-api-for-ui` | ⬜ |

Фаза 3 — frontend ([план и токены](frontend/ROADMAP.md) · [экраны и кнопки → API](frontend/SCREENS.md) · [макеты](frontend/mockups/README.md)):

| Веха | Название | Ветка | Статус |
|---|---|---|---|
| [F1](frontend/F1-foundation.md) | Фундамент: TypeScript, токены, layout, API-клиент, компоненты, CI | `feature/f1-foundation` | ⬜ |
| [F2](frontend/F2-home-search.md) | Главная, поиск, каталог, скидки | `feature/f2-home-search` | ⬜ |
| [F3](frontend/F3-edition.md) | Карточка издания | `feature/f3-edition` | ⬜ |
| [F4](frontend/F4-auth-profile.md) | Вход, регистрация, пароль, профиль | `feature/f4-auth-profile` | ⬜ |
| [F5](frontend/F5-beacons.md) | Маячки | `feature/f5-beacons` | ⬜ |
| [F6](frontend/F6-admin.md) | Админ-панель | `feature/f6-admin` | ⬜ |
| [F7](frontend/F7-polish-release.md) | Полировка, доступность, скорость, релиз | `feature/f7-polish-release` | ⬜ |
Фаза 4 (по желанию): Kafka для потока цен, Telegram, ИИ-функции.

Обозначения статуса: ⬜ не начато · 🟡 в работе · ✅ готово (смерджено в main).
