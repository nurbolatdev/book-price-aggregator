import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../../stores/auth'
import { getLocation, renderWithProviders } from '../../test/render'
import { Header } from './Header'

function openMenu() {
  fireEvent.click(screen.getByRole('button', { name: 'Основное меню' }))
  return screen.getByRole('dialog', { name: 'Меню' })
}

describe('MobileMenu', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('lists navigation links and login for guests', () => {
    renderWithProviders(<Header />, { route: '/' })
    const dialog = openMenu()
    expect(within(dialog).getByRole('link', { name: 'Каталог' })).toBeInTheDocument()
    expect(within(dialog).getByRole('link', { name: 'Скидки недели' })).toBeInTheDocument()
    expect(within(dialog).getByRole('link', { name: 'Мои маячки' })).toBeInTheDocument()
    expect(within(dialog).getByRole('link', { name: 'Войти' })).toBeInTheDocument()
  })

  it('has no login link for authenticated users', () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, name: 'Aidar', role: 'USER' },
    })
    renderWithProviders(<Header />, { route: '/' })
    const dialog = openMenu()
    expect(within(dialog).queryByRole('link', { name: 'Войти' })).toBeNull()
  })

  it('navigates and closes the dialog', async () => {
    renderWithProviders(<Header />, { route: '/' })
    const dialog = openMenu()
    fireEvent.click(within(dialog).getByRole('link', { name: 'Каталог' }))
    await waitFor(() => expect(getLocation()).toBe('/catalog'))
    expect(screen.queryByRole('dialog')).toBeNull()
  })
})
