# M19 — Маячки и email-уведомления

**Ветка:** `feature/m19-beacons-notifications` · **Зависит от:** M18 · **Оценка:** 2–3 дня

## Цель
Главная пользовательская функция: пользователь ставит маячок на издание, при изменении цены система проверяет условие и отправляет письмо (сразу или в ежедневной сводке). Без спама, с отпиской в один клик.

## Перед началом прочитать
`docs/03-api-contract.md` (раздел 4), `docs/02-data-model.md` (`beacons`, `notifications`), макеты экранов 4–6 («Маячок», «Мои маячки», «Email: цена упала»).

## Задачи

### 1. Миграция V12__notifications.sql
Фактически `V16__notifications.sql` (V12–V15 заняты в M17–M18). Там же частичный индекс маячков `(edition_id) WHERE status IN (ACTIVE, TRIGGERED)` вместо прежнего только по ACTIVE (см. п. 3).
- [x] Таблица `notifications` + индексы.

### 2. API маячков — `beacon/`
- [x] `BeaconService`: `create`, `list(status)`, `update(id, condition|status)`, `delete(id)`. Только свои маячки (иначе 404, не 403 — не раскрываем существование).
- [x] Валидация по типу условия (`BELOW_PRICE` → `targetPrice > 0`; `DISCOUNT_PERCENT` → 1–90; `ANY_DROP` → без параметров). Дубликат на издание → 409.
  Проверка — аннотацией `@ValidBeaconCondition` на запросе: ошибка приходит в `fieldErrors` нужного поля (422). Параметры, не нужные условию, очищаются. PATCH: новое условие передаётся целиком (тип + параметр); `TRIGGERED` вручную поставить нельзя. Смена условия сработавшего маячка тоже «перевзводит» его (новый baseline, `last_notified_price = null`).
- [x] При создании и при возврате из `TRIGGERED`/`PAUSED` в `ACTIVE`: `baseline_price` = текущая лучшая цена издания (в наличии), `last_notified_price = null`.
  Если в момент создания ни один магазин не продаёт издание, `baseline_price = null`: первая появившаяся цена становится baseline при проверке (не срабатывание).
- [x] Список — с текущей лучшей ценой и магазином **одним запросом** (без N+1), сортировка: сработавшие сверху, затем по дате.
  Один SQL-запрос с `LEFT JOIN LATERAL` (лучшая цена и магазин); `BeaconServiceIT` проверяет, что запрос ровно один.
- [x] `BeaconController` по контракту. После создания — сразу вызвать `RefreshOnDemandService.requestIfStale(editionId)`.

### 3. Проверка условий — `beacon/BeaconChecker`
- [x] Listener очереди `kr.price-changed.beacons` (заглушка из M17 заменяется).
- [x] Алгоритм для `PriceChangedMessage`:
  1. лучшая текущая цена издания `best` (в наличии, `MATCHED`) и её магазин;
  2. активные маячки издания (`status = ACTIVE`), пачками по 500;
     Фактически проверяются `ACTIVE` **и** `TRIGGERED`: иначе критерий «ещё ниже → второе письмо» недостижим (после первого письма маячок становится TRIGGERED). `PAUSED` не проверяются. Строки пачки блокируются (`FOR UPDATE`), чтобы два одновременных события одного издания не прислали одно снижение дважды.
  3. для каждого `BeaconConditionEvaluator.isSatisfied(beacon, best)`:
     - `ANY_DROP`: `best < baseline_price`;
     - `BELOW_PRICE`: `best <= target_price`;
     - `DISCOUNT_PERCENT`: `best <= baseline_price * (1 - target_percent/100)`;
  4. **защита от повторов:** письмо только если `last_notified_price == null` или `best < last_notified_price`;
  5. сработал → `status = TRIGGERED`, `last_triggered_at`, `last_notified_price = best`; запись `notifications` (QUEUED); если у пользователя `notify_frequency = INSTANT` и email подтверждён → `EmailPublisher` (`PRICE_DROP`).
