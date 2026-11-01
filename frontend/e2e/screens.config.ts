import type { Page } from '@playwright/test'

export interface ScreenEntry {
  name: string
  path: string
  widths?: (1280 | 390)[]
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
]
