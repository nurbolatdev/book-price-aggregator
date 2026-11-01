import type { Page } from '@playwright/test'

export interface ScreenEntry {
  name: string
  path: string
  widths?: (1280 | 390)[]
  prepare?: (page: Page) => Promise<void>
}

export const screens: ScreenEntry[] = [
  { name: 'home', path: '/' },
  { name: 'not-found', path: '/nope' },
]
