import { fireEvent, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { api } from '../../api/client'
import { useAuthStore } from '../../stores/auth'
import { mockApi } from '../../test/mockAdapter'
import { getLocation, renderWithProviders } from '../../test/render'
import { UserMenu } from './UserMenu'

function signIn(role: 'USER' | 'ADMIN') {
  useAuthStore.getState().setSession({
    accessToken: 'a',
    refreshToken: 'r',
    user: { id: 1, email: 'reader@example.com', name: 'Aidar', role },
  })
}

function openMenu() {
  const button = screen.getByRole('button', { name: 'Меню пользователя' })
  fireEvent.click(button)
  return button
}

describe('UserMenu', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('opens with focus on the first item', () => {
    signIn('USER')
    renderWithProviders(<UserMenu />)
    const button = openMenu()
    expect(button).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getAllByRole('menuitem')[0]).toHaveFocus()
    expect(screen.getByText('reader@example.com')).toBeInTheDocument()
  })

  it('shows the admin item only for admins', () => {
    signIn('ADMIN')
    const { unmount } = renderWithProviders(<UserMenu />)
    openMenu()
    expect(screen.getByRole('menuitem', { name: 'Админ-панель' })).toBeInTheDocument()
    unmount()
    signIn('USER')
    renderWithProviders(<UserMenu />)
    openMenu()
    expect(screen.queryByRole('menuitem', { name: 'Админ-панель' })).toBeNull()
  })

  it('closes on Escape and returns focus to the avatar', () => {
    signIn('USER')
    renderWithProviders(<UserMenu />)
    const button = openMenu()
    fireEvent.keyDown(screen.getAllByRole('menuitem')[0]!, { key: 'Escape' })
    expect(screen.queryByRole('menu')).toBeNull()
    expect(button).toHaveFocus()
  })

  it('moves focus with the arrow keys', () => {
    signIn('USER')
    renderWithProviders(<UserMenu />)
    openMenu()
    const items = screen.getAllByRole('menuitem')
    fireEvent.keyDown(items[0]!, { key: 'ArrowDown' })
    expect(items[1]).toHaveFocus()
    fireEvent.keyDown(items[1]!, { key: 'ArrowUp' })
    expect(items[0]).toHaveFocus()
  })

  it('logs out through the API and goes home', async () => {
    signIn('USER')
    const calls: string[] = []
    mockApi((config) => {
      calls.push(`${config.method} ${config.url}`)
      return [204]
    })
    renderWithProviders(<UserMenu />, { route: '/profile' })
    openMenu()
    fireEvent.click(screen.getByRole('menuitem', { name: 'Выйти' }))
    await waitFor(() => expect(useAuthStore.getState().status).toBe('anonymous'))
    await waitFor(() => expect(getLocation()).toBe('/'))
    expect(calls).toContain('post /auth/logout')
    api.defaults.adapter = undefined
  })
})
