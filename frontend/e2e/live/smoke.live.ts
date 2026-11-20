import { expect, test } from '@playwright/test'

const query = process.env.E2E_QUERY ?? 'Мастер и Маргарита'
const adminEmail = process.env.E2E_ADMIN_EMAIL ?? 'admin@example.com'
const adminPassword = process.env.E2E_ADMIN_PASSWORD ?? 'change_me_admin_password'

test('home, search, edition and admin sign-in on the real stack', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: 'Одна книга — все цены Казахстана' })).toBeVisible()

  await page.getByRole('combobox', { name: 'Поиск книги' }).fill(query)
  await page.getByRole('combobox', { name: 'Поиск книги' }).press('Enter')
  await expect(page).toHaveURL(/\/search\?q=/)
  await expect(page.getByRole('status').filter({ hasText: 'Найдено' })).toBeVisible()

  await page.getByRole('article').first().getByRole('link').first().click()
  await expect(page).toHaveURL(/\/editions\/\d+/)
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

  await page.goto('/login')
  await page.getByLabel('Email').fill(adminEmail)
  await page.getByRole('textbox', { name: 'Пароль' }).fill(adminPassword)
  await page.getByRole('button', { name: 'Войти', exact: true }).click()
  await page.goto('/admin')
  await expect(page.getByRole('heading', { level: 1, name: 'Обзор системы' })).toBeVisible()
})
