import { act, fireEvent, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../app/App'
import { server } from '../mocks/server'
import { useToastStore } from '../stores/toast'
import { renderWithProviders } from '../test/render'

const ROUTE = '/check-email?email=nurbo%40example.com'

describe('CheckEmailPage', () => {
  afterEach(() => {
    vi.useRealTimers()
    useToastStore.setState({ toasts: [] })
  })

  it('names the address in bold and links to sign in and sign up', async () => {
    renderWithProviders(<App />, { route: ROUTE })
    const strong = await screen.findByText('nurbo@example.com')
    expect(strong.tagName).toBe('STRONG')
    expect(screen.getByRole('link', { name: 'Зарегистрируйтесь заново.' })).toHaveAttribute('href', '/register')
    expect(screen.getByRole('link', { name: 'Войти' })).toHaveAttribute('href', '/login')
  })

  it('resends once, toasts and blocks the button for a minute', async () => {
    const bodies: unknown[] = []
    server.use(
      http.post('*/api/v1/auth/resend-verification', async ({ request }) => {
        bodies.push(await request.json())
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderWithProviders(<App />, { route: ROUTE })
    fireEvent.click(await screen.findByRole('button', { name: 'Отправить письмо ещё раз' }))
    const blocked = await screen.findByRole('button', { name: /Отправить письмо ещё раз · \d:\d\d/ })
    expect(blocked).toBeDisabled()
    expect(blocked).toHaveTextContent(/· (1:00|0:59)/)
    expect(bodies).toEqual([{ email: 'nurbo@example.com' }])
    expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Письмо отправлено')
  })

  it('enables the button again after 60 seconds', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'Date'] })
    renderWithProviders(<App />, { route: ROUTE })
    fireEvent.click(await screen.findByRole('button', { name: 'Отправить письмо ещё раз' }))
    await screen.findByRole('button', { name: /· 1:00/ })
    act(() => {
      vi.advanceTimersByTime(60_000)
    })
    expect(screen.getByRole('button', { name: 'Отправить письмо ещё раз' })).toBeEnabled()
  })

  it('has no resend button without an address', async () => {
    renderWithProviders(<App />, { route: '/check-email' })
    await screen.findByRole('heading', { level: 1, name: 'Подтвердите почту' })
    expect(screen.queryByRole('button', { name: /Отправить письмо/ })).toBeNull()
    expect(screen.getByText(/Мы отправили письмо на указанный адрес/)).toBeInTheDocument()
  })
})
