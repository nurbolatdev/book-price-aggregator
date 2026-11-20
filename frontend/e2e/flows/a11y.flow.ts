import { expect, test, type Page } from '@playwright/test'
import { expectNoSeriousA11yIssues } from './axe'
import { setMockSession } from './mockSession'

interface PageCase {
  name: string
  path: string
  signedIn?: boolean
}

const PAGES: PageCase[] = [
  { name: 'home', path: '/' },
  { name: 'search', path: '/search?q=Мастер и Маргарита' },
  { name: 'edition', path: '/editions/41' },
  { name: 'beacons', path: '/beacons', signedIn: true },
  { name: 'login', path: '/login' },
]

const SIZES = [
  { width: 1280, height: 900 },
  { width: 390, height: 844 },
]

// Controls that are allowed to be smaller than 44 px on phones, with the reason.
const TOUCH_EXCEPTIONS: { page: string; selector: string; reason: string }[] = [
  {
    page: 'beacons',
    selector: 'header button[aria-label="Меню пользователя"]',
    reason: 'phone header avatar is 36 px in mockup 08; the header row is 56 px high',
  },
]

async function openAndSettle(page: Page, entry: PageCase) {
  if (entry.signedIn) await setMockSession(page, { verified: true })
  await page.goto(entry.path)
  await page.waitForLoadState('networkidle')
  await page.evaluate(() => document.fonts.ready)
}

for (const size of SIZES) {
  for (const entry of PAGES) {
    test(`axe: ${entry.name} @ ${size.width}`, async ({ page }) => {
      await page.setViewportSize(size)
      await openAndSettle(page, entry)
      await expectNoSeriousA11yIssues(page, `${entry.name} @ ${size.width}`)
    })
  }
}

for (const entry of PAGES) {
  test(`touch targets: ${entry.name} @ 390`, async ({ page }) => {
    await page.setViewportSize(SIZES[1])
    await openAndSettle(page, entry)
    const small = await page.evaluate((exceptions: string[]) => {
      const nodes = document.querySelectorAll<HTMLElement>(
        'main button, header button, main select, header select, main input, header input',
      )
      const result: string[] = []
      nodes.forEach((el) => {
        if (el instanceof HTMLInputElement && ['checkbox', 'radio', 'hidden'].includes(el.type)) return
        if (exceptions.some((s) => el.matches(s))) return
        const box = el.getBoundingClientRect()
        if (box.width === 0 || box.height === 0) return
        if (getComputedStyle(el).visibility === 'hidden') return
        if (box.height < 43.5) {
          const label = el.getAttribute('aria-label') ?? el.textContent?.trim() ?? el.tagName
          result.push(`${el.tagName.toLowerCase()} "${label}" ${Math.round(box.height)}px`)
        }
      })
      return result
    }, TOUCH_EXCEPTIONS.filter((e) => e.page === entry.name).map((e) => e.selector))
    expect(small, `${entry.name}: controls under 44 px`).toEqual([])
  })
}

test('keyboard: skip link is the first Tab stop and moves focus to main', async ({ page }) => {
  await page.setViewportSize(SIZES[0])
  await page.goto('/')
  await page.waitForLoadState('networkidle')
  await page.keyboard.press('Tab')
  const skip = page.getByRole('link', { name: 'Перейти к содержимому' })
  await expect(skip).toBeFocused()
  const indicator = await skip.evaluate((el) => {
    const s = getComputedStyle(el)
    return s.outlineStyle !== 'none' || s.boxShadow !== 'none'
  })
  expect(indicator).toBe(true)
  await page.keyboard.press('Enter')
  await expect(page.locator('#main')).toBeFocused()
})

test('reduced motion: transitions are effectively disabled', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/')
  await page.waitForLoadState('networkidle')
  const durations = await page.evaluate(() =>
    Array.from(document.querySelectorAll('button')).slice(0, 5).map((b) => getComputedStyle(b).transitionDuration),
  )
  expect(durations.length).toBeGreaterThan(0)
  for (const d of durations) {
    for (const part of d.split(',')) {
      const value = parseFloat(part)
      const ms = part.trim().endsWith('ms') ? value : value * 1000
      expect(ms).toBeLessThanOrEqual(0.01)
    }
  }
})
