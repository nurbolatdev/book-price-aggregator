# F1 — Фундамент frontend

**Ветка:** `feature/f1-foundation` · **Зависит от:** M22 · **Оценка:** 3 дня

## Цель
Новый каркас приложения вместо фронтенда фазы 1: TypeScript, дизайн-токены из макетов, шапка и подвал, роутинг, API-клиент с обновлением токенов, общие компоненты и состояния. После F1 страницы F2–F6 собираются только из готовых деталей. Проверки (`lint`, `typecheck`, `test`, `build`) работают локально и в CI.

## Перед началом прочитать
`docs/frontend/ROADMAP.md` (стек, токены, приёмы скорости), `docs/frontend/SCREENS.md` §1 (общие правила, шапка, маршруты, ошибки API), `docs/frontend/mockups/README.md`, макет `15-states` (компоненты), `docs/brand/README.md`, ADR-004 (refresh-токены), `docs/03-api-contract.md` «Общие форматы».

## Решения (не пересматривать)
- **TypeScript** (`strict: true`). Типы API генерируются из `docs/api/openapi.json` командой `npm run api:types` (пакет `openapi-typescript`) в `src/api/schema.d.ts`; файл коммитится. Ручные интерфейсы ответов не писать.
- **Токены:** `accessToken` — только в памяти (Zustand), `refreshToken` — в `localStorage` под ключом `kr.refresh` (backend отдаёт его в теле ответа). При старте приложения, если есть `kr.refresh`, — тихий `POST /auth/refresh` до первого защищённого запроса.
- **Иконки:** `lucide-react`. **Шрифты:** `@fontsource/unbounded` (500, 700) и `@fontsource/onest` (400–700), подмножества latin + cyrillic — без запроса к Google Fonts.
- **Моки API:** MSW (`msw`) — для тестов и для режима `npm run dev:mock` (данные как в макетах). Фикстуры — `src/mocks/fixtures/*.ts`, типизированы по `schema.d.ts`.
- **Старый код фазы 1** (`src/pages/*`, `src/context/AuthContext.jsx`, `src/api/{favorites,alerts,books,auth}.js`, `src/components/*`) удаляется в этой вехе. Пока F2–F6 не готовы, маршруты показывают заглушку «Скоро здесь будет …» в общем layout.
- **Порт dev-сервера 5173** (совпадает с CORS backend по умолчанию); прокси `/api` → `http://localhost:8080` остаётся.

## Задачи

### 1. Инструменты и проверки
- [ ] Перевести проект на TypeScript: `tsconfig.json` (strict, `paths` `@/*` → `src/*`), `vite.config.ts`, `main.tsx`.
- [ ] Зависимости из ROADMAP: `@tanstack/react-query`, `react-router-dom` (6.x), `zustand`, `react-hook-form`, `zod`, `@hookform/resolvers`, `axios`, `lucide-react`, шрифты `@fontsource/*`. Dev: `typescript`, `eslint` (flat config) + `typescript-eslint` + `eslint-plugin-react-hooks` + `eslint-plugin-jsx-a11y`, `prettier`, `vitest`, `@testing-library/react`, `@testing-library/user-event`, `jsdom`, `msw`, `openapi-typescript`, `@playwright/test`, `rollup-plugin-visualizer`.
- [ ] Скрипты `package.json`: `dev`, `dev:mock`, `build` (`tsc -b && vite build`), `preview`, `lint`, `typecheck`, `test` (`vitest run`), `api:types`, `screens`, `analyze`.
- [ ] Закоммитить `package-lock.json`; в `Dockerfile` — `npm ci` вместо `npm install`.
- [ ] CI `.github/workflows/frontend.yml`: на PR и push в `main` при изменениях в `frontend/**` или `docs/api/openapi.json` — `npm ci`, `lint`, `typecheck`, `test`, `build`; проверка, что `npm run api:types` не меняет `schema.d.ts` (`git diff --exit-code`).

