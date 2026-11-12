import type { Page } from '@playwright/test'

export interface ScreenEntry {
  name: string
  path: string
  widths?: (1280 | 960 | 390)[]
  waitFor?: 'networkidle' | 'load'
  signedIn?: boolean
  prepare?: (page: Page) => Promise<void>
}

export const screens: ScreenEntry[] = [
  { name: 'home', path: '/' },
  { name: 'search', path: '/search?q=Мастер и Маргарита' },
  { name: 'catalog', path: '/catalog' },
  { name: 'deals', path: '/deals' },
  {
    name: 'filters-mobile',
    path: '/search?q=Мастер и Маргарита',
    widths: [390],
    prepare: async (page) => {
      await page.getByRole('button', { name: /Фильтры/ }).click()
      await page.getByRole('dialog', { name: 'Фильтры' }).waitFor()
    },
  },
  { name: 'not-found', path: '/nope' },
  { name: 'edition', path: '/editions/41' },
  {
    name: 'edition-skeleton',
    path: '/editions/999',
    waitFor: 'load',
    prepare: async (page) => {
      await page.getByRole('status', { name: 'Загружаем издание' }).waitFor()
    },
  },
  { name: 'edition-404', path: '/editions/100000' },
]

const AUTH: (1280 | 960 | 390)[] = [960, 390]

async function fillLogin(page: Page, email: string, password: string) {
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Пароль', { exact: true }).fill(password)
  await page.getByRole('button', { name: /^Войти$/ }).click()
}

const authScreens: ScreenEntry[] = [
  { name: 'login', path: '/login', widths: AUTH },
  {
    name: 'login-error',
    path: '/login',
    widths: AUTH,
    prepare: async (page) => {
      await fillLogin(page, 'nurbo@example.com', 'wrongpass1')
      await page.getByRole('alert').first().waitFor()
    },
  },
  {
    name: 'login-rate-limited',
    path: '/login',
    widths: AUTH,
    prepare: async (page) => {
      await fillLogin(page, 'limit@example.com', 'anypass123')
      await page.getByRole('alert').first().waitFor()
    },
  },
  { name: 'register', path: '/register', widths: AUTH },
  {
    name: 'register-email-taken',
    path: '/register',
    widths: AUTH,
    prepare: async (page) => {
      await page.getByLabel('Имя').fill('Нурбо')
      await page.getByLabel('Email').fill('taken@example.com')
      await page.getByLabel('Пароль', { exact: true }).fill('password123')
      await page.getByRole('checkbox').check()
      await page.getByRole('button', { name: 'Создать аккаунт' }).click()
      await page.getByText('Этот email уже зарегистрирован').first().waitFor()
    },
  },
  { name: 'check-email', path: '/check-email?email=nurbo%40example.com', widths: AUTH },
  {
    name: 'verify-email',
    path: '/verify-email?token=valid',
    widths: AUTH,
    prepare: async (page) => {
      await page.getByRole('heading', { name: 'Почта подтверждена' }).waitFor()
    },
  },
  { name: 'verify-email-expired', path: '/verify-email?token=expired', widths: AUTH },
  { name: 'forgot-password', path: '/forgot-password', widths: AUTH },
  {
    name: 'forgot-password-sent',
    path: '/forgot-password',
    widths: AUTH,
    prepare: async (page) => {
      await page.getByLabel('Email').fill('nurbo@example.com')
      await page.getByRole('button', { name: 'Отправить ссылку' }).click()
      await page.getByRole('heading', { name: 'Проверьте почту' }).waitFor()
    },
  },
  {
    name: 'reset-password-mismatch',
    path: '/reset-password?token=valid',
    widths: AUTH,
    prepare: async (page) => {
      await page.getByRole('textbox', { name: 'Новый пароль' }).fill('password123')
      await page.getByRole('textbox', { name: 'Повторите пароль' }).fill('password456')
      await page.getByRole('button', { name: 'Сохранить пароль' }).click()
      await page.getByText('Пароли не совпадают').first().waitFor()
    },
  },
  {
    name: 'reset-password-expired',
    path: '/reset-password?token=expired',
    widths: AUTH,
    prepare: async (page) => {
      await page.getByRole('textbox', { name: 'Новый пароль' }).fill('password123')
      await page.getByRole('textbox', { name: 'Повторите пароль' }).fill('password123')
      await page.getByRole('button', { name: 'Сохранить пароль' }).click()
      await page.getByRole('heading', { name: 'Ссылка устарела' }).waitFor()
    },
  },
  { name: 'profile', path: '/profile', widths: [1280, 390], signedIn: true },
  {
    name: 'profile-delete',
    path: '/profile',
    widths: [1280, 390],
    signedIn: true,
    prepare: async (page) => {
      await page.getByRole('button', { name: /Удалить аккаунт/ }).click()
      await page.getByRole('dialog').waitFor()
    },
  },
]

screens.push(...authScreens)
