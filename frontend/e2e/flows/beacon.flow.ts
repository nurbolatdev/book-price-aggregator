import { expect, test } from '@playwright/test'
import { expectNoSeriousA11yIssues } from './axe'
import { watchPageErrors } from './guards'

test('beacon flow: search, filter, sign in, set, pause, delete and undo', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  // Signed out, but the mock account already has a verified email.
  // An empty beacon list: the row found by title below can only be the one this flow created.
  await page.addInitScript(() => {
    sessionStorage.setItem('kr.mock.emailVerified', '1')
    sessionStorage.setItem('kr.mock.beacons', 'empty')
  })
  await page.setViewportSize({ width: 1280, height: 900 })

  await page.goto('/')
  await page.getByRole('combobox', { name: 'Поиск книги' }).fill('Мастер и Маргарита')
  await page.getByRole('combobox', { name: 'Поиск книги' }).press('Enter')
  await expect(page).toHaveURL(/\/search\?q=/)
  await expect(page.getByRole('status').filter({ hasText: 'Найдено' })).toBeVisible()

  const filters = page.getByRole('complementary', { name: 'Фильтры' })
  await filters.getByRole('checkbox', { name: /^Ozon/ }).click()
  await expect(page).toHaveURL(/stores=/)
  await expect(page.getByRole('status').filter({ hasText: 'Найдено' })).toBeVisible()

  await page.getByRole('article').first().getByRole('link').first().click()
  await expect(page).toHaveURL(/\/editions\/\d+/)
  const title = ((await page.getByRole('heading', { level: 1 }).textContent()) ?? '').trim()
  expect(title).not.toBe('')
  await page.getByRole('link', { name: 'Следить за ценой' }).first().click()
  await expect(page).toHaveURL(/\/login\?next=/)

  await page.getByLabel('Email').fill('nurbo@example.com')
  await page.getByRole('textbox', { name: 'Пароль' }).fill('password123')
  await page.getByRole('button', { name: 'Войти', exact: true }).click()

  await expect(page).toHaveURL(/\/editions\/\d+/)
  const dialog = page.getByRole('dialog', { name: 'Маячок на книгу' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: 'Поставить маячок' }).click()
  await expect(page.getByText('Маячок поставлен')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Маячок стоит · изменить' }).first()).toBeVisible()

  await page.getByRole('link', { name: 'Мои маячки' }).first().click()
  await expect(page).toHaveURL(/\/beacons/)
  const row = page.getByRole('article').filter({ hasText: title })
  await expect(row).toHaveCount(1)

  await row.getByRole('button', { name: 'Поставить на паузу' }).click()
  await expect(row.getByRole('button', { name: 'Возобновить' })).toBeVisible()
  await expect(row.getByText('На паузе')).toBeVisible()

  await row.getByRole('button', { name: 'Удалить маячок' }).click()
  await expect(page.getByText('Маячок удалён')).toBeVisible()
  await expect(row).toHaveCount(0)
  await page.getByRole('button', { name: 'Отменить' }).click()
  await expect(row).toHaveCount(1)
  await expect(row.getByRole('button', { name: 'Возобновить' })).toBeVisible()

  await expectNoSeriousA11yIssues(page, 'beacons after flow')
  assertNoErrors()
})
