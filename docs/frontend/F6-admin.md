# F6 — Админ-панель

**Ветка:** `feature/f6-admin` · **Зависит от:** F5 · **Оценка:** 3 дня

## Цель
Панель администратора в отдельном чанке: обзор с KPI и здоровьем системы, парсеры и магазины, очереди RabbitMQ, пользователи, журнал писем, ручное сопоставление предложений, журнал событий. Макеты 09, 18–23. Видна только роли ADMIN.

## Перед началом прочитать
`docs/frontend/SCREENS.md` — раздел «09, 18–23 · Админ-панель»; макеты `09-admin-overview`, `18-admin-parsers`, `19-admin-queues`, `20-admin-users`, `21-admin-notifications`, `22-admin-matching`, `23-admin-events`; `docs/03-api-contract.md` — раздел 5; `docs/backend/M20-admin-observability.md` (что значат цифры).

## Задачи

### 1. Каркас — `pages/admin/`
- [ ] Все страницы админки — один lazy-чанк `admin` (`import()` в роутере), Recharts и TanStack Virtual — только внутри него.
- [ ] `AdminLayout` (макет 09): тёмное меню 240 px (Обзор, Парсеры, Очереди RabbitMQ, Пользователи, Маячки и письма, Каталог и сопоставление, Журнал событий, «← На сайт»), активный пункт `#1F2E44`, контент с полями 28 px, заголовок страницы + подпись «Обновляется каждые 30 секунд, пока вкладка открыта» / «последнее обновление HH:MM».
- [ ] Опрос: `refetchInterval: 30000` и `refetchIntervalInBackground: false` для KPI, здоровья, парсеров, очередей; журналы — по кнопке и при смене фильтра.
- [ ] `RequireAdmin`: не ADMIN → 404. Ширина < 1024 px → плашка «Админ-панель рассчитана на экран от 1024 px».
- [ ] Статусы — всегда цвет + текст (`Badge`): OK зелёный, частичные ошибки оранжевый, не отвечает красный, выключен серый.

### 2. Обзор `/admin` — макет 09
- [ ] Переключатель 24 ч / 7 дней / 30 дней → `GET /admin/stats?period=`; KPI-карточки по макету; подписи, которых нет в API, заменить на реальные поля (`SCREENS.md`).
- [ ] «Состояние сервисов» — `GET /admin/health` (`components`: db → PostgreSQL, redis → Redis, rabbit → RabbitMQ, mail → SMTP, scheduler → Планировщик; `metrics.httpP95Millis` — под «Backend API»).
- [ ] Короткая таблица парсеров с «Перезапустить» и «Запустить все», активность (`GET /admin/activity`, график — Recharts), «Топ запросов» (`GET /admin/search/top?period=`).

### 3. Парсеры `/admin/parsers` — макет 18
- [ ] Таблица `GET /admin/parsers`: магазин, переключатель «Вкл.», статус, запуск, успех 24 ч, предложений, среднее время, состояние breaker, «Запустить».
- [ ] Выключение — окно подтверждения «Выключить {магазин}?» (текст из макета) → `PATCH /admin/stores/{code} {enabled:false}`; включение — без подтверждения.
- [ ] «Запустить» → `POST /admin/parsers/{code}/run` → toast «Задача в очереди»; 409 (выключен) и 503 (очередь недоступна) — сообщение в строке. Выключенный или без парсера — кнопка неактивна с подсказкой.
- [ ] «Запустить все включённые» — по очереди для `enabled && hasParser`, итог одним toast «Поставлено в очередь: N из M».

### 4. Очереди `/admin/queues` — макет 19
- [ ] Таблица `GET /admin/queues` (`ready`, `unacknowledged`, `consumers`, `ackRate`); блок dead letter с числом сообщений и «Вернуть в работу» → подтверждение → `POST /admin/queues/dead-letter/requeue` → toast с числом возвращённых.

### 5. Пользователи `/admin/users` — макет 20
- [ ] `GET /admin/users?q=&verified=&page=`: поиск по email/имени с debounce 300 мс, чипы «Все / Подтверждённые / Не подтверждённые», таблица, пагинация «Назад/Вперёд»; фильтры в URL.

### 6. Маячки и письма `/admin/notifications` — макет 21
- [ ] KPI из `GET /admin/stats`; журнал `GET /admin/notifications?status=&delivery=&page=`, чипы «Все / В очереди / Отправлено / Ошибка», текст ошибки у FAILED, «Повторить» → `POST /admin/notifications/{id}/retry` → строка меняет статус на «В очереди».

### 7. Каталог и сопоставление `/admin/matching` — макет 22
- [ ] Список `GET /admin/offers/pending-review?page=`; выбор предложения → правая панель с данными магазина.
- [ ] «Найти издание» → `GET /search?q=` (название предложения по умолчанию, до 5 вариантов) → выбор → «Привязать к изданию» → `POST /admin/offers/{id}/match {editionId}`; «Отклонить: не книга / мусор» → `POST /admin/offers/{id}/reject`. После действия — следующее предложение выбирается автоматически.

### 8. Журнал событий `/admin/events` — макет 23
- [ ] `GET /admin/events?level=&page=`, чипы уровня (Все/INFO/WARN/ERROR), строки раскрываются (`details` моноширинным шрифтом), длинный список — TanStack Virtual + подгрузка страниц при прокрутке.
- [ ] Справа «Запросы без результатов» — `GET /admin/search/zero-results?period=24h|7d`.

### 9. Снимки
- [ ] Все 7 страниц и окна подтверждения при 1280 px — в `e2e/screens.config.ts`.

## Тесты
- [ ] `AdminRoutes.test.tsx` — USER получает 404, админ-чанк не загружается для USER.
- [ ] `ParsersPage.test.tsx` — подтверждение выключения, 409/503 в строке, «Запустить все» считает итог.
- [ ] `NotificationsPage.test.tsx` — «Повторить» только у FAILED.
- [ ] `MatchingPage.test.tsx` — привязка и переход к следующему предложению.
- [ ] `EventsPage.test.tsx` — фильтр уровня и раскрытие строки.

## Критерии приёмки
- [ ] Снимки при 1280 px совпадают с макетами 09, 18–23.
- [ ] Код админки не попадает в чанки пользовательских страниц (`npm run analyze`).
- [ ] Опрос останавливается, когда вкладка скрыта.
- [ ] Опасные действия (выключение магазина, возврат DLQ) — только после подтверждения.
- [ ] `npm run lint`, `typecheck`, `test`, `build` зелёные, CI frontend проходит.

## План коммитов
1. `feat: admin layout, polling and access guard`
2. `feat: admin overview with health and activity`
3. `feat: parsers and queues pages`
4. `feat: users and notifications log`
5. `feat: manual offer matching`
6. `feat: virtualized events journal`
7. `test: admin screens`
