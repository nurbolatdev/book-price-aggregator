import { HttpResponse, http } from 'msw'
import { drops, editionDetail, emptySearchPage, facets, popular, searchPage, stores, suggestions } from './fixtures'

function isEmptyQuery(url: URL): boolean {
  return (url.searchParams.get('q') ?? '').toLowerCase().includes('ничего')
}

export const handlers = [
  http.get('*/api/v1/stores', () => HttpResponse.json(stores)),

  http.get('*/api/v1/search/popular', ({ request }) => {
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? popular.length)
    return HttpResponse.json(popular.slice(0, limit))
  }),

  http.get('*/api/v1/search/suggest', ({ request }) => {
    const q = new URL(request.url).searchParams.get('q') ?? ''
    return HttpResponse.json(suggestions(q))
  }),

  http.get('*/api/v1/search/facets', () => HttpResponse.json(facets)),

  http.get('*/api/v1/search', ({ request }) => {
    const url = new URL(request.url)
    if (isEmptyQuery(url)) return HttpResponse.json(emptySearchPage)
    return HttpResponse.json(searchPage(Number(url.searchParams.get('page') ?? 0)))
  }),

  http.get('*/api/v1/home/drops', ({ request }) => {
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? drops.length)
    return HttpResponse.json(drops.slice(0, limit))
  }),

  http.get('*/api/v1/editions/:id', ({ params }) => {
    const detail = editionDetail(Number(params.id))
    if (!detail) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: 'Издание не найдено' }, { status: 404 })
    }
    return HttpResponse.json(detail)
  }),
]
