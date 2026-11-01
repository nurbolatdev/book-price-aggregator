import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { getLocation, renderWithProviders } from '../test/render'
import HomePage from './HomePage'

describe('HomePage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('links popular chips to search', async () => {
    renderWithProviders(<HomePage />)
    const chip = await screen.findByRole('link', { name: 'Атомные привычки' })
    expect(chip).toHaveAttribute('href', '/search?q=' + encodeURIComponent('Атомные привычки'))
  })

  it('lists searched and unavailable stores', async () => {
    renderWithProviders(<HomePage />)
    expect(await screen.findByText('Kaspi.kz, Wildberries — временно недоступны')).toBeInTheDocument()
    const line = screen.getByText('Ищем в магазинах:').parentElement as HTMLElement
    for (const name of ['Flip.kz', 'Meloman', 'Ozon']) {
      expect(within(line).getByText(name)).toBeInTheDocument()
    }
  })

  it('renders four drop cards and links to deals', async () => {
    renderWithProviders(<HomePage />)
    await waitFor(() => expect(screen.getAllByText(/за 7 дней/)).toHaveLength(4))
    expect(screen.getByRole('link', { name: 'Все скидки' })).toHaveAttribute('href', '/deals')
  })

  it('shows the sign-up block only for anonymous users', async () => {
    const { unmount } = renderWithProviders(<HomePage />)
    expect(screen.getByText('Есть список «хочу прочитать»?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Создать аккаунт' })).toHaveAttribute('href', '/register')
    unmount()
    useAuthStore.getState().setSession({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, name: 'Aidar', role: 'USER' },
    })
    renderWithProviders(<HomePage />)
    expect(screen.queryByText('Есть список «хочу прочитать»?')).toBeNull()
  })

  it('isolates a drops failure and retries', async () => {
    let fail = true
    server.use(
      http.get('*/api/v1/home/drops', () =>
        fail ? HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status: 500 }) : HttpResponse.json([]),
      ),
    )
    renderWithProviders(<HomePage />)
    expect(await screen.findByText('Не удалось загрузить подборку.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument()
    expect(screen.getByText('Как это работает')).toBeInTheDocument()
    fail = false
    server.resetHandlers()
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    await waitFor(() => expect(screen.getAllByText(/за 7 дней/)).toHaveLength(4))
    expect(getLocation()).toBe('/')
  })
})
