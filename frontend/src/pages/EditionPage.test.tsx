import { QueryClient } from '@tanstack/react-query'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { queryKeys } from '../api/queryKeys'
import { formatPrice } from '../lib/format'
import { editionDetail } from '../mocks/editionFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { getLocation, renderWithProviders } from '../test/render'
import { resetViewport, setViewport } from '../test/viewport'
import EditionPage from './EditionPage'

function renderEdition(route: string | { pathname: string; state?: unknown } = '/editions/41', queryClient?: QueryClient) {
  return renderWithProviders(<EditionPage />, {
    route,
    path: '/editions/:id',
    queryClient,
  })
}

describe('EditionPage (desktop)', () => {
  beforeEach(() => {
    setViewport(1280)
    useAuthStore.setState({
      status: 'anonymous',
      user: null,
      accessToken: null,
    })
  })

  it('shows the title, author and highlights the best row', async () => {
    renderEdition()
    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Мастер и Маргарита',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Михаил Булгаков')).toBeInTheDocument()
    const rows = within(screen.getByRole('table')).getAllByRole('row').slice(1)
    expect(rows).toHaveLength(3)
    expect(rows[0]).toHaveAttribute('data-best', 'true')
    expect(within(rows[0]!).getByText('Flip.kz')).toBeInTheDocument()
    expect(within(rows[2]!).getByText('Нет в наличии')).toBeInTheDocument()
  })

  it('opens every store link in a new tab safely', async () => {
    renderEdition()
    await screen.findByRole('table')
    const links = screen.getAllByRole('link', { name: /В магазин|Перейти в/ })
    expect(links.length).toBeGreaterThanOrEqual(4)
    for (const link of links) {
      expect(link).toHaveAttribute('target', '_blank')
      expect(link).toHaveAttribute('rel', 'noopener noreferrer')
    }
  })

  it('sets the document title and description', async () => {
    renderEdition()
    await screen.findByRole('heading', { level: 1 })
    expect(document.title).toBe('Мастер и Маргарита — цены в магазинах Казахстана | KitapRadar')
    const description = document.querySelector('meta[name="description"]')?.getAttribute('content')
    expect(description).toContain(`от ${formatPrice(3150)}`)
  })

  it('shows the loading status', () => {
    renderEdition()
    expect(screen.getByRole('status', { name: 'Загружаем издание' })).toBeInTheDocument()
  })

  it('renders instantly from a prefetched edition', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: Infinity } },
    })
    queryClient.setQueryData(queryKeys.edition(41), editionDetail(41))
    renderEdition('/editions/41', queryClient)
    expect(
      screen.getByRole('heading', { level: 1, name: 'Мастер и Маргарита' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('status', { name: 'Загружаем издание' })).not.toBeInTheDocument()
  })

  it('shows the 404 screen for an unknown edition', async () => {
    renderEdition('/editions/100000')
    expect(await screen.findByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
  })

  it('shows the 404 screen for an invalid id without a request', () => {
    let requests = 0
    server.use(
      http.get('*/api/v1/editions/:id', () => {
        requests += 1
        return HttpResponse.json({}, { status: 500 })
      }),
    )
    renderEdition('/editions/abc')
    expect(screen.getByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('offers a retry after a server error', async () => {
    let fail = true
    server.use(
      http.get('*/api/v1/editions/:id', ({ params }) => {
        if (fail) return HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status: 500 })
        return HttpResponse.json(editionDetail(Number(params.id)))
      }),
    )
    renderEdition()
    expect(await screen.findByText('Не удалось загрузить издание.')).toBeInTheDocument()
    fail = false
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Мастер и Маргарита',
      }),
    ).toBeInTheDocument()
  })

  it('sends a guest from ?beacon=1 to login', async () => {
    renderEdition('/editions/41?beacon=1')
    await waitFor(() => expect(getLocation()).toBe('/login?next=%2Feditions%2F41%3Fbeacon%3D1'))
  })

  it('removes ?beacon=1 for a signed-in user', async () => {
    useAuthStore.setState({ status: 'authenticated' })
    renderEdition('/editions/41?beacon=1')
    await waitFor(() => expect(getLocation()).toBe('/editions/41'))
    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument()
  })

  it('shows breadcrumbs with the search origin', async () => {
    const from = '/search?q=' + encodeURIComponent('Мастер и Маргарита')
    renderEdition({ pathname: '/editions/41', state: { from } })
    const nav = await screen.findByRole('navigation', { name: 'Хлебные крошки' })
    const links = within(nav).getAllByRole('link')
    expect(links.map((l) => l.textContent)).toEqual(['Главная', 'Поиск «Мастер и Маргарита»'])
    expect(links[1]).toHaveAttribute('href', from)
    expect(within(nav).getByText('Мастер и Маргарита')).toHaveAttribute('aria-current', 'page')
  })

  it('shows short breadcrumbs without an origin', async () => {
    renderEdition()
    const nav = await screen.findByRole('navigation', { name: 'Хлебные крошки' })
    expect(within(nav).getAllByRole('link').map((l) => l.textContent)).toEqual(['Главная'])
    expect(within(nav).getByText('Мастер и Маргарита')).toHaveAttribute('aria-current', 'page')
  })

  it('shows the facts', async () => {
    renderEdition()
    expect(await screen.findByText('978-5-17-090630-4')).toBeInTheDocument()
    expect(screen.getByText('480')).toBeInTheDocument()
    expect(screen.getByText('Русский')).toBeInTheDocument()
  })

  it('omits facts that are missing', async () => {
    server.use(
      http.get('*/api/v1/editions/:id', ({ params }) => {
        const detail = editionDetail(Number(params.id))
        return HttpResponse.json({ ...detail, pages: undefined, isbn13: undefined })
      }),
    )
    renderEdition()
    await screen.findByText('Русский')
    expect(screen.queryByText('Страниц')).not.toBeInTheDocument()
    expect(screen.queryByText('ISBN')).not.toBeInTheDocument()
  })

  it('switches editions in the same render tree', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } })
    renderWithProviders(
      <>
        <span data-testid="probe" />
        <EditionPage />
      </>,
      {
        route: { pathname: '/editions/41', state: { from: '/search?q=bulgakov' } },
        path: '/editions/:id',
        queryClient,
      },
    )
    const probe = screen.getByTestId('probe')
    const current = await screen.findByRole('button', { name: 'АСТ 2023 · твёрдый' })
    expect(current).toHaveAttribute('aria-pressed', 'true')
    const other = screen.getByRole('button', { name: 'Азбука 2022 · мягкий' })
    expect(other).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(other)
    await waitFor(() => expect(getLocation()).toBe('/editions/16'))
    expect(await screen.findByRole('button', { name: 'Азбука 2022 · мягкий' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(editionDetail(16)?.book?.title ?? '')
    expect(screen.getByTestId('probe')).toBe(probe)
    const nav = screen.getByRole('navigation', { name: 'Хлебные крошки' })
    expect(within(nav).getByRole('link', { name: /Поиск/ })).toBeInTheDocument()
  })

  it('prefetches another edition on hover', async () => {
    let requests = 0
    server.use(
      http.get('*/api/v1/editions/:id', ({ params }) => {
        if (Number(params.id) === 16) requests += 1
        const detail = editionDetail(Number(params.id))
        return detail ? HttpResponse.json(detail) : new HttpResponse(null, { status: 404 })
      }),
    )
    renderEdition()
    const other = await screen.findByRole('button', { name: 'Азбука 2022 · мягкий' })
    fireEvent.mouseEnter(other)
    fireEvent.mouseEnter(other)
    await waitFor(() => expect(requests).toBe(1))
    fireEvent.click(other)
    await screen.findByRole('heading', { level: 1 })
    expect(requests).toBe(1)
  })
})

