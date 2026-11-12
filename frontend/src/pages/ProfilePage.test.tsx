import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { QueryClient, useQueryClient } from '@tanstack/react-query'
import App from '../app/App'
import { AppProviders } from '../app/AppProviders'
import { MOCK_ACCESS_TOKEN, setMockSignedIn } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { getLocation, renderWithProviders } from '../test/render'
import { setViewport } from '../test/viewport'

const user = {
  id: 7,
  email: 'nurbo@example.com',
  name: 'Нурболат',
  role: 'USER' as 'USER' | 'ADMIN',
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

  describe('password', () => {
    function fillPasswords(current: string, next: string) {
      fireEvent.change(screen.getByLabelText('Текущий пароль'), { target: { value: current } })
      fireEvent.change(screen.getAllByLabelText('Новый пароль')[0]!, { target: { value: next } })
      fireEvent.click(screen.getByRole('button', { name: 'Сменить пароль' }))
    }

    it('changes the password with exactly two fields and clears the form', async () => {
      signIn()
      const bodies: unknown[] = []
      server.use(
        http.post('*/api/v1/me/password', async ({ request }) => {
          bodies.push(await request.json())
          return new HttpResponse(null, { status: 204 })
        }),
      )
      renderProfile()
      await screen.findByLabelText('Текущий пароль')
      fillPasswords('password123', 'newpassword1')
      await waitFor(() => expect(bodies).toEqual([{ currentPassword: 'password123', newPassword: 'newpassword1' }]))
      await waitFor(() =>
        expect(useToastStore.getState().toasts.map((t) => t.message)).toContain(
          'Пароль изменён, другие устройства отключены',
        ),
      )
      expect(screen.getByLabelText('Текущий пароль')).toHaveValue('')
      expect(screen.getAllByLabelText('Новый пароль')[0]).toHaveValue('')
    })

    it('marks a wrong current password', async () => {
      signIn()
      renderProfile()
      await screen.findByLabelText('Текущий пароль')
      fillPasswords('wrongpass1', 'newpassword1')
      await screen.findByText('Неверный пароль')
      expect(screen.getByLabelText('Текущий пароль')).toHaveAttribute('aria-invalid', 'true')
    })

    it('shows the rate limit alert and a countdown on the button', async () => {
      signIn()
      server.use(
        http.post('*/api/v1/me/password', () =>
          HttpResponse.json(
            { code: 'RATE_LIMITED', message: 'Слишком много запросов', fieldErrors: [] },
            { status: 429, headers: { 'Retry-After': '45' } },
          ),
        ),
      )
      renderProfile()
      await screen.findByLabelText('Текущий пароль')
      fillPasswords('password123', 'newpassword1')
      await screen.findByRole('alert')
      expect(screen.getByRole('button', { name: 'Сменить пароль · 0:45' })).toBeDisabled()
    })
  })

  describe('session', () => {
    it('logs out and goes home', async () => {
      signIn()
      let called = 0
      server.use(
        http.post('*/api/v1/auth/logout', () => {
          called += 1
          return new HttpResponse(null, { status: 204 })
        }),
      )
      renderProfile()
      fireEvent.click(await screen.findByRole('button', { name: 'Выйти' }))
      await waitFor(() => expect(getLocation()).toBe('/'))
      expect(called).toBe(1)
      expect(useAuthStore.getState().status).toBe('anonymous')
    })

    it('logs out everywhere, drops the me cache and goes to login', async () => {
      signIn()
      let called = 0
      server.use(
        http.post('*/api/v1/auth/logout-all', () => {
          called += 1
          return new HttpResponse(null, { status: 204 })
        }),
      )
      let client: QueryClient | undefined
      function Probe() {
        client = useQueryClient()
        return null
      }
      renderWithProviders(
        <AppProviders>
          <Probe />
          <App />
        </AppProviders>,
        { route: '/profile' },
      )
      fireEvent.click(await screen.findByRole('button', { name: 'Выйти на всех устройствах' }))
      await waitFor(() => expect(getLocation()).toBe('/login'))
      expect(called).toBe(1)
      expect(useAuthStore.getState().status).toBe('anonymous')
      expect(client?.getQueryData(['me'])).toBeUndefined()
    })
  })

  describe.each([
    ['desktop', 1280 as const],
    ['phone', 390 as const],
  ])('account deletion on %s', (_name, width) => {
    function openDialog() {
      setViewport(width)
      signIn()
      renderProfile()
    }

    async function open() {
      fireEvent.click(await screen.findByRole('button', { name: 'Удалить аккаунт…' }))
      return screen.findByRole('dialog', { name: 'Удалить аккаунт?' })
    }

    it('keeps the dialog on a wrong password', async () => {
      openDialog()
      const dialog = await open()
      fireEvent.change(within(dialog).getByLabelText('Пароль'), { target: { value: 'wrongpass1' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Удалить навсегда' }))
      await within(dialog).findByText('Неверный пароль')
      expect(within(dialog).getByLabelText('Пароль')).toHaveAttribute('aria-invalid', 'true')
      expect(screen.getByRole('dialog', { name: 'Удалить аккаунт?' })).toBeInTheDocument()
    })

    it('deletes the account with the password and goes home', async () => {
      openDialog()
      const bodies: unknown[] = []
      server.use(
        http.delete('*/api/v1/me', async ({ request }) => {
          bodies.push(await request.json())
          return new HttpResponse(null, { status: 204 })
        }),
      )
      const dialog = await open()
      fireEvent.change(within(dialog).getByLabelText('Пароль'), { target: { value: 'password123' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Удалить навсегда' }))
      await waitFor(() => expect(bodies).toEqual([{ password: 'password123' }]))
      await waitFor(() => expect(getLocation()).toBe('/'))
      expect(useAuthStore.getState().status).toBe('anonymous')
      expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Аккаунт удалён')
    })

    it('shows the administrator message on 409', async () => {
      openDialog()
      server.use(
        http.delete('*/api/v1/me', () =>
          HttpResponse.json({ code: 'CONFLICT', message: 'x', fieldErrors: [] }, { status: 409 }),
        ),
      )
      const dialog = await open()
      fireEvent.change(within(dialog).getByLabelText('Пароль'), { target: { value: 'password123' } })
      fireEvent.click(within(dialog).getByRole('button', { name: 'Удалить навсегда' }))
      await within(dialog).findByText('Аккаунт администратора удалить нельзя.')
      expect(getLocation()).toBe('/profile')
    })

    it('closes on Esc and returns focus to the trigger', async () => {
      openDialog()
      await open()
      fireEvent.keyDown(document, { key: 'Escape' })
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(screen.getByRole('button', { name: 'Удалить аккаунт…' })).toHaveFocus()
    })
  })

  it('has no deletion section for an administrator', async () => {
    signIn({ role: 'ADMIN' })
    server.use(http.get('*/api/v1/me', () => HttpResponse.json({ ...user, role: 'ADMIN' })))
    renderProfile()
    await screen.findByRole('heading', { level: 1, name: 'Профиль и настройки' })
    expect(screen.queryByRole('button', { name: 'Удалить аккаунт…' })).not.toBeInTheDocument()
  })
})
