import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { server } from '../../mocks/server'
import { renderAdmin } from '../../test/admin'

afterEach(() => {
  server.events.removeAllListeners()
})

interface Logged {
  method: string
  url: URL
  body?: string
}

function logRequests(): Logged[] {
  const log: Logged[] = []
  server.events.on('request:start', async ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.endsWith('/search') || url.pathname.includes('/admin/offers/')) {
      log.push({ method: request.method, url, body: await request.clone().text() })
    }
  })
  return log
}

const offerButtons = () =>
  screen.getAllByRole('button', { pressed: undefined }).filter((b) => b.hasAttribute('aria-pressed'))

async function open() {
  renderAdmin('/admin/matching')
  await screen.findByRole('heading', { level: 2, name: 'Ждут проверки · 23' })
  await screen.findAllByRole('radio')
}

describe('MatchingPage', () => {
  it('shows the catalog summary, the queue and the selected offer', async () => {
    const log = logRequests()
    await open()
    expect(
      await screen.findByText('Книг 1 842 · изданий 2 917 · сопоставлено предложений 4 105'),
    ).toBeInTheDocument()
    const buttons = offerButtons()
    expect(buttons).toHaveLength(20)
    expect(buttons[0]).toHaveAttribute('aria-pressed', 'true')
    expect(
      screen.getByRole('heading', { level: 2, name: 'Булгаков М. Мастер и Маргарита / Михаил Булгаков (тв.)' }),
    ).toBeInTheDocument()
    expect(screen.getByText('ozon-1182934')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'открыть на сайте магазина' })).toBeInTheDocument()
    const field = screen.getByRole('searchbox', { name: 'Найти издание' })
    expect(field).toHaveValue('Булгаков М. Мастер и Маргарита / Михаил Булгаков (тв.)')
    expect(log.some((r) => r.url.searchParams.get('q') === field.getAttribute('value'))).toBe(true)
    expect(screen.getAllByRole('radio').length).toBeLessThanOrEqual(5)
    expect(screen.getByRole('button', { name: 'Привязать к изданию' })).toBeDisabled()
  })

  it('binds the chosen edition and moves to the next offer', async () => {
    const log = logRequests()
    await open()
    fireEvent.click(screen.getAllByRole('radio')[0]!)
    const bind = screen.getByRole('button', { name: 'Привязать к изданию' })
    expect(bind).toBeEnabled()
    fireEvent.click(bind)
    expect(await screen.findByText('Предложение привязано к изданию')).toBeInTheDocument()
    await screen.findByRole('heading', { level: 2, name: 'Ждут проверки · 22' })
    expect(
      await screen.findByRole('heading', {
        level: 2,
        name: 'Мастер и Маргарита. Роман (подарочное изд.)',
      }),
    ).toBeInTheDocument()
    expect(screen.queryByText('Булгаков М. Мастер и Маргарита / Михаил Булгаков (тв.)')).not.toBeInTheDocument()
    const post = log.find((r) => r.method === 'POST' && r.url.pathname.endsWith('/offers/501/match'))
    expect(post).toBeDefined()
    expect(JSON.parse(post!.body!)).toEqual({ editionId: expect.any(Number) })
    await waitFor(() =>
      expect(
        log.filter((r) => r.url.pathname.endsWith('/search') && r.url.searchParams.get('q')?.startsWith('Мастер и Маргарита. Роман')).length,
      ).toBeGreaterThan(0),
    )
  })

  it('rejects the offer and selects the next one', async () => {
    const log = logRequests()
    await open()
    fireEvent.click(screen.getByRole('button', { name: 'Отклонить: не книга / мусор' }))
    expect(await screen.findByText('Предложение отклонено')).toBeInTheDocument()
    expect(log.some((r) => r.method === 'POST' && r.url.pathname.endsWith('/offers/501/reject'))).toBe(true)
    await screen.findByRole('heading', { level: 2, name: 'Мастер и Маргарита. Роман (подарочное изд.)' })
  })

  it('searches editions again with the typed text', async () => {
    const log = logRequests()
    await open()
    const field = screen.getByRole('searchbox', { name: 'Найти издание' })
    fireEvent.change(field, { target: { value: 'чистый код' } })
    fireEvent.click(screen.getByRole('button', { name: 'Найти издание' }))
    await waitFor(() => expect(log.some((r) => r.url.searchParams.get('q') === 'чистый код')).toBe(true))
  })

  it('says so when no edition is found', async () => {
    await open()
    const field = screen.getByRole('searchbox', { name: 'Найти издание' })
    fireEvent.change(field, { target: { value: 'ничего такого' } })
    fireEvent.click(screen.getByRole('button', { name: 'Найти издание' }))
    expect(await screen.findByText('Изданий не нашли — уточните запрос')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Привязать к изданию' })).toBeDisabled()
  })

  it('selects another offer from the list', async () => {
    await open()
    const list = offerButtons()
    fireEvent.click(within(list[3]!.parentElement!).getByRole('button'))
    expect(
      await screen.findByRole('heading', {
        level: 2,
        name: 'Атомные привычки. Как приобрести хорошие привычки',
      }),
    ).toBeInTheDocument()
  })

  it('shows the empty state when everything is reviewed', async () => {
    server.use(
      http.get('*/api/v1/admin/offers/pending-review', () =>
        HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      ),
    )
    renderAdmin('/admin/matching')
    expect(await screen.findByText('Все предложения проверены')).toBeInTheDocument()
  })
})
