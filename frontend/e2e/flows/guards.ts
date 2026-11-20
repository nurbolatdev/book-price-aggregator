import { expect, type Page } from '@playwright/test'

export function watchPageErrors(page: Page) {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  page.on('console', (message) => {
    if (message.type() !== 'error') return
    const text = message.text()
    if (/Failed to load resource/.test(text)) return
    errors.push(text)
  })
  return () => expect(errors).toEqual([])
}
