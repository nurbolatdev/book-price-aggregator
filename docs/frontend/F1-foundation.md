# F1 — Фундамент frontend

**Ветка:** `feature/f1-foundation` · **Зависит от:** M21 · **Оценка:** 1–2 дня

## Цель
Подготовить основу нового UI: TypeScript, бренд и дизайн-токены, layout, ленивый роутинг, API-клиент с обновлением токенов, TanStack Query, обработка ошибок, toasts и skeleton-компоненты. Страницы F2–F6 пишутся уже на этой основе.

## Исходное состояние
Frontend остался от M0–M11 (JavaScript, `.jsx`). Он вызывает удалённые эндпоинты (`/books/search`, `/books/{id}/offers`, `/alerts`) и ждёт старый ответ входа (`token` вместо `accessToken`/`refreshToken`) — с текущим API он не работает. Поэтому F1 заменяет его целиком, а не чинит.

## Принятые решения
- **TypeScript** (`strict`). Типы API генерируются из `docs/api/openapi.json` (`openapi-typescript`), сгенерированный файл коммитится.
- **Старый UI удаляется** полностью: страницы, `components/`, `context/AuthContext.jsx`, `api/*.js`, включая избранное. Backend-часть удаления избранного (эндпоинты `/favorites`, миграция drop `favorites`) остаётся в F3.
- **Токены:** access — только в памяти (Zustand), refresh — в `localStorage` (ключ `kr.refreshToken`). Backend отдаёт пару в теле ответа, cookie нет.
- **Toasts** — свой небольшой компонент на Zustand, без отдельной библиотеки.
- Порт dev-сервера Vite — `5173` (совпадает с CORS по умолчанию в backend).

## Задачи

### 1. Инструменты
- [ ] TypeScript: `tsconfig.json` (`strict`, `noUncheckedIndexedAccess`), `vite.config.ts`, `tailwind.config.ts`; `build` = `tsc -b && vite build`.
- [ ] ESLint 9 (flat config, `typescript-eslint`, `eslint-plugin-react-hooks`) — скрипт `lint`.
- [ ] Vitest + React Testing Library + jsdom — скрипт `test` (`vitest run`).
- [ ] `gen:api` — `openapi-typescript ../docs/api/openapi.json -o src/api/schema.d.ts`.

### 2. Бренд и токены
- [ ] Иконки из `docs/brand/` в `frontend/public/` и ссылки в `index.html` — по `docs/brand/README.md`, раздел «Подключение во frontend». `<title>KitapRadar</title>`, `lang="ru"`.
- [ ] Шрифты Unbounded и Onest (Google Fonts, `display=swap`, `preconnect`, кириллица).
- [ ] Tailwind: цвета, шрифты, радиусы из таблицы токенов `ROADMAP.md`; старая палитра `primary` удаляется.
- [ ] `formatPrice(3150)` → `3 150 ₸` (`Intl.NumberFormat('ru-RU')`), с тестом.

### 3. Layout и роутинг
- [ ] `AppLayout`: шапка (логотип `logo-full.svg`, `alt="KitapRadar"`, ссылки «Войти»/профиль), `<main>`, подвал. Адаптивно, цели касания ≥ 44 px.
- [ ] Роуты через `React.lazy` + `Suspense` со skeleton-заглушкой: `/`, `/search`, `/editions/:id`, `/login`, `/register`, `/beacons`, `/profile`, `/admin`, `*` (404). Страницы F2–F6 пока — заглушки с заголовком.
- [ ] Защищённые роуты: `RequireAuth` (→ `/login?next=…`), `RequireAdmin` (роль `ADMIN`).

### 4. API-клиент
- [ ] axios-инстанс, `baseURL` `/api/v1`, заголовок `Authorization: Bearer <access>`.
- [ ] Перехватчик: на 401 с `code = TOKEN_EXPIRED` — один вызов `POST /auth/refresh` для всех параллельных запросов (одна общая очередь), затем повтор исходных запросов. Сам `/auth/refresh` и `/auth/login` не повторяются. Неудачный refresh → выход из сессии, без жёсткой перезагрузки страницы.
- [ ] `ApiError` из `ErrorResponse` (`status`, `code`, `message`, `fieldErrors`) и словарь понятных сообщений по кодам.
- [ ] `auth`-store (Zustand): `login`, `logout` (`POST /auth/logout`), восстановление сессии по refresh-токену при старте.

### 5. Данные и обратная связь
- [ ] `QueryClientProvider`: `retry: 2` (не для 4xx), `refetchOnWindowFocus: false`; константы `staleTime` (поиск 60 с, карточка 5 мин, справочники 1 ч).
- [ ] Error Boundary на уровне страницы (сброс при смене маршрута) и глобальный.
- [ ] Toasts: `success`/`error`/`info`, автоскрытие, `aria-live="polite"`; ошибки мутаций — toast по умолчанию.
- [ ] Skeleton: `Skeleton`, `SkeletonText`, `SkeletonCard` с фиксированными размерами.

### 6. Сборка и запуск
- [ ] `Dockerfile` (`npm ci`) и `nginx.conf` работают с новой сборкой; `docker compose up -d frontend` открывает новый layout.
- [ ] README: раздел про frontend (команды `dev`, `lint`, `test`, `gen:api`).

## Критерии приёмки
- [ ] `npm run lint`, `npm test`, `npm run build` зелёные; `bash .claude/gate.sh` → `GATE PASS`.
- [ ] Тесты: перехватчик refresh (один refresh на несколько параллельных 401, повтор запросов, выход при неудачном refresh), `ApiError`, `formatPrice`, Error Boundary, toasts, `RequireAuth`.
- [ ] В вкладке браузера — favicon KitapRadar, шрифты Unbounded/Onest загружаются.
- [ ] Каждая страница — отдельный чанк в `dist/assets`; основной чанк < 200 КБ gzip.
- [ ] В `frontend/src` нет `.js`/`.jsx` и обращений к удалённым эндпоинтам.

## План коммитов
1. `build(frontend): typescript, eslint and vitest setup`
2. `feat(frontend): brand assets, fonts and design tokens`
3. `refactor(frontend): remove legacy ui`
4. `feat(frontend): layout and lazy routes`
5. `feat(frontend): api client with token refresh and generated types`
6. `feat(frontend): query provider, error boundary, toasts and skeletons`
7. `build(frontend): docker image and readme`
8. `docs: update milestone status`
