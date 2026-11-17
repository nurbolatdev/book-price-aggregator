import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../app/App'
import { Toaster } from '../components/ui/Toaster'
import { MOCK_ACCESS_TOKEN, setMockSignedIn } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { pendingDeletes } from '../features/beacons/useBeacons'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { getLocation, renderWithProviders } from '../test/render'
import { setViewport } from '../test/viewport'
import { mockBeacons, setMockBeacons } from '../mocks/beaconFixtures'

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

  describe('delete and edit', () => {
    beforeEach(() => {
      pendingDeletes.clear()
      useToastStore.setState({ toasts: [] })
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    function trackDeletes(status = 204) {
      const deletes: number[] = []
      server.use(
        http.delete('*/api/v1/beacons/:id', ({ params }) => {
          deletes.push(Number(params.id))
          if (status === 204 || status === 404) setMockBeacons(mockBeacons.filter((b) => b.id !== Number(params.id)))
          if (status === 204) return new HttpResponse(null, { status })
          const code = status === 404 ? 'NOT_FOUND' : 'INTERNAL_ERROR'
          return HttpResponse.json({ code, message: 'x' }, { status })
        }),
      )
      return deletes
    }

    async function removeCleanCode() {
      renderPage()
      await rows()
      fireEvent.click(within(rowOf('Чистый код')).getByRole('button', { name: 'Удалить маячок' }))
    }

    const titles = () => screen.getAllByRole('article').map((a) => within(a).getAllByRole('link')[0]!.textContent)

    it('removes the row at once and brings it back on undo without a request', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const deletes = trackDeletes()
      renderPage()
      await rows()
      const before = titles()
      fireEvent.click(within(rowOf('Чистый код')).getByRole('button', { name: 'Удалить маячок' }))
      await waitFor(() => expect(screen.queryByRole('link', { name: 'Чистый код' })).not.toBeInTheDocument())
      expect(screen.getByRole('tab', { name: 'Все · 4' })).toBeInTheDocument()
      expect(screen.getByText('Маячок удалён')).toBeInTheDocument()
      fireEvent.click(screen.getByRole('button', { name: 'Отменить' }))
      await waitFor(() => expect(titles()).toEqual(before))
      await act(async () => {
        await vi.advanceTimersByTimeAsync(6000)
      })
      expect(deletes).toEqual([])
      expect(screen.getByRole('tab', { name: 'Все · 5' })).toBeInTheDocument()
    })

    it('sends one DELETE after the toast times out', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const deletes = trackDeletes()
      await removeCleanCode()
      expect(deletes).toEqual([])
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5100)
      })
      await waitFor(() => expect(deletes).toEqual([3]))
      expect(screen.queryByRole('link', { name: 'Чистый код' })).not.toBeInTheDocument()
    })

    it('sends the DELETE when the toast is closed', async () => {
      const deletes = trackDeletes()
      await removeCleanCode()
      fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }))
      await waitFor(() => expect(deletes).toEqual([3]))
    })

    it('sends the DELETE at once when leaving the page', async () => {
      const deletes = trackDeletes()
      await removeCleanCode()
      fireEvent.click(within(screen.getByRole('banner')).getByRole('link', { name: 'Каталог' }))
      await waitFor(() => expect(deletes).toEqual([3]))
      expect(screen.queryByText('Маячок удалён')).not.toBeInTheDocument()
    })

    it('brings the row back on undo after another row changed and the list refetched', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const deletes = trackDeletes()
      await removeCleanCode()
      await waitFor(() => expect(screen.queryByRole('link', { name: 'Чистый код' })).not.toBeInTheDocument())
      fireEvent.click(within(rowOf('Атомные привычки')).getByRole('button', { name: 'Поставить на паузу' }))
      expect(await screen.findByRole('tab', { name: 'На паузе · 2' })).toBeInTheDocument()
      await act(async () => {
        await vi.advanceTimersByTimeAsync(500)
      })
      fireEvent.click(screen.getByRole('button', { name: 'Отменить' }))
      expect(await screen.findByRole('link', { name: 'Чистый код' })).toBeInTheDocument()
      expect(deletes).toEqual([])
    })

    it('keeps a second pause on screen when the first one fails', async () => {
      let release: () => void = () => {}
      const gate = new Promise<void>((resolve) => {
        release = resolve
      })
      server.use(
        http.patch('*/api/v1/beacons/:id', async ({ params }) => {
          if (Number(params.id) !== 2) return undefined
          await gate
          return HttpResponse.json({ code: 'INTERNAL_ERROR', message: 'x' }, { status: 500 })
        }),
      )
      renderPage()
      await rows()
      fireEvent.click(within(rowOf('Атомные привычки')).getByRole('button', { name: 'Поставить на паузу' }))
      fireEvent.click(within(rowOf('Абай жолы')).getByRole('button', { name: 'Поставить на паузу' }))
      expect(await screen.findByRole('tab', { name: 'На паузе · 3' })).toBeInTheDocument()
      release()
      await waitFor(() =>
        expect(useToastStore.getState().toasts.map((t) => t.message)).toContain(
          'Не удалось сохранить. Изменения отменены.',
        ),
      )
      expect(within(rowOf('Атомные привычки')).getByText('Ждёт')).toBeInTheDocument()
      expect(within(rowOf('Абай жолы')).getByText('На паузе')).toBeInTheDocument()
    })

    it('treats a 404 on DELETE as done', async () => {
      const deletes = trackDeletes(404)
      await removeCleanCode()
      fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }))
      await waitFor(() => expect(deletes).toEqual([3]))
      await waitFor(() => expect(pendingDeletes.size).toBe(0))
      expect(screen.queryByText('Не удалось удалить маячок')).not.toBeInTheDocument()
      expect(screen.queryByRole('link', { name: 'Чистый код' })).not.toBeInTheDocument()
    })

    it('puts the row back and warns when the DELETE fails', async () => {
      const deletes = trackDeletes(500)
      await removeCleanCode()
      fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }))
      await waitFor(() => expect(deletes).toEqual([3]))
      expect(await screen.findByText('Не удалось удалить маячок')).toBeInTheDocument()
      expect(await screen.findByRole('link', { name: 'Чистый код' })).toBeInTheDocument()
    })

    it('edits the condition before the answer and sends the whole condition', async () => {
      const bodies: unknown[] = []
      let release: () => void = () => {}
      const gate = new Promise<void>((resolve) => {
        release = resolve
      })
      server.use(
        http.patch('*/api/v1/beacons/:id', async ({ request }) => {
          bodies.push(await request.json())
          await gate
          return HttpResponse.json({
            id: 3,
            editionId: 9,
            title: 'Чистый код',
            status: 'ACTIVE',
            conditionType: 'BELOW_PRICE',
            targetPrice: 7500,
          })
        }),
      )
      setViewport(1280)
      renderPage()
      await rows()
      fireEvent.click(within(rowOf('Чистый код')).getByRole('button', { name: 'Изменить условие' }))
      const dialog = await screen.findByRole('dialog', { name: 'Изменить маячок' })
      expect(within(dialog).getAllByText('Чистый код').length).toBeGreaterThan(0)
      expect(within(dialog).getByText('Роберт Мартин')).toBeInTheDocument()
      expect(within(dialog).getByText(/Сейчас от/)).toHaveTextContent(/8\s900/)
      const price = within(dialog).getByRole('textbox', { name: 'Желаемая цена, ₸' })
      expect(price).toHaveValue('8000')
      fireEvent.change(price, { target: { value: '7500' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Сохранить' }))
      await waitFor(() => expect(rowOf('Чистый код')).toHaveTextContent(/Цель: ниже 7\s500/))
      release()
      await waitFor(() => expect(bodies).toEqual([{ conditionType: 'BELOW_PRICE', targetPrice: 7500 }]))
      await waitFor(() => expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Маячок обновлён'))
    })

    it('returns focus to the edit button after Escape', async () => {
      setViewport(1280)
      renderPage()
      await rows()
      const edit = within(rowOf('Чистый код')).getByRole('button', { name: 'Изменить условие' })
      edit.focus()
      fireEvent.click(edit)
      const dialog = await screen.findByRole('dialog', { name: 'Изменить маячок' })
      fireEvent.keyDown(dialog, { key: 'Escape' })
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(edit).toHaveFocus()
    })

    it('opens a bottom sheet on the phone', async () => {
      renderPage()
      await rows()
      fireEvent.click(within(rowOf('Чистый код')).getByRole('button', { name: 'Изменить условие' }))
      expect(await screen.findByRole('dialog', { name: 'Изменить маячок' })).toBeInTheDocument()
    })
  })

  it('sends a guest to the login page', async () => {
    renderWithProviders(<App />, { route: '/beacons' })
    await waitFor(() => expect(getLocation()).toBe('/login?next=%2Fbeacons'))
  })
})
