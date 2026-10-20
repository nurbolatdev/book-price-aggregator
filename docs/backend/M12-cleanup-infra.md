# M12 — Чистка, исправление багов, инфраструктура

**Ветка:** `feature/m12-cleanup-infra` · **Зависит от:** — · **Оценка:** 1–2 дня

## Цель
Убрать фейковые данные и найденные баги, подготовить инфраструктуру Фазы 2 (RabbitMQ, Mailpit, Testcontainers, Swagger, `/api/v1`), чтобы следующие вехи строились на чистом основании.

## Перед началом прочитать
`docs/README.md`, `docs/04-conventions.md`, `docs/03-api-contract.md` (общие форматы, раздел 7).

## Текущее состояние (что не так)
| Проблема | Где |
|---|---|
| Моковые случайные цены | `source/KaspiMockAdapter.java`, `source/OzonMockAdapter.java` |
| Фейковая цена 2500 ₸ и «KZT» для Google Books | `source/GoogleBooksAdapter.java` → `extractPrice` |
| `@Async` не работает (вызов через `this`) | `book/BookSearchService.java` → `fetchAsync` |
| `@Cacheable` на методе, который пишет в БД | `book/BookSearchService.java` → `search` |
| Планировщик вызывает `search()` у моков, который возвращает пусто | `scheduler/PriceRefreshScheduler.java` |
| Нет Maven Wrapper (`mvnw`) | `backend/` |
| Пути API без версии | все `*Controller.java` |
| Нет тестов | `src/test` пуст |

## Задачи

### 1. Сборка
- [ ] Добавить Maven Wrapper в `backend/` (`mvn -N wrapper:wrapper -Dmaven=3.9.9`), закоммитить `mvnw`, `mvnw.cmd`, `.mvn/wrapper/`.
- [ ] Обновить Spring Boot до последнего патча 3.3.x.
- [ ] Добавить зависимости в `backend/pom.xml`:
  - `org.springframework.boot:spring-boot-starter-amqp`
  - `org.springdoc:springdoc-openapi-starter-webmvc-ui` (2.6.x)
  - test: `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`, `org.testcontainers:rabbitmq`, `com.redis:testcontainers-redis` (или `GenericContainer` для redis), `org.springframework.amqp:spring-rabbit-test`
  - Версии Testcontainers — через `testcontainers-bom` в `dependencyManagement`.

