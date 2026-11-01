import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../mocks/server'
import { renderWithProviders } from '../../test/render'
import { SimilarBooks } from './SimilarBooks'

function renderSimilar() {
  return renderWithProviders(<SimilarBooks editionId={41} />, {
    route: '/editions/41',
    path: '/editions/:id',
  })
}

describe('SimilarBooks', () => {
  it('renders four cards linking to their editions', async () => {
    renderSimilar()
    const list = await screen.findByRole('list')
    expect(screen.getByRole('heading', { name: 'Похожие книги' })).toBeInTheDocument()
    const links = within(list).getAllByRole('link')
    expect(links.map((l) => l.getAttribute('href'))).toEqual([
      '/editions/51',
      '/editions/52',
      '/editions/53',
      '/editions/54',
    ])
    expect(links[0]).toHaveTextContent('Собачье сердце')
    expect(links[0]).toHaveTextContent(/от 1\s450\s₸/)
  })

  it('renders nothing when there are no similar books', async () => {
    server.use(http.get('*/api/v1/editions/:id/similar', () => HttpResponse.json([])))
    renderSimilar()
    await screen.findByRole('status')
    await new Promise((r) => setTimeout(r, 50))
    expect(screen.queryByRole('heading', { name: 'Похожие книги' })).not.toBeInTheDocument()
  })

  it('shows an error inside the block', async () => {
    server.use(http.get('*/api/v1/editions/:id/similar', () => new HttpResponse(null, { status: 500 })))
    renderSimilar()
    expect(
      await screen.findByText('Не удалось загрузить похожие книги.', undefined, { timeout: 5000 }),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Похожие книги' })).toBeInTheDocument()
  })
})
