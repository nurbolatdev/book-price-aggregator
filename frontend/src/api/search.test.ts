import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../mocks/server'
import { ApiError } from './errors'
import {
  fetchDrops,
  fetchFacets,
  fetchSearchPage,
  fetchSuggestions,
} from './search'

describe('search api', () => {
  it('sends page and size and returns the second page', async () => {
    let search = ''
    server.use(
      http.get('*/api/v1/search', ({ request }) => {
        search = new URL(request.url).search
        return HttpResponse.json({
          content: Array.from({ length: 9 }, (_, i) => ({ editionId: i })),
          page: 1,
          size: 20,
          totalElements: 12,
          totalPages: 2,
        })
      }),
    )
    const page = await fetchSearchPage({ q: 'мастер' }, 1)
    expect(search).toContain('page=1')
    expect(search).toContain('size=20')
    expect(page.content).toHaveLength(9)
  })

  it('returns fixture facets', async () => {
    const facets = await fetchFacets({ q: 'мастер' })
    expect(facets.stores).toContainEqual({ code: 'OZON', count: 8 })
    expect(facets.languages).toContainEqual({ value: 'en', count: 0 })
  })

  it('returns drops with storesCount', async () => {
    const drops = await fetchDrops(8)
    expect(drops).toHaveLength(4)
    expect(drops.every((d) => typeof d.storesCount === 'number')).toBe(true)
  })

  it('returns at most 8 suggestions with labels', async () => {
    const items = await fetchSuggestions('ма')
    expect(items.length).toBeGreaterThan(0)
    expect(items.length).toBeLessThanOrEqual(8)
    expect(items.every((s) => Boolean(s.label))).toBe(true)
  })

  it('rejects when the signal is aborted', async () => {
    const controller = new AbortController()
    const promise = fetchSuggestions('ма', controller.signal)
    controller.abort()
    await expect(promise).rejects.toBeDefined()
  })

  it('rejects with ApiError on a server error', async () => {
    server.use(http.get('*/api/v1/home/drops', () => HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status: 500 })))
    await expect(fetchDrops(8)).rejects.toMatchObject({ status: 500 })
    await expect(fetchDrops(8)).rejects.toBeInstanceOf(ApiError)
  })
})
