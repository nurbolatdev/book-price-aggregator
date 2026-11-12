# F4 — Вход, регистрация, пароль, профиль

**Ветка:** `feature/f4-auth` · **Зависит от:** F3 · **Оценка:** 2–3 дня

## Цель
Полный цикл аккаунта: регистрация с подтверждением почты, вход, восстановление пароля, профиль с настройками уведомлений, сменой пароля, выходом со всех устройств и удалением аккаунта. Макеты 08, 10, 11, 12, 14.

## Перед началом прочитать
`docs/frontend/SCREENS.md` — экраны 08, 10, 11, 12, 14 и §1 «Ошибки API»; макеты `08-signup`, `10-login`, `11-password-reset`, `12-verify-email`, `14-profile`, `15-states`; ADR-004; `docs/03-api-contract.md` — разделы 2 (аутентификация) и 3 (пользователь).

## Задачи

### 0. Refresh-токен в cookie (backend сделал в M22, ADR-005)
- [x] API-клиент: `withCredentials: true` для `/auth/**`; `refresh` и `logout` — без тела; убрать `kr.refreshToken` из `localStorage` (при первом запуске — удалить старое значение). Восстановление сессии при старте — просто `POST /auth/refresh` (cookie уйдёт сама).
- [x] Между вкладками: обновление токена под `navigator.locks.request('kr-refresh', …)`; новая пара и выход рассылаются через `BroadcastChannel('kr-auth')` — вторая вкладка не шлёт свой refresh, а берёт результат первой; выход в одной вкладке — выход во всех.
- [x] После перехода — попросить backend убрать `refreshToken` из тела ответа (отдельный коммит `refactor(auth): drop refresh token from response body` в этой же ветке, с тестом).

### 1. Формы — общие правила
- [x] Новые компоненты в `src/components/ui/`: `Modal` (focus trap, Esc, возврат фокуса; на телефоне — `Sheet`), `PasswordInput` (показать/скрыть). Зависимости `react-hook-form`, `zod`, `@hookform/resolvers`.
- [x] React Hook Form + Zod; схемы в `features/auth/schemas.ts`: email, пароль 8–100 («Минимум 8 символов»), имя 1–100.
- [x] Ошибки сервера: `VALIDATION_FAILED` → `applyFieldErrors` под полями; `RATE_LIMITED` → жёлтая плашка «Слишком много попыток. Подождите N секунд», кнопка неактивна с обратным отсчётом из `Retry-After` (макет 10, «Войти · 0:45»).
- [x] Кнопка отправки — состояние загрузки, двойной клик не шлёт два запроса. `autocomplete` для всех полей.

### 2. Регистрация `/register` и `/check-email` — макет 08
- [x] Имя, Email, Пароль, обязательный чекбокс согласия на письма (без него кнопка неактивна; на backend не отправляется).
- [x] `POST /auth/register` → `/check-email?email=…`; `EMAIL_TAKEN` → ошибка под email со ссылкой «Войти».
- [x] `/check-email`: текст с email, «Отправить письмо ещё раз» (`POST /auth/resend-verification`, кнопка неактивна 60 с, toast «Письмо отправлено»), «Неверный email? Зарегистрируйтесь заново» → `/register`, ссылка «Войти».

### 3. Вход `/login` — макет 10
- [x] `POST /auth/login` → `authStore.login()` (access в памяти, refresh в cookie, `user` в кэш `me`) → переход на `next` (только относительные пути, иначе `/`).
- [x] `INVALID_CREDENTIALS` → красная плашка над формой и красная рамка email; «Забыли пароль?» → `/forgot-password`; «Создать аккаунт» → `/register`.
- [x] Вошедший пользователь на `/login` или `/register` → редирект на `/`.

### 4. Восстановление пароля — макет 11 (4 состояния)
- [x] `/forgot-password`: email → `POST /auth/forgot-password` → всегда шаг «Проверьте почту» (не раскрываем, есть ли такой email).
- [x] `/reset-password?token=`: два поля, совпадение проверяется на клиенте; `POST /auth/reset-password` → toast «Пароль изменён, войдите» → `/login`. `TOKEN_INVALID`/`TOKEN_EXPIRED` → шаг «Ссылка устарела» → «Запросить новую ссылку».

### 5. Подтверждение почты `/verify-email?token=` — макет 12
- [x] При загрузке один раз `POST /auth/verify-email` (защита от двойного вызова в StrictMode). Успех — «Почта подтверждена» + «Перейти к поиску»; если вошёл — обновить кэш `me`. Ошибка токена — «Ссылка устарела» + «Отправить письмо ещё раз» (для вошедшего — без ввода email; для гостя — поле email).

### 6. Профиль `/profile` — макет 14
- [x] Плашка «Почта не подтверждена» (только при `emailVerified=false`) с «Отправить письмо ещё раз».
- [x] «Личные данные»: имя → `PATCH /me {name}` по кнопке «Сохранить»; email только для чтения с плашкой статуса.
- [x] «Уведомления»: «Как часто» (`INSTANT`/`DAILY`) и «Еженедельная подборка скидок» → `PATCH /me` сразу при изменении, оптимистично, toast при ошибке с откатом. Компонент `NotificationSettings` переиспользуется на экране маячков (F5).
- [x] «Пароль»: текущий + новый → `POST /me/password {currentPassword, newPassword}` → toast «Пароль изменён, другие устройства отключены»; неверный текущий — ошибка под полем.
- [x] «Выйти» → `authStore.logout()`; «Выйти на всех устройствах» → `POST /auth/logout-all` → очистить всё → `/login`.
- [x] «Удалить аккаунт…» → `Modal` с пояснением и полем пароля → `DELETE /me {password}` → очистить токены и кэш → `/` + toast «Аккаунт удалён». Для ADMIN кнопка скрыта (backend всё равно вернёт 409).

### 7. Снимки
- [x] Все экраны и их состояния (ошибки, отсчёт, устаревшая ссылка) — в `e2e/screens.config.ts` при 960/1280 и 390 px.

## Тесты
- [x] `LoginPage.test.tsx` — успех с `next`, внешний `next` игнорируется, неверный пароль, 429 с отсчётом.
- [x] `RegisterPage.test.tsx` — чекбокс согласия, `EMAIL_TAKEN`, переход на `/check-email`.
- [x] `ResetPassword.test.tsx` — несовпадение паролей, устаревший токен.
- [x] `VerifyEmail.test.tsx` — запрос уходит один раз.
- [x] `SettingsPage.test.tsx` — оптимистичное изменение уведомлений с откатом, смена пароля, удаление аккаунта.

## Критерии приёмки
- [ ] Снимки совпадают с макетами 08, 10, 11, 12, 14 (десктоп) и правилами телефона из `mockups/README.md`.
- [x] Обновление токена прозрачно: после истечения access-токена пользователь продолжает работу без повторного входа.
- [x] Ни один экран не раскрывает, существует ли email (forgot-password, resend).
- [ ] Формы доступны с клавиатуры, ошибки связаны с полями (`aria-describedby`, `aria-invalid`).
- [ ] `npm run lint`, `test`, `build` (включает `tsc`) зелёные, CI frontend проходит.

## План коммитов
1. `feat: auth form schemas and rate limit handling`
2. `feat: sign up, check email and email verification`
3. `feat: login with return path`
4. `feat: password recovery flow`
5. `feat: profile settings, password change and account deletion`
6. `test: auth and profile screens`
