import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../../stores/auth'
import { renderWithProviders } from '../../test/render'
import { Header } from './Header'
import { getHeaderVariant } from './headerVariant'

describe('getHeaderVariant', () => {
  it.each([
    ['/', 'nav'],
    ['/deals', 'nav'],
    ['/beacons', 'nav'],
    ['/search', 'search'],
    ['/catalog', 'search'],
    ['/editions/5', 'search'],
  ])('%s -> %s', (path, expected) => {
    expect(getHeaderVariant(path)).toBe(expected)
  })
})

describe('Header', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('shows nav links and no search field on the home page', () => {
    renderWithProviders(<Header />, { route: '/' })
    expect(screen.getByRole('link', { name: 'Каталог' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Скидки недели' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Мои маячки' })).toBeInTheDocument()
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('marks the current section', () => {
    renderWithProviders(<Header />, { route: '/deals' })
    expect(screen.getByRole('link', { name: 'Скидки недели' })).toHaveAttribute('aria-current', 'page')
  })

  it('shows the search field with the query and no catalog link on search pages', () => {
    renderWithProviders(<Header />, { route: '/search?q=abc' })
    expect(screen.getByRole('combobox', { name: 'Поиск книги' })).toHaveValue('abc')
    expect(screen.queryByRole('link', { name: 'Каталог' })).toBeNull()
  })

  it('sends guests to login from beacons', () => {
    renderWithProviders(<Header />, { route: '/' })
    expect(screen.getByRole('link', { name: 'Мои маячки' })).toHaveAttribute('href', '/login?next=%2Fbeacons')
    expect(screen.getByRole('link', { name: 'Войти' })).toBeInTheDocument()
  })

  it('shows the user menu button for authenticated users', () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, name: 'Aidar', role: 'USER' },
    })
    renderWithProviders(<Header />, { route: '/' })
    expect(screen.getByRole('button', { name: 'Меню пользователя' })).toHaveTextContent('A')
    expect(screen.queryByRole('link', { name: 'Войти' })).toBeNull()
    for (const link of screen.getAllByRole('link', { name: 'Мои маячки' })) {
      expect(link).toHaveAttribute('href', '/beacons')
    }
  })
})
