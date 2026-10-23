<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/brand/logo-full-dark.svg">
    <img src="docs/brand/logo-full.svg" alt="KitapRadar" width="360">
  </picture>
</p>

<p align="center">Сравнение цен на книги в магазинах Казахстана и «маячки» на снижение цены</p>

# KitapRadar (Book Price Aggregator)

Веб-приложение для сравнения цен на книги между несколькими онлайн-магазинами. Система собирает предложения из разных источников, находит лучшую цену и позволяет отслеживать снижение цен на книги из избранного.

> Educational portfolio project. Не является коммерческим сервисом.

## Технологии

**Backend:** Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Flyway  
**Кэширование:** Redis  
**База данных:** PostgreSQL 16  
**Frontend:** React 18, Vite, Tailwind CSS, React Router  
**Инфраструктура:** Docker, Docker Compose

## Функциональность

- Поиск книг по названию или автору с агрегацией из нескольких источников
- Сравнение цен от разных магазинов (см. «Магазины» ниже); метаданные книг — Google Books
- Регистрация и авторизация через JWT
- Добавление книг в избранное
- Подписка на снижение цены (price alerts)
- Автоматическое обновление цен каждые 6 часов
- История изменения цен

## Магазины

Цены собираются со страниц магазинов по правилам [docs/05-parsing-policy.md](docs/05-parsing-policy.md): только публичные страницы, `robots.txt`, низкая частота, честный User-Agent, без обхода защиты.

| Магазин | Статус |
|---|---|
| Flip.kz | ✅ работает |
| Meloman | ✅ работает (поиск запрещён `robots.txt` — новые книги ищутся по каталогу) |
| Ozon | ✅ работает (поиск запрещён `robots.txt` — новые книги ищутся по категории «Книги») |
| Kaspi.kz | ⛔ выключен: не отвечает парсеру, даже на `robots.txt` |
| Wildberries | ⛔ выключен: отвечает антибот-проверкой с капчей |

Проверено 2026-10-22. Выключенные магазины не обходятся, а остаются выключенными, пока не станут отвечать по этим правилам.

## Запуск

### Требования
- Docker и Docker Compose

### 1. Клонировать репозиторий

```bash
git clone https://github.com/nurbolatdev/book-price-aggregator.git
cd book-price-aggregator
```

### 2. Создать файл `.env`

```bash
cp .env.example .env
```

Заполнить значения в `.env`:

```env
POSTGRES_DB=bookaggregator
POSTGRES_USER=bookuser
POSTGRES_PASSWORD=your_password

DB_HOST=postgres
DB_PORT=5432
DB_NAME=bookaggregator
DB_USERNAME=bookuser
DB_PASSWORD=your_password

REDIS_HOST=redis
REDIS_PORT=6379

JWT_SECRET=your_secret_key_minimum_32_characters
JWT_ACCESS_TTL=15m
JWT_REFRESH_TTL=30d

GOOGLE_BOOKS_API_KEY=your_api_key
```

### 3. Запустить

```bash
docker compose up --build
```

Приложение будет доступно по адресу: **http://localhost**  
API: **http://localhost:8080/api/v1** · Swagger UI: **http://localhost:8080/swagger-ui.html**  
Health check: **http://localhost:8080/actuator/health**

## Локальная разработка

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend доступен на `http://localhost:3000`, запросы к `/api` проксируются на `localhost:8080`.

## API

| Метод | URL | Доступ | Описание |
|-------|-----|--------|----------|
| POST | `/api/v1/auth/register` | Публичный | Регистрация |
| POST | `/api/v1/auth/login` | Публичный | Вход |
| GET | `/api/v1/books/search?query=` | Публичный | Поиск книг |
| GET | `/api/v1/books/{id}` | Публичный | Детали книги |
| GET | `/api/v1/books/{id}/offers` | Публичный | Предложения по книге |
| GET | `/api/v1/books/{id}/price-history` | Публичный | История цен |
| GET | `/api/v1/favorites` | JWT | Список избранного |
| POST | `/api/v1/favorites/{bookId}` | JWT | Добавить в избранное |
| DELETE | `/api/v1/favorites/{bookId}` | JWT | Удалить из избранного |
| GET | `/api/v1/alerts` | JWT | Список алертов |
| POST | `/api/v1/alerts/{bookId}` | JWT | Создать алерт |
| DELETE | `/api/v1/alerts/{bookId}` | JWT | Удалить алерт |

## Архитектура

Слоистая монолитная архитектура, пакеты организованы по фичам. Источники данных скрыты за абстракцией `BookSource` (Strategy pattern) — добавить новую площадку = написать один класс.

```
book-price-aggregator/
├── backend/
│   └── src/main/java/kz/nurbolat/bookaggregator/
│       ├── book/         # Поиск и хранение книг
│       ├── offer/        # Предложения и история цен
│       ├── source/       # Адаптеры источников (Strategy pattern)
│       ├── favorite/     # Избранное
│       ├── pricealert/   # Алерты на снижение цены
│       ├── scheduler/    # Обновление цен по расписанию
│       ├── security/     # JWT авторизация
│       └── config/       # Конфигурации
└── frontend/
    └── src/
        ├── api/          # Axios клиент и API функции
        ├── components/   # Переиспользуемые компоненты
        ├── context/      # AuthContext
        └── pages/        # Страницы приложения
```
