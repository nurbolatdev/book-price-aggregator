import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'
import { editionDetail } from '../mocks/editionFixtures'
import { server } from '../mocks/server'
import { fetchBeaconForEdition } from './beacons'
import { fetchBook, fetchPriceHistory, fetchSimilar } from './editions'
import { isNotFound } from './errors'
import { fetchEdition } from './search'

describe('edition api', () => {
  it('loads the edition with offers, best price and verdict', async () => {
    const detail = await fetchEdition(41)
    expect(detail.offers).toHaveLength(3)
    expect(detail.bestPrice?.store).toBe('FLIP')
    expect(detail.bestPrice?.price).toBe(3150)
    expect(detail.priceVerdict?.label).toBe('BELOW_AVERAGE')
  })

  it('sends the period and returns 1M points', async () => {
    let search = ''
    server.use(
      http.get('*/api/v1/editions/:id/price-history', ({ request }) => {
        search = new URL(request.url).search
        return HttpResponse.json({ points: [1, 2, 3, 4, 5].map((price) => ({ date: '2026-10-01', price })) })
      }),
    )
    const history = await fetchPriceHistory(41, '1M')
    expect(search).toContain('period=1M')
    expect(history.points).toHaveLength(5)
  })

  it('returns min, avg and max for 3M', async () => {
    const history = await fetchPriceHistory(41, '3M')
    expect([history.min, history.avg, history.max]).toEqual([3150, 3580, 3990])
  })

  it('sends the limit and returns similar editions', async () => {
    let search = ''
    server.use(
      http.get('*/api/v1/editions/:id/similar', ({ request }) => {
        search = new URL(request.url).search
        return HttpResponse.json([{ editionId: 1 }])
      }),
    )
    await fetchSimilar(41, 4)
    expect(search).toContain('limit=4')
    expect(await fetchSimilar(41, 4)).toHaveLength(1)
  })

  it('returns four fixture similar editions and none for other editions', async () => {
    expect(await fetchSimilar(41, 4)).toHaveLength(4)
    expect(await fetchSimilar(16, 4)).toEqual([])
  })

  it('loads a book with its editions', async () => {
    const book = await fetchBook(1)
    const ids = book.editions?.map((e) => e.editionId)
    expect(ids).toContain(41)
    expect(ids).toContain(16)
  })

  it('rejects with a not-found error for an unknown book', async () => {
    const error = await fetchBook(999999).catch((e: unknown) => e)
    expect(isNotFound(error)).toBe(true)
  })

  it('returns null when the user has no beacon for the edition', async () => {
    let search = ''
    server.use(
      http.get('*/api/v1/beacons', ({ request }) => {
        search = new URL(request.url).search
        return HttpResponse.json([])
      }),
    )
    expect(await fetchBeaconForEdition(41)).toBeNull()
    expect(search).toContain('editionId=41')
  })

  it('returns the first beacon when there is one', async () => {
    server.use(http.get('*/api/v1/beacons', () => HttpResponse.json([{ id: 7, editionId: 41 }])))
    expect((await fetchBeaconForEdition(41))?.id).toBe(7)
  })

  it('resolves every linked edition of edition 41', async () => {
    const detail = editionDetail(41)
    const ids = [
      ...(detail?.otherEditions ?? []).map((e) => e.editionId ?? 0),
      ...(await fetchSimilar(41, 4)).map((s) => s.editionId ?? 0),
    ]
    expect(ids.length).toBe(6)
    for (const id of ids) {
      expect((await fetchEdition(id)).editionId).toBe(id)
    }
  })
})