describe('EditionPage (phone)', () => {
  beforeEach(() => {
    resetViewport()
    useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
  })

  it('shows price cards instead of a table', async () => {
    renderEdition()
    expect(await screen.findByRole('heading', { level: 1, name: 'Мастер и Маргарита' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    const section = screen.getByRole('region', { name: /Цены в 3 магазинах/ })
    const cards = within(section).getAllByRole('link')
    expect(cards.map((c) => within(c).getByRole('strong').textContent)).toEqual(['Flip.kz', 'Ozon', 'Meloman'])
    expect(cards[0]).toHaveClass('border-2', 'border-deal', 'bg-best-row')
    expect(cards[1]).not.toHaveClass('border-deal')
    expect(cards[0]).toHaveTextContent('1–2 дня, Алматы · обновлено 2 ч назад')
    expect(cards[2]).toHaveTextContent('Самовывоз · нет в наличии')
    for (const card of cards) {
      expect(card).toHaveAttribute('target', '_blank')
      expect(card).toHaveAttribute('rel', 'noopener noreferrer')
    }
  })

  it('shows the compact summary and the details section', async () => {
    renderEdition()
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByText('АСТ 2023 · твёрдый', { selector: 'p' })).toBeInTheDocument()
    expect(screen.getByText('Ниже средней на 12%')).toBeInTheDocument()
    const details = screen.getByRole('region', { name: 'Подробности' })
    expect(within(details).getByRole('button', { name: 'Азбука 2022 · мягкий' })).toBeInTheDocument()
    expect(within(details).getByText('978-5-17-090630-4')).toBeInTheDocument()
  })

  it('has its own header and no site header', async () => {
    renderEdition()
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('button', { name: 'Назад' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Поделиться' })).toBeInTheDocument()
  })

  it('keeps the back button on the 404 screen', async () => {
    renderEdition('/editions/100000')
    expect(await screen.findByText('Такой страницы нет')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Назад' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Поделиться' })).not.toBeInTheDocument()
  })

  it('shows the phone skeleton while loading', () => {
    renderEdition()
    expect(screen.getByRole('status', { name: 'Загружаем издание' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Назад' })).toBeInTheDocument()
  })
})
