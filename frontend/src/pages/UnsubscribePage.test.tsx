import { StrictMode } from 'react'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../app/App'
import { MOCK_ACCESS_TOKEN } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { renderWithProviders } from '../test/render'

const user = {
  id: 7,
  email: 'nurbo@example.com',
  name: 'Нурболат',
  role: 'USER' as const,
  emailVerified: true,
  notifyFrequency: 'INSTANT' as const,
  weeklyDigest: true,
}

function renderPage(query: string) {
  return renderWithProviders(
    <StrictMode>
      <App />
    </StrictMode>,
    { route: `/unsubscribe${query}` },
  )
}

interface Seen {
  url: string
  auth: string | null
  body?: unknown
}

function record() {
  const seen: Seen[] = []
  server.events.on('request:start', async ({ request }) => {
    if (!request.url.includes('/beacons/unsubscribe') && !request.url.includes('/auth/refresh')) return
    const clone = request.clone()
    const text = await clone.text()
    seen.push({
      url: new URL(request.url).pathname,
      auth: request.headers.get('Authorization'),
      body: text ? JSON.parse(text) : undefined,
    })
  })
  return seen
}

const pauseButton = () => screen.findByRole('button', { name: 'Поставить маячок на паузу' })

describe('UnsubscribePage', () => {
  it('asks first and sends nothing on load', async () => {
    const seen = record()
    renderPage('?token=valid')
    await screen.findByRole('heading', { level: 1, name: 'Отключить маячок?' })
    expect(screen.getByText('Маячок из письма')).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(seen).toEqual([])
    server.events.removeAllListeners()
  })

  it('pauses with one request and no bearer, then shows the result', async () => {
    const seen = record()
    renderPage('?token=valid')
    const button = await pauseButton()
    fireEvent.click(button)
    fireEvent.click(button)
    const heading = await screen.findByRole('heading', { level: 1, name: 'Маячок на паузе' })
    await waitFor(() => expect(heading).toHaveFocus())
    expect(screen.getByText('Больше не пишем о книге «Мастер и Маргарита». Остальные маячки работают как прежде.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Открыть мои маячки' })).toHaveAttribute('href', '/beacons')
    expect(seen).toEqual([{ url: '/api/v1/beacons/unsubscribe', auth: null, body: { token: 'valid' } }])
    server.events.removeAllListeners()
  })

  it('works signed in and still sends no bearer', async () => {
    useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user })
    const seen = record()
    renderPage('?token=valid')
    fireEvent.click(await pauseButton())
    await screen.findByRole('heading', { level: 1, name: 'Маячок на паузе' })
    expect(seen).toHaveLength(1)
    expect(seen[0]?.auth).toBeNull()
    server.events.removeAllListeners()
  })

  it.each(['expired', 'x'])('shows the invalid panel for token %s without a refresh', async (token) => {
    const seen = record()
    renderPage(`?token=${token}`)
    fireEvent.click(await pauseButton())
    await screen.findByRole('heading', { level: 1, name: 'Ссылка недействительна' })
    expect(seen.some((r) => r.url.includes('/auth/refresh'))).toBe(false)
    expect(screen.getByRole('link', { name: 'Мои маячки' })).toHaveAttribute('href', '/beacons')
    server.events.removeAllListeners()
  })

  it('shows the invalid panel at once without a token', async () => {
    const seen = record()
    renderPage('')
    await screen.findByRole('heading', { level: 1, name: 'Ссылка недействительна' })
    expect(screen.queryByRole('button', { name: 'Поставить маячок на паузу' })).toBeNull()
    expect(seen).toEqual([])
    server.events.removeAllListeners()
  })

  it('keeps the button after a server error and retries', async () => {
    let calls = 0
    server.use(
      http.post('*/api/v1/beacons/unsubscribe', () => {
        calls += 1
        return HttpResponse.json({ code: 'INTERNAL_ERROR', message: 'boom' }, { status: 500 })
      }),
    )
    renderPage('?token=valid')
    fireEvent.click(await pauseButton())
    await screen.findByText('Не удалось поставить маячок на паузу. Попробуйте ещё раз.')
    const button = screen.getByRole('button', { name: 'Поставить маячок на паузу' })
    expect(button).toBeEnabled()
    fireEvent.click(button)
    await waitFor(() => expect(calls).toBe(2))
  })

  it('leaves for the home page', async () => {
    renderPage('?token=valid')
    await screen.findByRole('heading', { level: 1, name: 'Отключить маячок?' })
    expect(screen.getByRole('link', { name: 'Оставить как есть' })).toHaveAttribute('href', '/')
  })
})
