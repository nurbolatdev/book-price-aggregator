# Book Price Aggregator

Веб-платформа для сравнения цен на книги между онлайн-магазинами.
Собирает данные из внешних источников, агрегирует предложения, показывает
лучшую цену, ведёт историю цен и уведомляет о снижениях.

> Educational portfolio project. Не является коммерческим сервисом.

## Стек

- **Backend:** Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, Flyway
- **БД:** PostgreSQL
- **Кэш:** Redis
- **Контейнеризация:** Docker, docker-compose
- **Frontend:** React (Vite)

## Архитектура

Слоистая монолитная архитектура, пакеты организованы по фичам
(book, offer, source, aggregation, favorite, pricealert, notification...).
Источники данных скрыты за абстракцией `BookSource` (Strategy pattern),
что позволяет добавлять новые площадки без изменения логики агрегации.

## Запуск (локально)

```bash
cp .env.example .env   # заполнить значения
docker compose up -d   # поднимает postgres + redis
```

Backend и frontend добавляются в compose по мере разработки.

## Статус

🚧 В разработке. Развивается по milestone (см. историю коммитов).
