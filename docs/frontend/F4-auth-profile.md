# F4 — Вход, регистрация, пароль, профиль

**Ветка:** `feature/f4-auth` · **Зависит от:** F3 · **Оценка:** 2–3 дня

## Цель
Полный цикл аккаунта: регистрация с подтверждением почты, вход, восстановление пароля, профиль с настройками уведомлений, сменой пароля, выходом со всех устройств и удалением аккаунта. Макеты 08, 10, 11, 12, 14.

## Перед началом прочитать
`docs/frontend/SCREENS.md` — экраны 08, 10, 11, 12, 14 и §1 «Ошибки API»; макеты `08-signup`, `10-login`, `11-password-reset`, `12-verify-email`, `14-profile`, `15-states`; ADR-004; `docs/03-api-contract.md` — разделы 2 (аутентификация) и 3 (пользователь).

## Задачи

### 1. Формы — общие правила
- [ ] Новые компоненты в `src/components/ui/`: `Modal` (focus trap, Esc, возврат фокуса; на телефоне — `Sheet`), `PasswordInput` (показать/скрыть). Зависимости `react-hook-form`, `zod`, `@hookform/resolvers`.
- [ ] React Hook Form + Zod; схемы в `features/auth/schemas.ts`: email, пароль 8–100 («Минимум 8 символов»), имя 1–100.
- [ ] Ошибки сервера: `VALIDATION_FAILED` → `applyFieldErrors` под полями; `RATE_LIMITED` → жёлтая плашка «Слишком много попыток. Подождите N секунд», кнопка неактивна с обратным отсчётом из `Retry-After` (макет 10, «Войти · 0:45»).
- [ ] Кнопка отправки — состояние загрузки, двойной клик не шлёт два запроса. `autocomplete` для всех полей.

### 2. Регистрация `/register` и `/check-email` — макет 08
- [ ] Имя, Email, Пароль, обязательный чекбокс согласия на письма (без него кнопка неактивна; на backend не отправляется).
- [ ] `POST /auth/register` → `/check-email?email=…`; `EMAIL_TAKEN` → ошибка под email со ссылкой «Войти».
- [ ] `/check-email`: текст с email, «Отправить письмо ещё раз» (`POST /auth/resend-verification`, кнопка неактивна 60 с, toast «Письмо отправлено»), «Неверный email? Зарегистрируйтесь заново» → `/register`, ссылка «Войти».

### 3. Вход `/login` — макет 10
- [ ] `POST /auth/login` → `authStore.login()` (access в памяти, refresh в `localStorage`, `user` в кэш `me`) → переход на `next` (только относительные пути, иначе `/`).
- [ ] `INVALID_CREDENTIALS` → красная плашка над формой и красная рамка email; «Забыли пароль?» → `/forgot-password`; «Создать аккаунт» → `/register`.
- [ ] Вошедший пользователь на `/login` или `/register` → редирект на `/`.

### 4. Восстановление пароля — макет 11 (4 состояния)
- [ ] `/forgot-password`: email → `POST /auth/forgot-password` → всегда шаг «Проверьте почту» (не раскрываем, есть ли такой email).
- [ ] `/reset-password?token=`: два поля, совпадение проверяется на клиенте; `POST /auth/reset-password` → toast «Пароль изменён, войдите» → `/login`. `TOKEN_INVALID`/`TOKEN_EXPIRED` → шаг «Ссылка устарела» → «Запросить новую ссылку».

### 5. Подтверждение почты `/verify-email?token=` — макет 12
- [ ] При загрузке один раз `POST /auth/verify-email` (защита от двойного вызова в StrictMode). Успех — «Почта подтверждена» + «Перейти к поиску»; если вошёл — обновить кэш `me`. Ошибка токена — «Ссылка устарела» + «Отправить письмо ещё раз» (для вошедшего — без ввода email; для гостя — поле email).

### 6. Профиль `/profile` — макет 14
- [ ] Плашка «Почта не подтверждена» (только при `emailVerified=false`) с «Отправить письмо ещё раз».
- [ ] «Личные данные»: имя → `PATCH /me {name}` по кнопке «Сохранить»; email только для чтения с плашкой статуса.
- [ ] «Уведомления»: «Как часто» (`INSTANT`/`DAILY`) и «Еженедельная подборка скидок» → `PATCH /me` сразу при изменении, оптимистично, toast при ошибке с откатом. Компонент `NotificationSettings` переиспользуется на экране маячков (F5).
- [ ] «Пароль»: текущий + новый → `POST /me/password {currentPassword, newPassword, refreshToken}` → toast «Пароль изменён, другие устройства отключены»; неверный текущий — ошибка под полем.
- [ ] «Выйти» → `authStore.logout()`; «Выйти на всех устройствах» → `POST /auth/logout-all` → очистить всё → `/login`.
- [ ] «Удалить аккаунт…» → `Modal` с пояснением и полем пароля → `DELETE /me {password}` → очистить токены и кэш → `/` + toast «Аккаунт удалён». Для ADMIN кнопка скрыта (backend всё равно вернёт 409).

### 7. Снимки
- [ ] Все экраны и их состояния (ошибки, отсчёт, устаревшая ссылка) — в `e2e/screens.config.ts` при 960/1280 и 390 px.

## Тесты
- [ ] `LoginPage.test.tsx` — успех с `next`, внешний `next` игнорируется, неверный пароль, 429 с отсчётом.
- [ ] `RegisterPage.test.tsx` — чекбокс согласия, `EMAIL_TAKEN`, переход на `/check-email`.
- [ ] `ResetPassword.test.tsx` — несовпадение паролей, устаревший токен.
- [ ] `VerifyEmail.test.tsx` — запрос уходит один раз.
- [ ] `SettingsPage.test.tsx` — оптимистичное изменение уведомлений с откатом, смена пароля, удаление аккаунта.

## Критерии приёмки
- [ ] Снимки совпадают с макетами 08, 10, 11, 12, 14 (десктоп) и правилами телефона из `mockups/README.md`.
- [ ] Обновление токена прозрачно: после истечения access-токена пользователь продолжает работу без повторного входа.
- [ ] Ни один экран не раскрывает, существует ли email (forgot-password, resend).
- [ ] Формы доступны с клавиатуры, ошибки связаны с полями (`aria-describedby`, `aria-invalid`).
- [ ] `npm run lint`, `test`, `build` (включает `tsc`) зелёные, CI frontend проходит.

## План коммитов
1. `feat: auth form schemas and rate limit handling`
2. `feat: sign up, check email and email verification`
3. `feat: login with return path`
4. `feat: password recovery flow`
5. `feat: profile settings, password change and account deletion`
6. `test: auth and profile screens`
