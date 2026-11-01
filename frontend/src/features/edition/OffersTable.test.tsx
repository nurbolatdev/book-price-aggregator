import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { editionDetail } from '../../mocks/editionFixtures'
import { renderWithProviders } from '../../test/render'
import { OffersTable } from './OffersTable'

function detail() {
  const base = editionDetail(41)!
  return {
    ...base,
    offers: [
      ...(base.offers ?? []),
      {
        offerId: 99,
        store: 'KASPI' as const,
        storeName: 'Kaspi.kz',
        price: 4500,
        inStock: false,
        deliveryText: 'Курьер',
        url: 'https://kaspi.example/x',
        lastSeenAt: new Date().toISOString(),
      },
    ],
  }
}

describe('OffersTable', () => {
  it('renders heading and rows in order with out-of-stock last', () => {
    renderWithProviders(<OffersTable edition={detail()} />)
    expect(screen.getByRole('heading', { name: 'Цены в 4 магазинах' })).toBeInTheDocument()
    const rows = screen.getAllByRole('row').slice(1)
    expect(rows.map((r) => within(r).getAllByRole('cell')[0]?.textContent)).toEqual([
      'Flip.kz',
      'Ozon',
      'Meloman',
      'Kaspi.kz',
    ])
  })

  it('highlights only the best row', () => {
    renderWithProviders(<OffersTable edition={detail()} />)
    const rows = screen.getAllByRole('row').slice(1)
    expect(rows.filter((r) => r.getAttribute('data-best') === 'true')).toHaveLength(1)
    expect(rows[0]).toHaveAttribute('data-best', 'true')
    expect(rows[0]).toHaveClass('bg-best-row')
  })

  it('shows the old price only for Flip and marks out-of-stock', () => {
    renderWithProviders(<OffersTable edition={detail()} />)
    expect(screen.getAllByText('3 850 ₸')).toHaveLength(1)
    const meloman = screen.getAllByRole('row')[3]!
    expect(within(meloman).getByText('Нет в наличии')).toBeInTheDocument()
  })

  it('links to the store in a new tab', () => {
    const d = detail()
    renderWithProviders(<OffersTable edition={d} />)
    const link = screen.getByRole('link', { name: 'В магазин Flip.kz' })
    expect(link).toHaveAttribute('href', d.offers![0]?.url)
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })
})
