import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../app/App'
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

function signIn(patch: Partial<typeof user> = {}) {
  setMockSignedIn(true)
  useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user: { ...user, ...patch } })
}

function renderProfile() {
  return renderWithProviders(<App />, { route: '/profile' })
}

describe('ProfilePage', () => {
  it('shows the unverified banner and resends to the own address', async () => {
    signIn()
    const bodies: unknown[] = []
    server.use(
      http.post('*/api/v1/auth/resend-verification', async ({ request }) => {
        bodies.push(await request.json())
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderProfile()
    await screen.findByRole('heading', { level: 1, name: 'Профиль и настройки' })
    expect(screen.getByText(/Почта не подтверждена — маячки работают/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Отправить письмо ещё раз' }))
    await waitFor(() => expect(bodies).toEqual([{ email: 'nurbo@example.com' }]))
    await waitFor(() =>
      expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Письмо отправлено'),
    )
  })

  it('has no banner for a verified user', async () => {
    signIn({ emailVerified: true })
    server.use(http.get('*/api/v1/me', () => HttpResponse.json({ ...user, emailVerified: true })))
    renderProfile()
    await screen.findByRole('heading', { level: 1, name: 'Профиль и настройки' })
    expect(screen.queryByText(/Почта не подтверждена/)).not.toBeInTheDocument()
    expect(screen.getByText('подтверждён')).toBeInTheDocument()
  })

  it('saves the name with a PATCH of exactly the name', async () => {
    signIn()
    const bodies: unknown[] = []
    server.use(
      http.patch('*/api/v1/me', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        bodies.push(body)
        return HttpResponse.json({ ...user, ...body })
      }),
    )
    renderProfile()
    const input = await screen.findByLabelText('Имя')
    fireEvent.change(input, { target: { value: 'Айгерим' } })
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }))
    await waitFor(() => expect(bodies).toEqual([{ name: 'Айгерим' }]))
    await waitFor(() => expect(useAuthStore.getState().user?.name).toBe('Айгерим'))
    expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Изменения сохранены')
    expect(screen.getByRole('button', { name: 'Меню пользователя' })).toHaveTextContent('А')
  })

  it('rejects an empty name without a request', async () => {
    signIn()
    const bodies: unknown[] = []
    server.use(
      http.patch('*/api/v1/me', async ({ request }) => {
        bodies.push(await request.json())
        return HttpResponse.json(user)
      }),
    )
    renderProfile()
    const input = await screen.findByLabelText('Имя')
    fireEvent.change(input, { target: { value: '   ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }))
    await screen.findByText('Введите имя')
    expect(bodies).toEqual([])
  })

  it('sends a guest to the login page with the return path', async () => {
    useAuthStore.getState().clearSession()
    renderProfile()
    await waitFor(() => expect(getLocation()).toBe('/login?next=%2Fprofile'))
  })
})
