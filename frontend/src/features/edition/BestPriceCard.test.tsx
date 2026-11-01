import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { editionDetail } from '../../mocks/editionFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { renderWithProviders } from '../../test/render'
import { BestPriceCard } from './BestPriceCard'

describe('BestPriceCard', () => {
  beforeEach(() => {
    useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
  })

  it('shows price, store, verdict and the external button', () => {
    renderWithProviders(<BestPriceCard edition={editionDetail(41)!} editionId={41} />)
    expect(screen.getByText('3 150 ₸')).toBeInTheDocument()
    expect(screen.getByText(/в наличии · доставка 1–2 дня/)).toHaveTextContent(
      'Flip.kz · в наличии · доставка 1–2 дня',
    )
    expect(screen.getByText('Ниже средней за 3 месяца на 12%')).toBeInTheDocument()
    const link = screen.getByRole('link', { name: /Перейти в Flip\.kz/ })
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
    expect(screen.getByText(/Обновлено 2 ч назад\./)).toBeInTheDocument()
  })

  it('sends a guest to login with the beacon return path', () => {
    renderWithProviders(<BestPriceCard edition={editionDetail(41)!} editionId={41} />)
    expect(screen.getByRole('link', { name: 'Следить за ценой' })).toHaveAttribute(
      'href',
      '/login?next=%2Feditions%2F41%3Fbeacon%3D1',
    )
  })

  it('shows an existing beacon for an authenticated user', async () => {
    useAuthStore.setState({ status: 'authenticated' })
    server.use(
      http.get('*/api/v1/beacons', () => HttpResponse.json([{ id: 7, editionId: 41 }])),
    )
    renderWithProviders(<BestPriceCard edition={editionDetail(41)!} editionId={41} />)
    const link = await screen.findByRole('link', { name: 'Маячок стоит · изменить' })
    expect(link).toHaveAttribute('href', '/beacons')
  })

  it('offers to follow when authenticated without a beacon', () => {
    useAuthStore.setState({ status: 'authenticated' })
    renderWithProviders(<BestPriceCard edition={editionDetail(41)!} editionId={41} />)
    expect(screen.getByRole('link', { name: 'Следить за ценой' })).toHaveAttribute(
      'href',
      '/beacons',
    )
  })

  it('handles an edition without a best price', () => {
    const edition = { ...editionDetail(41)!, bestPrice: undefined }
    renderWithProviders(<BestPriceCard edition={edition} editionId={41} />)
    expect(screen.getByText('Сейчас нет в наличии ни в одном магазине')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Перейти/ })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Следить за ценой' })).toBeInTheDocument()
  })
})
