import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../stores/auth'
import { getLocation, renderWithProviders } from '../test/render'
import App from './App'

function renderAt(path: string) {
  return renderWithProviders(<App />, { route: path })
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
    expect(await screen.findByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
  })

  it('searches from the not found page', async () => {
    renderAt('/nope')
    const input = await screen.findByPlaceholderText('Название, автор или ISBN')
    fireEvent.change(input, { target: { value: 'abc' } })
    fireEvent.submit(input.closest('form')!)
    await waitFor(() => expect(getLocation()).toBe('/search?q=abc'))
    expect(screen.queryByRole('heading', { name: 'Такой страницы нет' })).toBeNull()
  })

  it('shows the logo and login link for anonymous', async () => {
    renderAt('/')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('img', { name: 'KitapRadar' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Войти' })).toBeInTheDocument()
  })

  it('shows the user menu button for authenticated', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, name: 'Aidar', role: 'USER' },
    })
    renderAt('/')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('button', { name: 'Меню пользователя' })).toHaveTextContent('A')
    expect(screen.queryByRole('link', { name: 'Войти' })).toBeNull()
  })
})
