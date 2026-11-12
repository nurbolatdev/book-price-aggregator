import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../app/App'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { getLocation, renderWithProviders } from '../test/render'

function renderRegister() {
  return renderWithProviders(<App />, { route: '/register' })
}

function fill(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

async function fillValid(email = 'nurbo@example.com') {
  await screen.findByRole('heading', { level: 1, name: 'Создать аккаунт' })
  fill('Имя', 'Нурболат')
  fill('Email', email)
  fill('Пароль', 'password123')
  fireEvent.click(screen.getByRole('checkbox'))
}

const submitButton = () => screen.getByRole('button', { name: /^Создать аккаунт/ })

describe('RegisterPage', () => {
  it('keeps the submit button disabled until the consent box is ticked', async () => {
    renderRegister()
    await screen.findByRole('heading', { level: 1, name: 'Создать аккаунт' })
    expect(submitButton()).toBeDisabled()
    fireEvent.click(screen.getByRole('checkbox'))
    expect(submitButton()).toBeEnabled()
  })

  it('shows linked field errors on an empty submit and the password hint', async () => {
    renderRegister()
    await screen.findByRole('heading', { level: 1, name: 'Создать аккаунт' })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(submitButton())
    const name = screen.getByLabelText('Имя')
    const email = screen.getByLabelText('Email')
    const password = screen.getByLabelText('Пароль')
    await waitFor(() => expect(name).toHaveAttribute('aria-invalid', 'true'))
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(password).toHaveAttribute('aria-invalid', 'true')
    expect(name).toHaveAccessibleDescription('Введите имя')
    expect(email).toHaveAccessibleDescription('Введите email')
    expect(password).toHaveAccessibleDescription(/Минимум 8 символов/)
  })

  it('shows the hint in the password description', async () => {
    renderRegister()
    await screen.findByRole('heading', { level: 1, name: 'Создать аккаунт' })
    expect(screen.getByLabelText('Пароль')).toHaveAccessibleDescription('Минимум 8 символов')
  })

  it('reports a taken email with a link to sign in', async () => {
    renderRegister()
    await fillValid('taken@example.com')
    fireEvent.click(submitButton())
    const message = await screen.findByText(/Этот email уже зарегистрирован\./)
    expect(message).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    const links = screen.getAllByRole('link', { name: 'Войти' })
    expect(links.some((link) => link.getAttribute('href') === '/login')).toBe(true)
    expect(getLocation()).toBe('/register')
  })

  it('goes to check-email without sending the consent flag', async () => {
    const bodies: unknown[] = []
    server.use(
      http.post('*/api/v1/auth/register', async ({ request }) => {
        bodies.push(await request.json())
        return HttpResponse.json({ userId: 7, emailVerificationRequired: true }, { status: 201 })
      }),
    )
    renderRegister()
    await fillValid()
    fireEvent.click(submitButton())
    await waitFor(() => expect(getLocation()).toBe('/check-email?email=nurbo%40example.com'))
    expect(bodies).toEqual([{ name: 'Нурболат', email: 'nurbo@example.com', password: 'password123' }])
  })

  it('shows the rate limit countdown on 429', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json(
          { code: 'RATE_LIMITED', message: 'x' },
          { status: 429, headers: { 'Retry-After': '30' } },
        ),
      ),
    )
    renderRegister()
    await fillValid()
    fireEvent.click(submitButton())
    expect(await screen.findByText(/Подождите 30 секунд/)).toBeInTheDocument()
    const button = screen.getByRole('button', { name: 'Создать аккаунт · 0:30' })
    expect(button).toBeDisabled()
  })

  it('sends one request on a double click', async () => {
    let calls = 0
    server.use(
      http.post('*/api/v1/auth/register', async () => {
        calls += 1
        await new Promise((resolve) => setTimeout(resolve, 20))
        return HttpResponse.json({ userId: 7, emailVerificationRequired: true }, { status: 201 })
      }),
    )
    renderRegister()
    await fillValid()
    fireEvent.click(submitButton())
    fireEvent.click(submitButton())
    await waitFor(() => expect(getLocation()).toContain('/check-email'))
    expect(calls).toBe(1)
  })

  it('sends a signed-in user away from the page', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      user: { id: 1, name: 'A', role: 'USER' },
    })
    renderRegister()
    await waitFor(() => expect(getLocation()).toBe('/'))
  })
})
