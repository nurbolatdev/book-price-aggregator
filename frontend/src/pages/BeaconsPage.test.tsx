import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../app/App'
import { Toaster } from '../components/ui/Toaster'
import { MOCK_ACCESS_TOKEN, setMockSignedIn } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { getLocation, renderWithProviders } from '../test/render'

const user = {
  id: 7,
  email: 'nurbo@example.com',
  name: 'Нурболат',
  role: 'USER' as const,
  emailVerified: false,
  notifyFrequency: 'INSTANT' as const,
  weeklyDigest: true,
}

function signIn() {
  setMockSignedIn(true)
  useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user })
}

function renderPage(route = '/beacons') {
  signIn()
  return renderWithProviders(
    <>
      <App />
      <Toaster />
    </>,
    { route },
  )
}

async function rows() {
  const panel = await screen.findByRole('tabpanel')
  return within(panel).getAllByRole('article')
}

function rowOf(title: string) {
  const link = screen.getByRole('link', { name: title })
  return link.closest('article') as HTMLElement
}

describe('BeaconsPage', () => {
  it('shows the heading, tab counts and the triggered card first', async () => {
    renderPage()
    expect(await screen.findByRole('heading', { level: 1, name: 'Мои маячки' })).toBeInTheDocument()
    expect(await screen.findByRole('tab', { name: 'Все · 5' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Сработали · 1' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Ждут · 3' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'На паузе · 1' })).toBeInTheDocument()
    const first = (await rows())[0]!
    expect(within(first).getByRole('link', { name: /Купить за 3\s150/ })).toBeInTheDocument()
    expect(first).toHaveTextContent(/Сработал (сегодня|вчера) в \d\d:\d\d/)
  })

  it('shows goals and captions of every condition type', async () => {
    renderPage()
    await rows()
    expect(rowOf('Атомные привычки')).toHaveTextContent('Цель: любое снижение')
    expect(rowOf('Чистый код')).toHaveTextContent('Цель: ниже 8 000')
    expect(rowOf('Абай жолы')).toHaveTextContent('Цель: скидка от 15%')
    expect(rowOf('Сто лет одиночества')).toHaveTextContent(/пауза с /)
  })

  it('filters by tab and keeps it in the URL', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('tab', { name: 'Ждут · 3' }))
    await waitFor(() => expect(getLocation()).toBe('/beacons?tab=active'))
    expect(await rows()).toHaveLength(3)
    expect(screen.getByRole('tab', { name: 'Ждут · 3' })).toHaveAttribute('aria-selected', 'true')
  })

  it('reads the tab from the URL and falls back to all for an unknown one', async () => {
    const view = renderPage('/beacons?tab=paused')
    expect(await rows()).toHaveLength(1)
    view.unmount()
    renderPage('/beacons?tab=nope')
    expect(await rows()).toHaveLength(5)
  })

  it('moves between tabs with the arrow keys', async () => {
    renderPage()
    const all = await screen.findByRole('tab', { name: 'Все · 5' })
    all.focus()
    fireEvent.keyDown(all, { key: 'ArrowRight' })
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Сработали · 1' })).toHaveAttribute('aria-selected', 'true'))
  })

  it('shows the empty state without tabs', async () => {
    server.use(http.get('*/api/v1/beacons', () => HttpResponse.json([])))
    renderPage()
    expect(await screen.findByText('Маячков пока нет')).toBeInTheDocument()
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Найти книгу' })).toHaveAttribute('href', '/catalog')
  })

  it('shows a loading status with skeleton rows', async () => {
    let release: () => void = () => {}
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    server.use(
      http.get('*/api/v1/beacons', async () => {
        await gate
        return HttpResponse.json([])
      }),
    )
    renderPage()
    const status = await screen.findByRole('status', { name: 'Загружаем маячки' })
    expect(status.children).toHaveLength(3)
    release()
    expect(await screen.findByText('Маячков пока нет')).toBeInTheDocument()
  })

  it('shows an error and retries', async () => {
    let calls = 0
    server.use(
      http.get('*/api/v1/beacons', () => {
        calls += 1
        if (calls === 1) return HttpResponse.json({ code: 'INTERNAL_ERROR', message: 'x' }, { status: 500 })
        return HttpResponse.json([])
      }),
    )
    renderPage()
    expect(await screen.findByText('Не удалось загрузить маячки.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    expect(await screen.findByText('Маячков пока нет')).toBeInTheDocument()
  })

  it('pauses a beacon before the answer and sends the status', async () => {
    const bodies: unknown[] = []
    let release: () => void = () => {}
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    server.use(
      http.patch('*/api/v1/beacons/:id', async ({ request }) => {
        bodies.push(await request.json())
        await gate
        return HttpResponse.json({ id: 2, editionId: 7, title: 'Атомные привычки', status: 'PAUSED' })
      }),
    )
    renderPage()
    await rows()
    fireEvent.click(within(rowOf('Атомные привычки')).getByRole('button', { name: 'Поставить на паузу' }))
    expect(await screen.findByRole('tab', { name: 'На паузе · 2' })).toBeInTheDocument()
    expect(within(rowOf('Атомные привычки')).getByText('На паузе')).toBeInTheDocument()
    release()
    await waitFor(() => expect(bodies).toEqual([{ status: 'PAUSED' }]))
  })

  it('rolls a failed pause back with a toast', async () => {
    server.use(
      http.patch('*/api/v1/beacons/:id', () => HttpResponse.json({ code: 'INTERNAL_ERROR', message: 'x' }, { status: 500 })),
    )
    renderPage()
    await rows()
    fireEvent.click(within(rowOf('Атомные привычки')).getByRole('button', { name: 'Поставить на паузу' }))
    await waitFor(() =>
      expect(useToastStore.getState().toasts.map((t) => t.message)).toContain(
        'Не удалось сохранить. Изменения отменены.',
      ),
    )
    expect(within(rowOf('Атомные привычки')).getByText('Ждёт')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'На паузе · 1' })).toBeInTheDocument()
  })

  it('resumes a paused beacon', async () => {
    const bodies: unknown[] = []
    server.use(
      http.patch('*/api/v1/beacons/:id', async ({ request }) => {
        bodies.push(await request.json())
        return HttpResponse.json({ id: 5, editionId: 61, title: 'Сто лет одиночества', status: 'ACTIVE' })
      }),
    )
    renderPage()
    await rows()
    fireEvent.click(within(rowOf('Сто лет одиночества')).getByRole('button', { name: 'Возобновить' }))
    await waitFor(() => expect(bodies).toEqual([{ status: 'ACTIVE' }]))
  })

  it('follows on from a triggered beacon and turns it into a regular row', async () => {
    const bodies: unknown[] = []
    server.events.on('request:start', async ({ request }) => {
      if (request.method === 'PATCH') bodies.push(await request.clone().json())
    })
    renderPage()
    await rows()
    fireEvent.click(screen.getByRole('button', { name: 'Следить дальше' }))
    expect(await screen.findByRole('tab', { name: 'Сработали · 0' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Следить дальше' })).not.toBeInTheDocument()
    await waitFor(() => expect(bodies).toEqual([{ status: 'ACTIVE' }]))
    server.events.removeAllListeners()
  })

  it('shows the notification panels after the list and saves the frequency', async () => {
    const bodies: unknown[] = []
    server.use(
      http.patch('*/api/v1/me', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        bodies.push(body)
        return HttpResponse.json({ ...user, ...body })
      }),
    )
    renderPage()
    const panel = await screen.findByRole('region', { name: 'Уведомления' })
    expect(within(panel).getByText('nurbo@example.com')).toBeInTheDocument()
    expect(within(panel).getByText('не подтверждён')).toBeInTheDocument()
    const list = await screen.findByRole('tabpanel')
    expect(list.compareDocumentPosition(panel) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    fireEvent.click(within(panel).getByRole('radio', { name: 'Сводка раз в день' }))
    await waitFor(() => expect(bodies).toEqual([{ notifyFrequency: 'DAILY' }]))
    await waitFor(() => expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Сохранено'))
    expect(screen.getByRole('region', { name: 'Как работает маячок' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Все настройки аккаунта' })).toHaveAttribute('href', '/profile')
  })

  it('sends a guest to the login page', async () => {
    renderWithProviders(<App />, { route: '/beacons' })
    await waitFor(() => expect(getLocation()).toBe('/login?next=%2Fbeacons'))
  })
})