- [x] Пользователь без подтверждённого email: маячок срабатывает (видно в UI), письмо не отправляется, `notifications.status = FAILED`, `error = EMAIL_NOT_VERIFIED`.
- [x] Всё в одной транзакции на пачку; письма публикуются после коммита.
  Письмо «было → стало»: «было» — последняя цена, о которой сообщили, иначе baseline. **Найдена и исправлена ошибка M17:** ключ идемпотентности `processed:v1:{messageId}` был общим для всех потребителей, а `price.changed` уходит в две очереди (маячки и кэш) — один из потребителей пропускал сообщение. Теперь ключ `processed:v1:{consumer}:{messageId}`.

### 4. Письма — `notification/`
- [x] Шаблон `templates/email/price-drop.html` по макету «Email: цена упала»: обложка, было → стало, разница, магазин, кнопка «Открыть в <магазин>», ссылка «Сравнить все цены», цены в других магазинах, время сбора цен, ссылки «Отключить этот маячок» и «Настройки уведомлений».
  Макеты UI в репозитории не лежат (ссылку на холст даёт автор), шаблон собран по описанию из задания и в стиле писем M18. Есть текстовая версия. Цены — `3 150 ₸`, время сбора — по Алматы.
- [x] Тема: `Цена упала: «<название>» — <цена> ₸`.
- [x] Ссылка отписки: токен `UNSUBSCRIBE` (`EmailTokenService`, TTL 90 дней, многоразовый для этого маячка) → `${app.base-url}/unsubscribe?token=...`; `POST /api/v1/beacons/unsubscribe {token}` ставит маячок на паузу без входа.
  Таблица `email_tokens` не знала, к какому маячку относится ссылка: добавлена колонка `beacon_id` (`V17__unsubscribe_tokens.sql`). Ответ `POST /beacons/unsubscribe` — 200 `{beaconId, editionId, title, status}` (а не 204), чтобы страница могла назвать книгу. Повторный клик безвреден. Срок — `app.auth.unsubscribe-ttl` (90 дней).
- [x] Заголовок письма `List-Unsubscribe` с этой ссылкой.
  Ссылка ведёт на страницу фронтенда, поэтому `List-Unsubscribe-Post` (one-click по RFC 8058) не добавлен — для него нужен POST-адрес без тела.
- [x] `EmailListener` после отправки обновляет `notifications` → `SENT` (`sent_at`) или `FAILED` (`error`).
  Неудачная попытка сразу ставит `FAILED` с текстом ошибки, успешный повтор — `SENT`; отправленное не перезаписывается.

### 5. Ежедневная сводка
- [x] `DigestScheduler` — каждый день в 09:00 `Asia/Almaty` (`@SchedulerLock`): для пользователей с `notify_frequency = DAILY` собрать `notifications` со статусом QUEUED за сутки → одно письмо `DIGEST` (шаблон `digest.html`, список книг с ценами) → статусы `SENT`.
  Логика — `DigestService`: у одного маячка в сводке только последняя цена; уведомления удалённых и поставленных на паузу маячков → `FAILED` (`BEACON_INACTIVE`); у каждой книги — своя ссылка отписки. Расписание — `app.scheduler.digest-cron`.
- [ ] (по желанию, если время есть) еженедельная подборка скидок для `weekly_digest = true` — по понедельникам, из `DropsService`.
  Не сделано: необязательная часть, перенесена на потом (поле `weekly_digest` уже есть в профиле).

## Тесты
- [x] `BeaconConditionEvaluatorTest` — все условия, граничные значения (равно цели, ровно N%).
- [x] `BeaconServiceIT` — CRUD, чужой маячок → 404, дубликат → 409, baseline при создании.
- [x] `BeaconCheckerIT` (Testcontainers RabbitMQ + GreenMail) — изменение цены → маячок TRIGGERED → письмо отправлено; та же цена повторно → письма нет; ещё ниже → второе письмо; email не подтверждён → письма нет, FAILED.
- [x] `DigestSchedulerIT` — 3 срабатывания у DAILY-пользователя → одно письмо.
- [x] `UnsubscribeIT` — токен ставит маячок на паузу без авторизации; неверный токен → 400 `TOKEN_INVALID`.
- [x] `PriceDropEmailTemplateTest` — все поля шаблона заполнены, ссылки абсолютные.
  Ещё: `BeaconCheckerIT` проверяет, что приостановленный маячок и невыполненное условие писем не дают.

