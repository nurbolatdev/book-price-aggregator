import { test } from '@playwright/test'
import { screens } from './screens.config'

const SIZES = {
  960: { width: 960, height: 800 },
  1280: { width: 1280, height: 900 },
  390: { width: 390, height: 844 },
} as const

for (const entry of screens) {
  for (const width of entry.widths ?? [1280, 390]) {
    test(`${entry.name} @ ${width}`, async ({ page }) => {
      await page.setViewportSize(SIZES[width])
      if (entry.signedIn) {
        await page.addInitScript(() => sessionStorage.setItem('kr.mock.session', '1'))
      }
      await page.goto(entry.path)
      await page.waitForLoadState(entry.waitFor ?? 'networkidle')
      await page.evaluate(() => document.fonts.ready)
      if (entry.prepare) await entry.prepare(page)
      await page.screenshot({
        path: `screens/${entry.name}-${width}.png`,
        fullPage: true,
        animations: 'disabled',
      })
    })
  }
}
