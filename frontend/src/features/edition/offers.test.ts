import { describe, expect, it } from 'vitest'
import type { OfferInfo } from '../../api/types'
import { bestOfferId, lastSeen, sortOffers } from './offers'

const offers: OfferInfo[] = [
  { offerId: 1, store: 'KASPI', storeName: 'Kaspi.kz', price: 2000, inStock: false },
  { offerId: 2, store: 'OZON', storeName: 'Ozon', price: 3720, inStock: true },
  { offerId: 3, store: 'FLIP', storeName: 'Flip.kz', price: 3150, inStock: true },
]

describe('offers', () => {
  it('puts in-stock first even when out-of-stock is cheaper, then by price', () => {
    expect(sortOffers(offers).map((o) => o.offerId)).toEqual([3, 2, 1])
  })

  it('breaks price ties by store name', () => {
    const tie: OfferInfo[] = [
      { offerId: 1, storeName: 'Ozon', price: 100 },
      { offerId: 2, storeName: 'Flip.kz', price: 100 },
    ]
    expect(sortOffers(tie).map((o) => o.offerId)).toEqual([2, 1])
  })

  it('bestOfferId matches the best price store', () => {
    expect(bestOfferId(offers, { store: 'OZON', price: 3720 })).toBe(2)
  })

  it('falls back to the first in-stock offer', () => {
    expect(bestOfferId(offers, { store: 'KASPI', price: 2000 })).toBe(3)
    expect(bestOfferId(offers)).toBe(3)
  })

  it('returns null when nothing is in stock', () => {
    expect(bestOfferId(offers.slice(0, 1))).toBeNull()
  })

  it('lastSeen is the latest timestamp', () => {
    expect(
      lastSeen([
        { lastSeenAt: '2026-10-20T10:00:00Z' },
        { lastSeenAt: '2026-10-20T12:00:00Z' },
        {},
      ]),
    ).toBe('2026-10-20T12:00:00Z')
    expect(lastSeen([])).toBeNull()
  })
})
