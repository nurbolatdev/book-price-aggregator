import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { formatPrice } from '../../lib/format'
import { editionDetail } from '../../mocks/editionFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { renderWithProviders } from '../../test/render'
import { MobileBuyBar } from './MobileBuyBar'

describe('MobileBuyBar', () => {
  beforeEach(() => {
    useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
  })

  it('sends a guest to login and offers an external buy link', () => {
    renderWithProviders(<MobileBuyBar edition={editionDetail(41)!} editionId={41} />)
    expect(screen.getByRole('link', { name: 'Следить' })).toHaveAttribute(
      'href',
      '/login?next=%2Feditions%2F41%3Fbeacon%3D1',
    )
    const buy = screen.getByRole('link', { name: `Купить за ${formatPrice(3150)} в Flip.kz` })
    expect(buy.textContent).toBe(`${formatPrice(3150)} в Flip.kz`)
    expect(buy).toHaveAttribute('target', '_blank')
    expect(buy).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('shows an existing beacon for an authenticated user', async () => {
    useAuthStore.setState({ status: 'authenticated' })
    server.use(http.get('*/api/v1/beacons', () => HttpResponse.json([{ id: 7, editionId: 41 }])))
    renderWithProviders(<MobileBuyBar edition={editionDetail(41)!} editionId={41} />)
    const link = await screen.findByRole('link', { name: 'Маячок стоит' })
    expect(link).toHaveAttribute('href', '/beacons')
  })

  it('has only the follow button without a best price', () => {
    const edition = { ...editionDetail(41)!, bestPrice: undefined }
    renderWithProviders(<MobileBuyBar edition={edition} editionId={41} />)
    expect(screen.getByRole('link', { name: 'Следить' })).toBeInTheDocument()
    expect(screen.getAllByRole('link')).toHaveLength(1)
  })

  it('is sticky with a safe-area bottom padding and 52px buttons', () => {
    renderWithProviders(<MobileBuyBar edition={editionDetail(41)!} editionId={41} />)
    const links = screen.getAllByRole('link')
    const bar = links[0]!.parentElement!
    expect(bar.className).toContain('sticky')
    expect(bar.className).toContain('bottom-0')
    expect(bar.className).toContain('env(safe-area-inset-bottom)')
    for (const l of links) expect(l.className).toContain('h-[52px]')
  })
})
