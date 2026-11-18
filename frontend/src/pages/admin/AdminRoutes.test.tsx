import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App'
import { useAuthStore } from '../../stores/auth'
import { renderAdmin } from '../../test/admin'
import { getLocation, renderWithProviders } from '../../test/render'
import { resetViewport } from '../../test/viewport'

const loader = vi.hoisted(() => ({ load: vi.fn() }))

vi.mock('../../app/loadAdminRoutes', () => ({ loadAdminRoutes: loader.load }))

describe('admin routes', () => {
  beforeEach(() => {
    loader.load.mockReset()
    loader.load.mockImplementation(() => import('./AdminRoutes'))
    resetViewport()
    useAuthStore.getState().setSession({
      accessToken: 'a',
      user: { id: 7, email: 'u@example.com', name: 'U', role: 'USER', emailVerified: true },
    })
  })

  it.each(['/admin', '/admin/parsers'])('shows the site 404 to a USER at %s', async (route) => {
    renderWithProviders(<App />, { route })
    expect(await screen.findByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
    expect(screen.getByAltText(/kitapradar/i)).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Админ-меню' })).toBeNull()
    expect(getLocation()).toBe(route)
    expect(loader.load).not.toHaveBeenCalled()
  })

  it('sends an anonymous visitor to login without loading the admin chunk', async () => {
    useAuthStore.getState().clearSession()
    renderWithProviders(<App />, { route: '/admin/parsers' })
    await waitFor(() => expect(getLocation()).toBe('/login?next=%2Fadmin%2Fparsers'))
    expect(loader.load).not.toHaveBeenCalled()
  })

  it('renders the overview for an ADMIN', async () => {
    renderAdmin('/admin')
    expect(await screen.findByRole('heading', { name: 'Обзор системы' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Админ-меню' })).toBeInTheDocument()
    expect(document.title).toBe('Обзор системы · Админ-панель | KitapRadar')
    expect(loader.load).toHaveBeenCalled()
  })

  it('shows not found for an unknown admin path', async () => {
    renderAdmin('/admin/nope')
    expect(await screen.findByRole('heading', { name: 'Такой страницы нет' })).toBeInTheDocument()
  })
})
