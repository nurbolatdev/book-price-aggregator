import { describe, expect, it } from 'vitest'
import { searchPage, searchPageFor } from './fixtures'

describe('searchPageFor', () => {
  it('without filters or sort is the plain fixture', () => {
    expect(searchPageFor({}, 0)).toEqual(searchPage(0))
    expect(searchPageFor({ sort: 'RELEVANCE' }, 1)).toEqual(searchPage(1))
  })

  it('sorts by price ascending across pages', () => {
    const prices = [0, 1].flatMap((p) => searchPageFor({ sort: 'PRICE_ASC' }, p).content.map((i) => i.bestPrice?.price ?? 0))
    expect(prices.length).toBe(searchPage(0).totalElements)
    expect(prices).toEqual([...prices].sort((a, b) => a - b))
  })

  it('sorts by number of stores descending', () => {
    const counts = searchPageFor({ sort: 'STORES_DESC' }, 0).content.map((i) => i.storesCount ?? 0)
    expect(counts).toEqual([...counts].sort((a, b) => b - a))
  })

  it('filters by price range and store', () => {
    const page = searchPageFor({ minPrice: 3000, maxPrice: 4000, stores: ['FLIP'] }, 0)
    expect(page.totalElements).toBeGreaterThan(0)
    for (const item of page.content) {
      expect(item.bestPrice?.price).toBeGreaterThanOrEqual(3000)
      expect(item.bestPrice?.price).toBeLessThanOrEqual(4000)
      expect(item.offers?.some((o) => o.store === 'FLIP')).toBe(true)
    }
  })
})
