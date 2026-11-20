import { expect, test } from '@playwright/test'
import { watchPageErrors } from './guards'

test('register leads to the check-email page', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/register')

  await page.getByLabel('Имя').fill('Нурбо')
  await page.getByLabel('Email').fill('new.reader@example.com')
  await page.getByRole('textbox', { name: 'Пароль' }).fill('password123')
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: 'Создать аккаунт' }).click()

  await expect(page).toHaveURL(/\/check-email\?email=new\.reader%40example\.com/)
  await expect(page.getByRole('heading', { name: 'Подтвердите почту' })).toBeVisible()
  assertNoErrors()
})

test('forgot password shows the confirmation', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/forgot-password')

  await page.getByLabel('Email').fill('reader@example.com')
  await page.getByRole('button', { name: 'Отправить ссылку' }).click()

  await expect(page.getByRole('heading', { name: 'Проверьте почту' })).toBeVisible()
  assertNoErrors()
})

test('reset password with a valid token returns to login', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/reset-password?token=valid')

  await page.getByRole('textbox', { name: 'Новый пароль' }).fill('password456')
  await page.getByRole('textbox', { name: 'Повторите пароль' }).fill('password456')
  await page.getByRole('button', { name: 'Сохранить пароль' }).click()

  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByText('Пароль изменён, войдите')).toBeVisible()
  assertNoErrors()
})

test('reset password with an expired token shows the expired state', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/reset-password?token=expired')

  await page.getByRole('textbox', { name: 'Новый пароль' }).fill('password456')
  await page.getByRole('textbox', { name: 'Повторите пароль' }).fill('password456')
  await page.getByRole('button', { name: 'Сохранить пароль' }).click()

  await expect(page.getByRole('heading', { name: 'Ссылка устарела' })).toBeVisible()
  assertNoErrors()
})
