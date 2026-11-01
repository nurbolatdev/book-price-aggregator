import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { DropItem } from '../../api/types'
import { drops } from '../../mocks/fixtures'
import { server } from '../../mocks/server'
import { renderWithProviders } from '../../test/render'
import { DropCard } from './DropCard'

const base = drops[0] as DropItem

describe('DropCard', () => {
  it('shows price, drop and store line', async () => {
    renderWithProviders(<DropCard drop={base} />)
    expect(screen.getByText(/от 3\s150 ₸/)).toBeInTheDocument()
    expect(screen.getByText('−18% за 7 дней')).toBeInTheDocument()
    expect(await screen.findByText('Flip.kz · ещё в 2 магазинах')).toBeInTheDocument()
    expect(screen.getByRole('link')).toHaveAttribute('href', '/editions/6')
  })

  it('uses singular for one other store', async () => {
    renderWithProviders(<DropCard drop={{ ...base, storesCount: 2 }} />)
    expect(await screen.findByText('Flip.kz · ещё в 1 магазине')).toBeInTheDocument()
  })

  it('shows only the store name when it is the only one', async () => {
    renderWithProviders(<DropCard drop={{ ...base, storesCount: 1 }} />)
    expect(await screen.findByText('Flip.kz')).toBeInTheDocument()
  })

  it('prefetches the edition on hover', async () => {
    let count = 0
    server.use(
      http.get('*/api/v1/editions/:id', () => {
        count += 1
        return HttpResponse.json({ editionId: 6 })
      }),
    )
    renderWithProviders(<DropCard drop={base} />)
    fireEvent.mouseEnter(screen.getByRole('link'))
    await waitFor(() => expect(count).toBe(1))
  })
})
