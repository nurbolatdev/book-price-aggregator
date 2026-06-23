# Book Price Aggregator

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
- Сравнение цен от разных магазинов (Kaspi, Ozon, Google Books)
- Регистрация и авторизация через JWT
- Добавление книг в избранное
- Подписка на снижение цены (price alerts)
- Автоматическое обновление цен каждые 6 часов
- История изменения цен

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
JWT_EXPIRATION_MS=3600000

GOOGLE_BOOKS_API_KEY=your_api_key
```

### 3. Запустить

```bash
docker compose up --build
```

Приложение будет доступно по адресу: **http://localhost**  
API: **http://localhost:8080/api**  
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
| POST | `/api/auth/register` | Публичный | Регистрация |
| POST | `/api/auth/login` | Публичный | Вход |
| GET | `/api/books/search?query=` | Публичный | Поиск книг |
| GET | `/api/books/{id}` | Публичный | Детали книги |
| GET | `/api/books/{id}/offers` | Публичный | Предложения по книге |
| GET | `/api/books/{id}/price-history` | Публичный | История цен |
| GET | `/api/favorites` | JWT | Список избранного |
| POST | `/api/favorites/{bookId}` | JWT | Добавить в избранное |
| DELETE | `/api/favorites/{bookId}` | JWT | Удалить из избранного |
| GET | `/api/alerts` | JWT | Список алертов |
| POST | `/api/alerts/{bookId}` | JWT | Создать алерт |
| DELETE | `/api/alerts/{bookId}` | JWT | Удалить алерт |

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
