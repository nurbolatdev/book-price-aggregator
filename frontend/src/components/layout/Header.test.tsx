import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../../stores/auth'
import { renderWithProviders } from '../../test/render'
import { Header } from './Header'
import { getHeaderVariant, hasOwnPhoneHeader } from './headerVariant'

describe('getHeaderVariant', () => {
  it.each([
    ['/', 'nav'],
    ['/deals', 'nav'],
    ['/beacons', 'nav'],
    ['/search', 'search'],
    ['/catalog', 'search'],
    ['/editions/5', 'search'],
    ['/login', 'minimal'],
    ['/reset-password', 'minimal'],
  ])('%s -> %s', (path, expected) => {
    expect(getHeaderVariant(path)).toBe(expected)
  })
})

describe('hasOwnPhoneHeader', () => {
  it('is true only for edition pages', () => {
    expect(hasOwnPhoneHeader('/editions/41')).toBe(true)
    expect(hasOwnPhoneHeader('/search')).toBe(false)
  })
})

describe('Header', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it.each(['/login', '/reset-password'])('shows only the logo on %s', (route) => {
    renderWithProviders(<Header />, { route })
    expect(screen.getByRole('link', { name: 'KitapRadar' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Войти' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Основное меню' })).toBeNull()
    expect(screen.queryByRole('combobox')).toBeNull()
    expect(screen.queryByRole('link', { name: 'Каталог' })).toBeNull()
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
      user: { id: 1, name: 'Aidar', role: 'USER' },
    })
    renderWithProviders(<Header />, { route: '/' })
    expect(screen.getByRole('button', { name: 'Меню пользователя' })).toHaveTextContent('A')
    expect(screen.queryByRole('link', { name: 'Войти' })).toBeNull()
    for (const link of screen.getAllByRole('link', { name: 'Мои маячки' })) {
      expect(link).toHaveAttribute('href', '/beacons')
    }
  })

  it('hides the site header on phones for edition pages only', () => {
    const edition = renderWithProviders(<Header />, { route: '/editions/41' })
    expect(edition.container.querySelector('header')).toHaveClass('max-md:hidden')
    edition.unmount()
    const search = renderWithProviders(<Header />, { route: '/search' })
    expect(search.container.querySelector('header')).not.toHaveClass('max-md:hidden')
  })
})