### 2. Дизайн-токены и базовые стили — `tailwind.config.ts`, `src/index.css`
- [ ] Цвета из ROADMAP и макетов: `bg #F4F6F8`, `surface #FFFFFF`, `ink #0E1A2B`, `muted #5A6576`, `line #DDE2E8`, `line-soft #EDF0F3`, `accent #C2410C` (+ `accent-soft #FFF4EC`), `good #0F6B45` на `good-soft #E7F5EE`, `best-row #F1FAF5`, `link #1D4ED8` / hover `#1E3A8A`, `warn #8A4B00` на `warn-soft #FFF4E0`, `danger #A11B1B` на `danger-soft #FDE8E8`, `neutral-soft #EEF1F4` (серые плашки статусов), админка: меню `ink`, активный пункт `#1F2E44`, текст меню `#C9D1DC`, разделители `#34404F`; цвета обложек-заглушек (6 шт., `SCREENS.md` §1). Редкие оттенки (графики, точки статусов) брать из HTML конкретного макета; цветов, которых нет в макетах, не вводить.
- [ ] Шрифты: `font-display` Unbounded, `font-sans` Onest. Радиусы: `btn 10px`, `card 16px`, `block 20px`. Контейнер `max-w-[1200px] px-6`; точки перелома `md 768`, `xl 1200`.
- [ ] Глобально: фон `bg`, цвет текста `ink`, видимый фокус (`outline 2px accent`, offset 2px) для клавиатуры, `prefers-reduced-motion` отключает анимации.

### 3. Бренд
- [ ] Логотип и favicon из `docs/brand/` по `docs/brand/README.md`: `public/favicon.svg`, `favicon-16`, `apple-touch-icon.png`, `icon-192/512.png`, `site.webmanifest`, `og:image` = `social-preview.png`; `<title>` по умолчанию «KitapRadar — сравнение цен на книги в Казахстане», `lang="ru"`.
- [ ] Компонент `Logo` (знак + слово, вариант для тёмного фона).

### 4. Каркас приложения — `src/app/`
- [ ] `providers.tsx`: `QueryClientProvider` (настройки по умолчанию из ROADMAP §1: `retry 2`, `refetchOnWindowFocus: false` для пользовательских страниц), `RouterProvider`, `Toaster`.
- [ ] `router.tsx`: все маршруты из `SCREENS.md` §1 «Маршруты», каждая страница через `React.lazy` + `Suspense` со скелетоном страницы; админка — отдельный чанк `admin`.
- [ ] `RequireAuth` (нет входа → `/login?returnTo=`) и `RequireAdmin` (не ADMIN → экран 404).
- [ ] Layout пользовательских страниц: шапка в двух вариантах (А — навигация, Б — поиск; `SCREENS.md` §1 «Шапка»), меню пользователя (макет 15), мобильная шапка с меню «≡» (макеты 16, 25, 26), подвал (макет 01). Layout форм (вход и т.п.): только логотип.
- [ ] Обновление `<title>` на каждой странице (`useDocumentTitle`); прокрутка наверх при смене маршрута, кроме «назад».

### 5. API-клиент — `src/api/`
- [ ] `client.ts`: axios `baseURL: '/api/v1'`, `Authorization` из Zustand-стора.
- [ ] Перехватчик 401 `TOKEN_EXPIRED`: один общий `POST /auth/refresh` (сохранить новую пару), повтор исходного запроса; параллельные запросы ждут тот же промис. Refresh вернул 401 → выход, toast «Сессия закончилась, войдите снова», `/login?returnTo=`.
- [ ] `ApiError` из тела ошибки (`code`, `message`, `fieldErrors`, `Retry-After`); хелпер `applyFieldErrors(form, error)` для React Hook Form.
- [ ] `authStore.ts` (Zustand): `accessToken`, `user`, `login()`, `logout()` (`POST /auth/logout {refreshToken}`, очистить токены и `queryClient.clear()`).
- [ ] Ключи запросов — фабрика `queryKeys` в одном файле (`search`, `edition`, `beacons`, `me`, `admin.*`).

