# 04. Правила разработки

## 1. Код (Java)
- Java 21: `record` для DTO и сообщений очередей, `switch` с pattern matching, `var` — где тип очевиден.
- Lombok: `@RequiredArgsConstructor`, `@Getter/@Setter` на сущностях, `@Slf4j`. **Не** `@Data` на JPA-сущностях (equals/hashCode ломают Hibernate).
- Внедрение зависимостей только через конструктор.
- Деньги — `BigDecimal` (scale 2), сравнение через `compareTo`, никогда `double`.
- Время — `Instant` в коде и `TIMESTAMPTZ` в БД; текущее время берём из бина `Clock` (удобно в тестах).
- Константы конфигурации — через `@ConfigurationProperties` (record), не россыпь `@Value`.
- Никаких `System.out`, `printStackTrace`, пустых `catch`.
- Комментарии — зачем, а не что. Публичные методы сервисов — короткий Javadoc, если логика неочевидна.

## 2. Слои
```
Controller  → только HTTP: валидация (@Valid), вызов сервиса, маппинг в DTO-ответ
Service     → бизнес-логика, транзакции (@Transactional на публичных методах сервиса)
Repository  → Spring Data JPA; сложные запросы — @Query (JPQL) или native для pg_trgm
Entity      → JPA, без бизнес-логики сверх простых методов состояния
dto/        → record-классы Request/Response
```
- `open-in-view: false` (уже стоит) — значит, всё нужное для ответа загружаем в сервисе (fetch join / `@EntityGraph` / DTO-проекции).
- `@Transactional(readOnly = true)` для чтения.
- `@Async` и `@Cacheable` работают только при вызове **из другого бина** (прокси Spring). Не вызывать такие методы через `this`.
- `@Cacheable` — только на методах **без побочных эффектов** (чтение).

## 3. Ошибки
- Бизнес-исключения наследуют `AppException(ErrorCode code, String message)`.
- `GlobalExceptionHandler` превращает их в формат ошибки из `docs/03-api-contract.md`.
- 500 — без stack trace в ответе; stack trace — в лог с `traceId`.

## 4. Логи
- Уровни: `ERROR` — нужна реакция; `WARN` — деградация (магазин не ответил); `INFO` — ключевые события (запуск парсера, письмо отправлено); `DEBUG` — детали.
- В каждый лог запроса — `traceId` (MDC, фильтр в M12). В сообщения очередей — `messageId`, в лог парсера — `runId` и `store`.
- Не логировать пароли, токены, полные email (маскировать: `n***@example.com`).
- Важные события для админки дополнительно пишутся в `system_events` через `SystemEventService`.

## 5. Тесты
| Тип | Инструмент | Что покрывать | Имя |
|---|---|---|---|
| Unit | JUnit 5, Mockito, AssertJ | сервисы, условия маячков, нормализация текста, матчинг | `*Test` |
| Интеграционные | `@SpringBootTest` + **Testcontainers** (PostgreSQL, Redis, RabbitMQ) | репозитории, миграции, очереди, контроллеры (MockMvc) | `*IT` |
| Парсеры | сохранённые HTML/JSON в `src/test/resources/fixtures/<store>/` | извлечение цены, названия, ISBN, наличия | `*ParserTest` |
| Внешний HTTP | WireMock | Google Books, поведение при таймаутах/429 | |

- Базовый класс `AbstractIntegrationTest` с контейнерами (один набор контейнеров на весь прогон, `@ServiceConnection`).
- Тест не ходит в реальный интернет. Никогда.
- Шаблон теста: given / when / then, одно поведение — один тест, имя `shouldXxxWhenYyy`.
- Цель покрытия сервисного слоя — ≥ 70% (JaCoCo в M21).

## 6. Git
- Ветки: `feature/mNN-short-name` (backend), `feature/fN-short-name` (frontend), `fix/short-name`, `docs/short-name`.
- От свежего `main`, PR обратно в `main`, merge commit (как в истории M0–M11).
- **Сначала все коммиты вехи, потом PR.** Не открывать PR на середине.
- Не коммитить: `.env`, `target/`, `node_modules/`, IDE-файлы, дампы БД, локальные файлы инструментов разработчика.
- **Порядок работы:** одна веха в работе; backend M12–M22 полностью, затем frontend F1–F7. Следующая веха начинается только после мерджа предыдущей.

