import { api } from './client'
import type {
  DropItem,
  EditionDetailResponse,
  SearchFacets,
  SearchPage,
  StoreResponse,
  Suggestion,
} from './types'

export const SEARCH_PAGE_SIZE = 20

export type SortKey = 'RELEVANCE' | 'PRICE_ASC' | 'DISCOUNT_DESC' | 'STORES_DESC'

export interface SearchParams {
  q?: string
  stores?: string[]
  binding?: string[]
  language?: string[]
  minPrice?: number
  maxPrice?: number
  inStock?: boolean
  sort?: SortKey
}

export type FacetParams = Omit<SearchParams, 'sort'>

function filterQuery(params: FacetParams): Record<string, string | number | boolean> {
  const query: Record<string, string | number | boolean> = {}
  if (params.q) query.q = params.q
  if (params.stores?.length) query.stores = params.stores.join(',')
  if (params.binding?.length) query.binding = params.binding.join(',')
  if (params.language?.length) query.language = params.language.join(',')
  if (params.minPrice !== undefined) query.minPrice = params.minPrice
  if (params.maxPrice !== undefined) query.maxPrice = params.maxPrice
  if (params.inStock !== undefined) query.inStock = params.inStock
  return query
}

export async function fetchStores(signal?: AbortSignal): Promise<StoreResponse[]> {
  const { data } = await api.get<StoreResponse[]>('/stores', { signal })
  return data
}

export async function fetchPopular(limit: number, signal?: AbortSignal): Promise<string[]> {
  const { data } = await api.get<string[]>('/search/popular', { params: { limit }, signal })
  return data
}

export async function fetchSuggestions(q: string, signal?: AbortSignal): Promise<Suggestion[]> {
  const { data } = await api.get<Suggestion[]>('/search/suggest', { params: { q }, signal })
  return data
}

export async function fetchSearchPage(
  params: SearchParams,
  page: number,
  signal?: AbortSignal,
): Promise<SearchPage> {
  const query: Record<string, string | number | boolean> = {
    ...filterQuery(params),
    page,
    size: SEARCH_PAGE_SIZE,
  }
  if (params.sort) query.sort = params.sort
  const { data } = await api.get<SearchPage>('/search', { params: query, signal })
  return data
}

export async function fetchFacets(params: FacetParams, signal?: AbortSignal): Promise<SearchFacets> {
  const { data } = await api.get<SearchFacets>('/search/facets', {
    params: filterQuery(params),
    signal,
  })
  return data
}

export async function fetchDrops(limit: number, signal?: AbortSignal): Promise<DropItem[]> {
  const { data } = await api.get<DropItem[]>('/home/drops', { params: { limit }, signal })
  return data
}

export async function fetchEdition(id: number, signal?: AbortSignal): Promise<EditionDetailResponse> {
  const { data } = await api.get<EditionDetailResponse>(`/editions/${id}`, { signal })
  return data
}
