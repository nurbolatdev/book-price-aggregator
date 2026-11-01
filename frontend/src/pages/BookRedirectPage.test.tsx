import { fireEvent, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../app/App'
import { server } from '../mocks/server'
import { getLocation, renderWithProviders } from '../test/render'

describe('BookRedirectPage', () => {
  it('redirects to the cheapest edition', async () => {
    renderWithProviders(<App />, { route: '/books/1' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Издание' })).toBeInTheDocument()
    expect(getLocation()).toBe('/editions/16')
  })

  it('shows the 404 screen for an unknown book', async () => {
    renderWithProviders(<App />, { route: '/books/999999' })
    expect(await screen.findByText('Такой страницы нет')).toBeInTheDocument()
  })

  it('shows the 404 screen for an invalid id without a request', async () => {
    let requests = 0
    server.use(
      http.get('*/api/v1/books/:id', () => {
        requests += 1
        return HttpResponse.json({})
      }),
    )
    renderWithProviders(<App />, { route: '/books/abc' })
    expect(await screen.findByText('Такой страницы нет')).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('shows an error and recovers on retry', async () => {
    server.use(
      http.get('*/api/v1/books/:id', () => new HttpResponse(null, { status: 500 }), { once: true }),
    )
    renderWithProviders(<App />, { route: '/books/1' })
    expect(await screen.findByText('Не удалось загрузить книгу.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Издание' })).toBeInTheDocument()
    expect(getLocation()).toBe('/editions/16')
  })

  it('shows the 404 screen for a book without editions', async () => {
    server.use(http.get('*/api/v1/books/:id', () => HttpResponse.json({ id: 1, editions: [] })))
    renderWithProviders(<App />, { route: '/books/1' })
    expect(await screen.findByText('Такой страницы нет')).toBeInTheDocument()
  })
})
