# M19 — Маячки и email-уведомления

**Ветка:** `feature/m19-beacons-notifications` · **Зависит от:** M18 · **Оценка:** 2–3 дня

## Цель
Главная пользовательская функция: пользователь ставит маячок на издание, при изменении цены система проверяет условие и отправляет письмо (сразу или в ежедневной сводке). Без спама, с отпиской в один клик.

## Перед началом прочитать
`docs/03-api-contract.md` (раздел 4), `docs/02-data-model.md` (`beacons`, `notifications`), макеты экранов 4–6 («Маячок», «Мои маячки», «Email: цена упала»).

## Задачи

### 1. Миграция V12__notifications.sql
- [ ] Таблица `notifications` + индексы.

### 2. API маячков — `beacon/`
- [ ] `BeaconService`: `create`, `list(status)`, `update(id, condition|status)`, `delete(id)`. Только свои маячки (иначе 404, не 403 — не раскрываем существование).
- [ ] Валидация по типу условия (`BELOW_PRICE` → `targetPrice > 0`; `DISCOUNT_PERCENT` → 1–90; `ANY_DROP` → без параметров). Дубликат на издание → 409.
- [ ] При создании и при возврате из `TRIGGERED`/`PAUSED` в `ACTIVE`: `baseline_price` = текущая лучшая цена издания (в наличии), `last_notified_price = null`.
- [ ] Список — с текущей лучшей ценой и магазином **одним запросом** (без N+1), сортировка: сработавшие сверху, затем по дате.
- [ ] `BeaconController` по контракту. После создания — сразу вызвать `RefreshOnDemandService.requestIfStale(editionId)`.

### 3. Проверка условий — `beacon/BeaconChecker`
- [ ] Listener очереди `kr.price-changed.beacons` (заглушка из M17 заменяется).
- [ ] Алгоритм для `PriceChangedMessage`:
  1. лучшая текущая цена издания `best` (в наличии, `MATCHED`) и её магазин;
  2. активные маячки издания (`status = ACTIVE`), пачками по 500;
  3. для каждого `BeaconConditionEvaluator.isSatisfied(beacon, best)`:
     - `ANY_DROP`: `best < baseline_price`;
     - `BELOW_PRICE`: `best <= target_price`;
     - `DISCOUNT_PERCENT`: `best <= baseline_price * (1 - target_percent/100)`;
  4. **защита от повторов:** письмо только если `last_notified_price == null` или `best < last_notified_price`;
  5. сработал → `status = TRIGGERED`, `last_triggered_at`, `last_notified_price = best`; запись `notifications` (QUEUED); если у пользователя `notify_frequency = INSTANT` и email подтверждён → `EmailPublisher` (`PRICE_DROP`).
- [ ] Пользователь без подтверждённого email: маячок срабатывает (видно в UI), письмо не отправляется, `notifications.status = FAILED`, `error = EMAIL_NOT_VERIFIED`.
- [ ] Всё в одной транзакции на пачку; письма публикуются после коммита.

### 4. Письма — `notification/`
- [ ] Шаблон `templates/email/price-drop.html` по макету «Email: цена упала»: обложка, было → стало, разница, магазин, кнопка «Открыть в <магазин>», ссылка «Сравнить все цены», цены в других магазинах, время сбора цен, ссылки «Отключить этот маячок» и «Настройки уведомлений».
- [ ] Тема: `Цена упала: «<название>» — <цена> ₸`.
- [ ] Ссылка отписки: токен `UNSUBSCRIBE` (`EmailTokenService`, TTL 90 дней, многоразовый для этого маячка) → `${app.base-url}/unsubscribe?token=...`; `POST /api/v1/beacons/unsubscribe {token}` ставит маячок на паузу без входа.
- [ ] Заголовок письма `List-Unsubscribe` с этой ссылкой.
- [ ] `EmailListener` после отправки обновляет `notifications` → `SENT` (`sent_at`) или `FAILED` (`error`).

### 5. Ежедневная сводка
- [ ] `DigestScheduler` — каждый день в 09:00 `Asia/Almaty` (`@SchedulerLock`): для пользователей с `notify_frequency = DAILY` собрать `notifications` со статусом QUEUED за сутки → одно письмо `DIGEST` (шаблон `digest.html`, список книг с ценами) → статусы `SENT`.
- [ ] (по желанию, если время есть) еженедельная подборка скидок для `weekly_digest = true` — по понедельникам, из `DropsService`.

## Тесты
- [ ] `BeaconConditionEvaluatorTest` — все условия, граничные значения (равно цели, ровно N%).
- [ ] `BeaconServiceIT` — CRUD, чужой маячок → 404, дубликат → 409, baseline при создании.
- [ ] `BeaconCheckerIT` (Testcontainers RabbitMQ + GreenMail) — изменение цены → маячок TRIGGERED → письмо отправлено; та же цена повторно → письма нет; ещё ниже → второе письмо; email не подтверждён → письма нет, FAILED.
- [ ] `DigestSchedulerIT` — 3 срабатывания у DAILY-пользователя → одно письмо.
- [ ] `UnsubscribeIT` — токен ставит маячок на паузу без авторизации; неверный токен → 400 `TOKEN_INVALID`.
- [ ] `PriceDropEmailTemplateTest` — все поля шаблона заполнены, ссылки абсолютные.

## Критерии приёмки
- [ ] End-to-end локально: поставить маячок (Swagger) → вручную изменить цену предложения SQL-запросом или запустить парсер → письмо в Mailpit по макету.
- [ ] Повторные письма на ту же цену не приходят.
- [ ] Отписка из письма работает без входа.
- [ ] DAILY-пользователь получает одну сводку.
- [ ] `./mvnw -q verify` зелёный.

## План коммитов
1. `feat: notifications table`
2. `feat: beacon crud api with validation`
3. `feat: beacon condition evaluator`
4. `feat: beacon checker on price changed events`
5. `feat: price drop email template and sending status`
6. `feat: one-click unsubscribe from beacon emails`
7. `feat: daily digest for beacon notifications`
8. `test: beacon and notification integration tests`
9. `docs: update milestone status`

## Не входит
Telegram (v2), веб-push (v2).
