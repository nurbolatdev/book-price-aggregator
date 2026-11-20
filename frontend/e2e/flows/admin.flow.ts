import { expect, test, type Page } from '@playwright/test'
import { watchPageErrors } from './guards'
import { setMockSession } from './mockSession'

const meloman = (page: Page) => page.getByRole('switch', { name: 'Магазин Meloman' })

test('admin turns a store off after confirming', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await setMockSession(page, { admin: true, verified: true })
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/admin/parsers')

  await expect(meloman(page)).toHaveAttribute('aria-checked', 'true')
  await meloman(page).click()
  const dialog = page.getByRole('dialog', { name: 'Выключить Meloman?' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: 'Выключить' }).click()

  await expect(page.getByText('Магазин Meloman выключен')).toBeVisible()
  await expect(meloman(page)).toHaveAttribute('aria-checked', 'false')
  assertNoErrors()
})

test('cancelling the dialog keeps the store on', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await setMockSession(page, { admin: true, verified: true })
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/admin/parsers')

  await meloman(page).click()
  const dialog = page.getByRole('dialog', { name: 'Выключить Meloman?' })
  await dialog.getByRole('button', { name: 'Отмена' }).click()

  await expect(dialog).toBeHidden()
  await expect(meloman(page)).toHaveAttribute('aria-checked', 'true')
  assertNoErrors()
})

async function setVisibility(page: Page, state: 'visible' | 'hidden') {
  await page.evaluate((value) => {
    Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => value })
    Object.defineProperty(document, 'hidden', { configurable: true, get: () => value === 'hidden' })
    document.dispatchEvent(new Event('visibilitychange', { bubbles: true }))
  }, state)
}

test('admin polling pauses in a hidden tab and resumes at once', async ({ page }) => {
  const assertNoErrors = watchPageErrors(page)
  await setMockSession(page, { admin: true, verified: true })
  await page.setViewportSize({ width: 1280, height: 900 })
  let statsRequests = 0
  page.on('request', (request) => {
    if (request.url().includes('/api/v1/admin/stats')) statsRequests += 1
  })

  await page.clock.install()
  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Обзор системы' })).toBeVisible()
  await expect.poll(() => statsRequests).toBeGreaterThan(0)

  const afterLoad = statsRequests
  await page.clock.runFor(31_000)
  await expect.poll(() => statsRequests).toBeGreaterThan(afterLoad)

  await setVisibility(page, 'hidden')
  const beforeHidden = statsRequests
  await page.clock.runFor(65_000)
  await page.waitForTimeout(300)
  expect(statsRequests).toBe(beforeHidden)

  await setVisibility(page, 'visible')
  await expect.poll(() => statsRequests).toBeGreaterThan(beforeHidden)
  assertNoErrors()
})
