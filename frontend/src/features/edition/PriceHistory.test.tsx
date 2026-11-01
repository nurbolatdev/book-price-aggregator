import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../mocks/server'
import { renderWithProviders } from '../../test/render'
import { PriceHistory } from './PriceHistory'

function logPeriods() {
  const periods: string[] = []
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.endsWith('/price-history')) periods.push(url.searchParams.get('period') ?? '')
  })
  return periods
}

describe('PriceHistory', () => {
  it('loads 3M by default, shows stats and the lazy chart', async () => {
    const periods = logPeriods()
    renderWithProviders(<PriceHistory editionId={41} />)

    expect(await screen.findByText('Минимум:')).toBeInTheDocument()
    expect(periods[0]).toBe('3M')
    expect(screen.getByRole('button', { name: '3 мес' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('3 150 ₸')).toBeInTheDocument()
    expect(screen.getByText('3 580 ₸')).toBeInTheDocument()
    expect(screen.getByText('3 990 ₸')).toBeInTheDocument()
    expect(
      await screen.findByRole('img', { name: /снизилась с 3 990 до 3 150/ }),
    ).toBeInTheDocument()
  })

  it('switches the period and requests it', async () => {
    const periods = logPeriods()
    renderWithProviders(<PriceHistory editionId={41} />)
    await screen.findByText('Минимум:')

    fireEvent.click(screen.getByRole('button', { name: '1 мес' }))

    await waitFor(() => expect(periods).toContain('1M'))
    expect(screen.getByRole('button', { name: '1 мес' })).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() =>
      expect(screen.getByRole('img', { name: /за месяц/ })).toBeInTheDocument(),
    )
  })

  it('shows a hint instead of the chart for a single point', async () => {
    server.use(
      http.get('*/api/v1/editions/:id/price-history', () =>
        HttpResponse.json({
          editionId: 41,
          period: '3M',
          min: 3150,
          avg: 3150,
          max: 3150,
          points: [{ date: '2026-10-20', price: 3150 }],
        }),
      ),
    )
    renderWithProviders(<PriceHistory editionId={41} />)

    expect(
      await screen.findByText('История появится после нескольких проверок цены'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('shows an error inside the section and keeps the period switch', async () => {
    server.use(
      http.get('*/api/v1/editions/:id/price-history', () =>
        HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status: 500 }),
      ),
    )
    renderWithProviders(<PriceHistory editionId={41} />)

    const section = (await screen.findByText('История минимальной цены')).closest('section')!
    expect(
      await within(section).findByText('Не удалось загрузить историю цены.'),
    ).toBeInTheDocument()
    expect(within(section).getByRole('group', { name: 'Период' })).toBeInTheDocument()
  })
})
