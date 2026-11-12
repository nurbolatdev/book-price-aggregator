import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../app/App'
import { queryKeys } from '../api/queryKeys'
import { MOCK_PASSWORD } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { getLocation, renderWithProviders } from '../test/render'

function renderLogin(route = '/login') {
  return renderWithProviders(<App />, { route })
}

function fill(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

async function fillValid(email = 'nurbo@example.com', password = MOCK_PASSWORD) {
  await screen.findByRole('heading', { level: 1, name: 'Вход' })
  fill('Email', email)
  fill('Пароль', password)
}

const submitButton = () => screen.getByRole('button', { name: /^Войти/ })

describe('LoginPage', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('signs in and returns to the path from next, caching the user', async () => {
    const { queryClient } = renderLogin('/login?next=%2Feditions%2F41%3Fbeacon%3D1')
    await fillValid()
    fireEvent.click(submitButton())
    await waitFor(() => expect(getLocation()).toMatch(/^\/editions\/41/))
    expect(useAuthStore.getState().status).toBe('authenticated')
    expect(queryClient.getQueryData(queryKeys.me)).toMatchObject({ email: 'nurbo@example.com' })
  })

  it.each([['https%3A%2F%2Fevil.example'], ['%2F%2Fevil.example']])(
    'ignores the unsafe return path %s',
    async (next) => {
      renderLogin(`/login?next=${next}`)
      await fillValid()
      fireEvent.click(submitButton())
      await waitFor(() => expect(useAuthStore.getState().status).toBe('authenticated'))
      await waitFor(() => expect(getLocation()).toBe('/'))
    },
  )

  it('explains wrong credentials and clears the error on typing', async () => {
    renderLogin()
    await fillValid('nurbo@example.com', 'wrongpass1')
    fireEvent.click(submitButton())
    const alert = await screen.findByText(/Неверный email или пароль/)
    const email = screen.getByLabelText('Email')
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveAccessibleDescription(/Неверный email или пароль/)
    expect(alert).toBeInTheDocument()
    fill('Email', 'nurbo@example.org')
    await waitFor(() => expect(screen.queryByText(/Неверный email или пароль/)).not.toBeInTheDocument())
    expect(email).not.toHaveAttribute('aria-invalid')
  })

  it('shows the rate limit countdown and re-enables the button', async () => {
    renderLogin()
    await fillValid('limit@example.com')
    vi.useFakeTimers({ shouldAdvanceTime: true })
    fireEvent.click(submitButton())
    expect(await screen.findByText(/Подождите 45 секунд/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Войти · 0:45' })).toBeDisabled()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(45_000)
    })
    expect(screen.getByRole('button', { name: 'Войти' })).toBeEnabled()
    expect(screen.queryByText(/Подождите/)).not.toBeInTheDocument()
  })

  it('sends one request on a double click', async () => {
    let calls = 0
    server.use(
      http.post('*/api/v1/auth/login', async () => {
        calls += 1
        await new Promise((resolve) => setTimeout(resolve, 20))
        return HttpResponse.json({ code: 'INVALID_CREDENTIALS', message: 'x' }, { status: 401 })
      }),
    )
    renderLogin()
    await fillValid()
    fireEvent.click(submitButton())
    fireEvent.click(submitButton())
    await screen.findByText(/Неверный email или пароль/)
    expect(calls).toBe(1)
  })

  it('links to password recovery and sign up', async () => {
    renderLogin()
    await screen.findByRole('heading', { level: 1, name: 'Вход' })
    expect(screen.getByRole('link', { name: 'Забыли пароль?' })).toHaveAttribute('href', '/forgot-password')
    expect(screen.getByRole('link', { name: 'Создать аккаунт' })).toHaveAttribute('href', '/register')
  })

  it('validates an empty form and focuses the email', async () => {
    renderLogin()
    await screen.findByRole('heading', { level: 1, name: 'Вход' })
    fireEvent.click(submitButton())
    expect(await screen.findByText('Введите email')).toBeInTheDocument()
    expect(screen.getByText('Введите пароль')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Email')).toHaveFocus())
  })

  it('sends a signed-in user away from the page', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      user: { id: 1, name: 'A', role: 'USER' },
    })
    renderLogin()
    await waitFor(() => expect(getLocation()).toBe('/'))
  })
})