### 2. Docker Compose (`docker-compose.yml`)
- [ ] Добавить сервис `rabbitmq` — образ `rabbitmq:3.13-management-alpine`, порты `127.0.0.1:5672:5672`, `127.0.0.1:15672:15672`, healthcheck `rabbitmq-diagnostics -q ping`, volume `rabbitdata`.
- [ ] Добавить `mailpit` — образ `axllent/mailpit`, порты `127.0.0.1:1025:1025` (SMTP), `127.0.0.1:8025:8025` (UI).
- [ ] Postgres и Redis: порты только на `127.0.0.1` (нужны для `spring-boot:run` с хоста).
- [ ] `backend.depends_on` += `rabbitmq` (healthy), `mailpit`; передать новые переменные.
- [ ] `.env.example`: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`, `APP_BASE_URL` (с комментариями).

### 3. Конфигурация (`application.yml`, `application-docker.yml`)
- [ ] `spring.threads.virtual.enabled: true` (виртуальные потоки Java 21 для Tomcat и `@Async`).
- [ ] `spring.rabbitmq.*` (localhost/guest в dev, переменные в docker).
- [ ] `spring.mail.host/port` → Mailpit.
- [ ] `springdoc.swagger-ui.path: /swagger-ui.html`.
- [ ] Создать `config/AppProperties` (`@ConfigurationProperties("app")`, record): `baseUrl`, вложенные блоки добавятся в следующих вехах.
- [ ] Бин `Clock` (`Clock.systemUTC()`) в `config/TimeConfig`.

### 4. Удалить фейковые данные и баги
- [ ] Удалить `KaspiMockAdapter`, `OzonMockAdapter`.
- [ ] `GoogleBooksAdapter`: не выдумывать цену — адаптер возвращает только метаданные; предложение (offer) из Google Books **не создаётся**. Переименовать/перенести в `metadata/GoogleBooksClient` можно в M14, здесь — минимальная правка.
- [ ] `BookSearchService`: убрать `@Cacheable` с метода, который пишет в БД; убрать фазу «enrich price sources»; асинхронный вызов источников вынести в отдельный бин (или убрать — источник один). Временное решение до M14.
- [ ] Удалить `PriceRefreshScheduler` (заменяется в M17). Удалить `@EnableScheduling`, если больше не нужен.
- [ ] Удалить пустые `.gitkeep` в пакетах, где уже есть код.

### 5. API v1 и ошибки
- [ ] Все контроллеры: `/api/...` → `/api/v1/...`; обновить матчеры в `SecurityConfig`.
- [ ] Единственное разрешённое изменение фронтенда в этой вехе: `frontend/src/api/client.js` — `baseURL` на `/api/v1`.
- [ ] `common/error/ErrorCode` (enum со статусом HTTP), `AppException`, `NotFoundException extends AppException`.
- [ ] `GlobalExceptionHandler` → формат ошибки из `docs/03-api-contract.md` (`timestamp, status, code, message, path, fieldErrors`). Обработать: `AppException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`, `AccessDeniedException`, всё прочее → 500 `INTERNAL_ERROR`.
- [ ] JSON-ответы для 401/403 от Spring Security: свои `AuthenticationEntryPoint` и `AccessDeniedHandler` в том же формате.
- [ ] `config/OpenApiConfig`: название, версия, схема `bearerAuth` (JWT).

### 6. Наблюдаемость (минимум)
- [ ] `common/web/TraceIdFilter` — берёт `X-Request-Id` или генерирует UUID, кладёт в MDC `traceId`, возвращает в заголовке ответа.
- [ ] Паттерн логов с `%X{traceId}` (`logback-spring.xml` или `logging.pattern.console`).

### 7. Тесты
- [ ] `src/test/java/.../support/AbstractIntegrationTest` — Testcontainers PostgreSQL 16, Redis 7, RabbitMQ 3.13 с `@ServiceConnection`, контейнеры `static`, переиспользуются всеми тестами.
- [ ] `src/test/resources/application-test.yml`.
- [ ] `ApplicationIT` — контекст поднимается, Flyway применил миграции.
- [ ] `AuthControllerIT` — регистрация, вход, неверный пароль → 401 в формате ошибки, повторный email → 409.
- [ ] `GlobalExceptionHandlerTest` (MockMvc slice) — валидация даёт `fieldErrors`.

## Критерии приёмки
- [ ] `docker compose up -d` поднимает postgres, redis, rabbitmq, mailpit, backend, frontend; все healthy.
- [ ] `http://localhost:15672` (RabbitMQ UI) и `http://localhost:8025` (Mailpit) открываются.
- [ ] `http://localhost:8080/swagger-ui.html` показывает все эндпоинты под `/api/v1`, есть кнопка Authorize (JWT).
- [ ] В коде нет `Random` для цен, нет моков, нет цены 2500.
- [ ] Ошибки API приходят в едином формате, в ответе есть заголовок `X-Request-Id`.
- [ ] `cd backend && ./mvnw -q verify` — зелёный, есть минимум 3 интеграционных теста.

## План коммитов
1. `build: add maven wrapper and bump spring boot patch`
2. `chore: add rabbitmq and mailpit to docker-compose`
3. `build: add amqp, springdoc and testcontainers dependencies`
4. `refactor: remove mock price adapters and fake google books price`
5. `fix: remove cacheable from writing search method and broken async call`
6. `refactor: remove price refresh scheduler built on mocks`
7. `refactor: move api to /api/v1`
8. `feat: unified api error format with error codes`
9. `feat: add trace id filter and openapi config`
10. `test: add testcontainers base and first integration tests`
11. `docs: update milestone status`

## Не входит в веху
Новая модель данных (M13), новый поиск (M14), парсеры (M15+), очереди с логикой (M17).
