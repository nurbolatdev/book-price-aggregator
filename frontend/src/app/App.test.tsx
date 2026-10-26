import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../stores/auth'
import App from './App'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('renders the home page lazily', async () => {
    renderAt('/')
    expect(
      await screen.findByRole('heading', { name: 'Цены на книги в магазинах Казахстана' }),
    ).toBeInTheDocument()
  })

  it('renders not found for unknown paths', async () => {
    renderAt('/nope')
    expect(await screen.findByRole('heading', { name: 'Страница не найдена' })).toBeInTheDocument()
  })

  it('shows the logo and login link for anonymous', async () => {
    renderAt('/')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('img', { name: 'KitapRadar' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Войти' })).toBeInTheDocument()
  })

  it('shows the user name for authenticated', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, name: 'Aidar', role: 'USER' },
    })
    renderAt('/')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('link', { name: 'Aidar' })).toHaveAttribute('href', '/profile')
    expect(screen.queryByRole('link', { name: 'Войти' })).toBeNull()
  })
})