## Критерии приёмки
- [x] End-to-end локально: поставить маячок (Swagger) → вручную изменить цену предложения SQL-запросом или запустить парсер → письмо в Mailpit по макету.
- [x] Повторные письма на ту же цену не приходят.
- [x] Отписка из письма работает без входа.
- [x] DAILY-пользователь получает одну сводку.
- [ ] `./mvnw -q verify` зелёный.
  Unit-тесты зелёные (165). Интеграционные — на локальных PostgreSQL 16, Redis, RabbitMQ вместо Testcontainers (в среде нет Docker): 159 из 162, не запускаются три теста `MigrationIT`.

Проверено вживую (jar + Mailpit, событие `price.changed` опубликовано через RabbitMQ API после изменения цены SQL-запросом): письмо «Цена упала» с заголовком `List-Unsubscribe`; та же цена — письма нет; ниже — второе письмо «3 150 → 2 990»; отписка по ссылке без входа ставит паузу, дальнейшие снижения писем не дают; DAILY-пользователь получил одну сводку (расписание на время проверки — раз в минуту).

## Надёжность доставки писем (по итогам двух раундов ревью)
- [x] Ключ идемпотентности в Redis ставится **после** успешной обработки (раньше — до неё: сбой посреди отправки терял письмо). Источник истины — база.
- [x] Обработчик писем: если все уведомления письма уже `SENT` — не отправляет (защита от повторно опубликованной копии). Порядок: проверка в БД → SMTP → `SENT` → ключ Redis → подтверждение сообщения.
- [x] Публикация писем маячков ждёт подтверждения брокера (ack и не возвращено как неадресуемое при `mandatory`), не дольше 5 с; затем `published_at` (`V18__notification_delivery.sql`).
- [x] `BeaconEmailRelay` раз в минуту (ShedLock, `FOR UPDATE SKIP LOCKED`) публикует повторно только `QUEUED` + `INSTANT` + неопубликованные старше 1 мин; через 24 ч — `FAILED` / `NOT_PUBLISHED`. Сообщения в очереди и в DLQ не трогает — их повторы остаются за RabbitMQ.
- [x] Способ доставки (`INSTANT` / `DIGEST`) фиксируется при создании уведомления; сводка берёт только неопубликованные `DIGEST`.
- [x] Тайм-ауты SMTP: подключение 5 с, чтение и запись 10 с (по умолчанию JavaMail ждёт бесконечно).
- [x] Две копии одного письма, обработанные одновременно, отправляют его один раз: строки уведомления блокируются (`FOR UPDATE`) от проверки до записи `SENT`. Окончательные причины отказа (`NotificationErrors.FINAL`) закрывают уведомление — поздняя копия не отправляется; ошибка SMTP остаётся временной. Тесты — `NotificationSendingIT`.
- [x] Правило писем зафиксировано: письмо — только о **новом минимуме** относительно последней сообщённой цены (3 000 → 3 800 → 3 000 — письма нет; → 2 950 — есть).
- [x] Тесты сценариев отказа — `BeaconEmailDeliveryIT` (9), одновременное создание маячка — `BeaconServiceIT`. Тест одновременной проверки без блокировки строк падает (2 уведомления вместо 1).

Подробно — [M19-report.md](M19-report.md).

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

Сверх плана: `docs: mark M18 as done`, `fix: keep message idempotency per consumer`, `fix: reliable delivery of beacon emails`, `fix: serialize sending of one notification email`.

## Не входит
Telegram (v2), веб-push (v2).