### 6. Общие компоненты — `src/shared/ui/` (по макету 15 и повторяющимся элементам макетов)
- [ ] `Button` (primary — accent, secondary — белая с рамкой, dark — `ink`, ghost; размеры 40/44/48/52 px; состояние загрузки), `IconButton` (обязателен `aria-label`), `Input`, `PasswordInput` (показать/скрыть), `Checkbox`, `Radio`, `Switch`, `Select`, `Field` (подпись, подсказка, ошибка под полем — макет 15).
- [ ] `Chip` (фильтр/вкладка с числом), `Badge` (статусы: ok/warn/danger/neutral/good), `PriceTag` (форматирование, старая цена зачёркнутая), `Cover` (картинка с `loading="lazy"`, фиксированные размеры, заглушка цвета по `editionId % 6` с названием).
- [ ] `Card`, `Skeleton` (прямоугольник/текст/обложка), `Modal` (focus trap, Esc, возврат фокуса; на телефоне — нижняя панель), `Sheet` (нижняя панель), `Toast` + `toast()` (снизу справа, 4 с, на телефоне по центру, `role="status"`, действие «Отменить»), `Alert` (плашки info/warn/danger/offline), `Pagination`, `Tabs`.
- [ ] Состояния (макет 15): `NotFound` (404), `EmptyState`, `ErrorBlock` («Не удалось загрузить… Повторить»), `OfflineBanner` (`navigator.onLine`), `PageErrorBoundary` («Что-то пошло не так · Обновить страницу»).

### 7. Утилиты — `src/shared/lib/`
- [ ] `formatPrice(3150) → "3 150 ₸"` (`Intl.NumberFormat('ru-RU')`, неразрывный пробел), `formatPriceFrom`, `formatRelative(date)` («2 ч назад», «вчера», дата старше 7 дней), `formatDate`, `languageName('kk') → 'Қазақша'`, `coverColor(editionId)`, `pluralRu(n, ['магазин','магазина','магазинов'])`.
- [ ] `useDebounce`, `useMediaQuery('(max-width: 767px)')`, `useOnline`.

### 8. Моки и снимки экранов
- [ ] MSW: обработчики для всех эндпоинтов из `SCREENS.md` с фикстурами, повторяющими данные макетов (книги, цены, магазины, маячки, админка). `npm run dev:mock` запускает приложение на моках без backend.
- [ ] `npm run screens`: Playwright открывает приложение в режиме моков и сохраняет снимки каждого маршрута при ширине 1280 и 390 px в `frontend/screens/` (в `.gitignore`). Список маршрутов — `e2e/screens.config.ts`, пополняется в каждой следующей вехе.
- [ ] Страница-витрина `/dev/ui` (только в dev-сборке): все компоненты из задачи 6 во всех состояниях — для сравнения с макетом 15.

## Тесты
- [ ] `format.test.ts` — цены, относительное время, склонения.
- [ ] `client.test.ts` (MSW) — один refresh на несколько параллельных 401, повтор запроса; refresh 401 → выход.
- [ ] `RequireAuth.test.tsx`, `RequireAdmin.test.tsx` — редирект с `returnTo`, 404 для не ADMIN.
- [ ] `Modal.test.tsx` — фокус внутри, Esc закрывает, фокус возвращается.
- [ ] `Toast.test.tsx` — «Отменить» вызывает обработчик, автозакрытие.

## Критерии приёмки
- [ ] `npm run lint`, `typecheck`, `test`, `build` зелёные локально и в CI.
- [ ] Старых файлов фазы 1 нет; все маршруты из `SCREENS.md` открываются (заглушки там, где страница будет позже).
- [ ] Шапка (оба варианта, гость/вошёл, телефон), подвал и `/dev/ui` совпадают с макетами 01, 02, 15, 25 на снимках `npm run screens` (1280 и 390 px).
- [ ] Начальный JS главной ≤ 200 КБ gzip (`npm run analyze`), админка в отдельном чанке.
- [ ] `docker compose up` собирает frontend через `npm ci` и отдаёт приложение на `http://localhost`.

## План коммитов
1. `build: typescript, lint, tests and frontend ci`
2. `feat: design tokens, fonts and brand assets`
3. `feat: app shell with lazy routes, header and footer`
4. `feat: api client with token refresh and auth store`
5. `feat: shared ui components and page states`
6. `test: msw mocks and screenshot script`
7. `refactor: remove phase 1 frontend`
