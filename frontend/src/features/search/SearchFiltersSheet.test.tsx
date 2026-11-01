import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { searchPage } from '../../mocks/fixtures'
import { server } from '../../mocks/server'
import { getLocation, renderWithProviders } from '../../test/render'
import SearchPage from '../../pages/SearchPage'

const ROUTE = '/search?q=' + encodeURIComponent('Мастер и Маргарита')

function useDraftHandler() {
  server.use(
    http.get('*/api/v1/search', ({ request }) => {
      const url = new URL(request.url)
      if (url.searchParams.get('stores') === 'FLIP,OZON') {
        return HttpResponse.json({ ...searchPage(0), totalElements: 9 })
      }
      return HttpResponse.json(searchPage(Number(url.searchParams.get('page') ?? 0)))
    }),
  )
}

async function openSheet() {
  fireEvent.click(await screen.findByRole('button', { name: /^Фильтры/ }))
  return screen.findByRole('dialog', { name: 'Фильтры' })
}

describe('SearchFiltersSheet', () => {
  it('applies the draft only on the apply button', async () => {
    useDraftHandler()
    renderWithProviders(<SearchPage />, { route: ROUTE })
    const dialog = await openSheet()

    const meloman = await within(dialog).findByRole('checkbox', { name: /Meloman/ })
    fireEvent.click(meloman)
    expect(getLocation()).toBe(ROUTE)

    const apply = await within(dialog).findByRole('button', { name: 'Показать 9 изданий' })
    fireEvent.click(apply)

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(new URLSearchParams(getLocation().split('?')[1]).get('stores')).toBe('FLIP,OZON')
  })

  it('resets only the draft', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE + '&stores=FLIP,OZON&inStock=false' })
    const dialog = await openSheet()
    const meloman = await within(dialog).findByRole('checkbox', { name: /Meloman/ })
    expect(meloman).not.toBeChecked()

    fireEvent.click(within(dialog).getByRole('button', { name: 'Сбросить' }))

    await waitFor(() => expect(within(dialog).getByRole('checkbox', { name: /Meloman/ })).toBeChecked())
    expect(within(dialog).getByRole('checkbox', { name: 'Только в наличии' })).toBeChecked()
    expect(getLocation()).toContain('stores=FLIP,OZON')
    expect(getLocation()).toContain('inStock=false')
  })

  it('closes on Escape without applying', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE })
    const dialog = await openSheet()
    fireEvent.click(await within(dialog).findByRole('checkbox', { name: /Meloman/ }))
    fireEvent.keyDown(document, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(getLocation()).not.toContain('stores=')
  })

  it('shows the number of active filters on the toolbar button', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE + '&stores=FLIP,OZON' })
    expect(await screen.findByRole('button', { name: 'Фильтры · 1' })).toBeInTheDocument()
  })
})
