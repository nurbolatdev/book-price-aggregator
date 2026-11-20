import type { Page } from '@playwright/test'

export interface MockSessionOptions {
  verified?: boolean
  admin?: boolean
  beacons?: 'empty'
}

export async function setMockSession(page: Page, options: MockSessionOptions = {}) {
  await page.addInitScript((opts: MockSessionOptions) => {
    sessionStorage.setItem('kr.mock.session', '1')
    if (opts.verified) sessionStorage.setItem('kr.mock.emailVerified', '1')
    if (opts.admin) sessionStorage.setItem('kr.mock.role', 'ADMIN')
    if (opts.beacons) sessionStorage.setItem('kr.mock.beacons', opts.beacons)
  }, options)
}