### Коммиты
Формат Conventional Commits, на английском, повелительное наклонение, ≤ 72 символа:
```
<type>: <что сделано>

[тело: зачем, если неочевидно]
```
Типы: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`.
Примеры:
```
feat: add pg_trgm fuzzy search for books
fix: call fetchAsync through proxy so @Async works
refactor: move isbn and publisher from books to editions
test: add Flip parser fixtures for price and stock
chore: add rabbitmq and mailpit to docker-compose
```
Сообщение коммита — только суть изменения: без служебных трейлеров (`Co-Authored-By` и т.п.), подписей и ссылок на сторонние сервисы. Автор коммитов — владелец репозитория.

Одна логическая правка — один коммит. Миграция и код, который от неё зависит, — в одном коммите (сборка не должна ломаться между коммитами).

### Описание PR (шаблон)
```markdown
## Веха MNN — <название>
### Что сделано
- ...
### Как проверить
1. docker compose up -d ...
2. curl ... → ожидаемый ответ
### Миграции
V.. — ...
### Критерии приёмки
- [x] ...
### Что осталось / заметки
- ...
```

## 7. Рабочая сессия и PROGRESS.md
`docs/PROGRESS.md` — короткая сводка «где остановились» (до ~60 строк).

**Начало сессии**
1. `git fetch`; если есть feature-ветка текущей вехи (указана в PROGRESS.md) — переключиться на неё, иначе `main`.
2. Прочитать `docs/PROGRESS.md` и файл текущей вехи. Остальные документы и код — только по мере необходимости для задачи.
3. Продолжить со «Следующего шага» без повторного анализа проекта.

**Конец сессии**
1. Закоммитить сделанное по плану коммитов вехи.
2. Обновить `docs/PROGRESS.md`: «Сейчас», «Последняя сессия», вопросы, строка в истории; старое из «Последней сессии» сжимать до строки в истории.
3. Отдельный коммит `docs: update progress`, push feature-ветки (PR — только после решения владельца).

Экономия контекста: не перечитывать файлы без нужды, гонять точечные тесты во время работы и полный `./mvnw -q verify` перед завершением вехи.

## 8. Конфигурация
- Профили: по умолчанию (`application.yml`, локальная разработка), `docker`, `test`.
- Все настройки приложения — под префиксом `app.` (`app.parser.*`, `app.mail.*`, `app.jwt.*`, `app.cache.*`).
- Новая переменная окружения → добавить в `.env.example` с комментарием и в `docker-compose.yml`.

## 9. Frontend (вехи F1–F7)
- **Структура `frontend/src/`:** `app/` (провайдеры, роутер, layout), `pages/<Page>Page.tsx` (одна страница — один lazy-модуль), `features/<feature>/` (компоненты и хуки одной функции: `search`, `edition`, `beacons`, `auth`, `admin`), `shared/ui/` (общие компоненты), `shared/lib/` (форматирование, хуки), `api/` (клиент, `schema.d.ts`, `queryKeys`), `mocks/` (MSW).
- **TypeScript strict**, без `any` и `@ts-ignore`. Типы ответов — только из `api/schema.d.ts` (генерируется из `docs/api/openapi.json`).
- **Имена:** компоненты `PascalCase.tsx`, хуки `useXxx.ts`, тесты рядом `Xxx.test.tsx`. Тексты интерфейса — на русском прямо в компонентах, как в макетах.
- **Стили:** только Tailwind-классы и токены из `tailwind.config.ts`; произвольные значения (`w-[372px]`) — только если так в макете. Inline-стили и новые цвета вне макетов не вводить.
- **Данные:** все запросы через TanStack Query; ключи — из `queryKeys`; состояние фильтров — в URL; глобальное состояние (Zustand) — только сессия.
- **Доступность:** у каждой кнопки-иконки `aria-label`, у полей — `label`, ошибки через `aria-describedby`; цели касания ≥ 44 px.
- **Тесты:** Vitest + Testing Library, поиск элементов по роли и тексту (`getByRole`), API — через MSW, без моков модулей axios.
- **Готовность экрана:** совпадает с макетом на снимке `npm run screens` (1280 и 390 px), есть состояния загрузки/пусто/ошибка.
