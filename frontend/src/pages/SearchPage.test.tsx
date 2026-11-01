import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { emptySearchPage } from '../mocks/fixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { renderWithProviders } from '../test/render'
import SearchPage from './SearchPage'

const ROUTE = '/search?q=' + encodeURIComponent('Мастер и Маргарита')

describe('SearchPage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('renders heading, total and result cards', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE })
    expect(await screen.findByRole('heading', { level: 1, name: '«Мастер и Маргарита»' })).toBeInTheDocument()
    expect(await screen.findByText('Найдено 12 изданий')).toBeInTheDocument()
    const links = screen.getAllByRole('link', { name: 'Мастер и Маргарита' })
    expect(links[0]).toHaveAttribute('href', '/editions/12')
    expect(screen.getAllByRole('article')).toHaveLength(3)
    const best = document.querySelector('[data-best]') as HTMLElement
    expect(within(best).getByText(/3\s150/)).toBeInTheDocument()
  })

  it('loads the next page on demand', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE })
    fireEvent.click(await screen.findByRole('button', { name: 'Показать ещё 9 изданий' }))
    await waitFor(() => expect(screen.getAllByRole('article')).toHaveLength(12))
    expect(screen.queryByRole('button', { name: /Показать ещё/ })).not.toBeInTheDocument()
  })

  it('restarts from page 0 with the new sort', async () => {
    const seen: string[] = []
    server.use(
      http.get('*/api/v1/search', ({ request }) => {
        seen.push(new URL(request.url).search)
        return HttpResponse.json(emptySearchPage)
      }),
    )
    renderWithProviders(<SearchPage />, { route: ROUTE })
    await screen.findByRole('combobox', { name: 'Сортировка' })
    fireEvent.change(screen.getByRole('combobox', { name: 'Сортировка' }), { target: { value: 'PRICE_ASC' } })
    await waitFor(() => expect(seen.some((s) => s.includes('sort=PRICE_ASC'))).toBe(true))
    const last = new URLSearchParams(seen[seen.length - 1])
    expect(last.get('page')).toBe('0')
    expect(screen.getByRole('combobox', { name: 'Сортировка' })).toHaveValue('PRICE_ASC')
  })

  it('restores the sort from the url', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE + '&sort=PRICE_ASC' })
    expect(await screen.findByRole('combobox', { name: 'Сортировка' })).toHaveValue('PRICE_ASC')
  })

  it('shows the empty state with filter reset and popular chips', async () => {
    server.use(http.get('*/api/v1/search', () => HttpResponse.json(emptySearchPage)))
    renderWithProviders(<SearchPage />, { route: ROUTE + '&stores=FLIP' })
    expect(await screen.findByText('По запросу «Мастер и Маргарита» ничего не нашли')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Сбросить фильтры' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Атомные привычки' })).toBeInTheDocument()
  })

  it('shows an error block and recovers on retry', async () => {
    let fail = true
    server.use(
      http.get('*/api/v1/search', ({ request }) => {
        if (fail) return HttpResponse.json({ code: 'INTERNAL', message: 'x' }, { status: 500 })
        const page = new URL(request.url).searchParams.get('page')
        return HttpResponse.json({ content: [], page: Number(page), size: 20, totalElements: 0, totalPages: 0 })
      }),
    )
    renderWithProviders(<SearchPage />, { route: ROUTE })
    expect(await screen.findByText('Не удалось загрузить результаты.')).toBeInTheDocument()
    fail = false
    server.resetHandlers()
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    expect(await screen.findAllByRole('article')).toHaveLength(3)
  })

  it('sends guests to login from the bell', async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE })
    const bells = await screen.findAllByRole('link', { name: 'Поставить маячок' })
    expect(bells[0]).toHaveAttribute('href', '/login?next=%2Feditions%2F12%3Fbeacon%3D1')
  })
})
