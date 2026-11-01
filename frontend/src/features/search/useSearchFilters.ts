import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { FacetParams, SearchParams, SortKey } from '../../api/search'

export type SearchMode = 'search' | 'catalog'

export const STORE_ORDER = ['KASPI', 'FLIP', 'MELOMAN', 'WILDBERRIES', 'OZON'] as const
export const BINDING_ORDER = ['HARDCOVER', 'PAPERBACK'] as const
export const LANGUAGE_ORDER = ['ru', 'kk', 'en'] as const

const SORTS: SortKey[] = ['RELEVANCE', 'PRICE_ASC', 'DISCOUNT_DESC', 'STORES_DESC']

export interface SearchFilters {
  q: string
  stores: string[]
  minPrice?: number
  maxPrice?: number
  inStock: boolean
  binding: string[]
  language: string[]
  sort: SortKey
}

export function defaultSort(mode: SearchMode): SortKey {
  return mode === 'catalog' ? 'STORES_DESC' : 'RELEVANCE'
}

function parseList(raw: string | null, allowed: readonly string[]): string[] {
  if (!raw) return []
  const wanted = new Set(raw.split(',').map((v) => v.trim()))
  return allowed.filter((v) => wanted.has(v))
}

function parsePrice(raw: string | null): number | undefined {
  if (raw === null || !/^\d+$/.test(raw.trim())) return undefined
  return Number(raw.trim())
}

export function parseFilters(params: URLSearchParams, mode: SearchMode): SearchFilters {
  const rawSort = params.get('sort') as SortKey | null
  const sortValid = rawSort !== null && SORTS.includes(rawSort) && !(mode === 'catalog' && rawSort === 'RELEVANCE')
  return {
    q: params.get('q') ?? '',
    stores: parseList(params.get('stores'), STORE_ORDER),
    minPrice: parsePrice(params.get('minPrice')),
    maxPrice: parsePrice(params.get('maxPrice')),
    inStock: params.get('inStock') !== 'false',
    binding: parseList(params.get('binding'), BINDING_ORDER),
    language: parseList(params.get('language'), LANGUAGE_ORDER),
    sort: sortValid ? rawSort : defaultSort(mode),
  }
}

export function serializeFilters(filters: SearchFilters, mode: SearchMode): URLSearchParams {
  const out = new URLSearchParams()
  if (filters.q) out.set('q', filters.q)
  const csv = (key: string, values: string[], order: readonly string[]) => {
    const ordered = order.filter((v) => values.includes(v))
    if (ordered.length) out.set(key, ordered.join(','))
  }
  csv('stores', filters.stores, STORE_ORDER)
  if (filters.minPrice !== undefined) out.set('minPrice', String(filters.minPrice))
  if (filters.maxPrice !== undefined) out.set('maxPrice', String(filters.maxPrice))
  if (!filters.inStock) out.set('inStock', 'false')
  csv('binding', filters.binding, BINDING_ORDER)
  csv('language', filters.language, LANGUAGE_ORDER)
  if (filters.sort !== defaultSort(mode)) out.set('sort', filters.sort)
  return out
}

export function toFacetParams(filters: SearchFilters, mode: SearchMode): FacetParams {
  const params: FacetParams = { inStock: filters.inStock }
  if (mode === 'search' && filters.q.trim()) params.q = filters.q.trim()
  if (filters.stores.length) params.stores = filters.stores
  if (filters.binding.length) params.binding = filters.binding
  if (filters.language.length) params.language = filters.language
  if (filters.minPrice !== undefined) params.minPrice = filters.minPrice
  if (filters.maxPrice !== undefined) params.maxPrice = filters.maxPrice
  return params
}

export function toApiParams(filters: SearchFilters, mode: SearchMode): SearchParams {
  return { ...toFacetParams(filters, mode), sort: filters.sort }
}

export function countActiveFilters(filters: SearchFilters): number {
  return [
    filters.stores.length > 0,
    filters.minPrice !== undefined || filters.maxPrice !== undefined,
    !filters.inStock,
    filters.binding.length > 0,
    filters.language.length > 0,
  ].filter(Boolean).length
}

export function useSearchFilters(mode: SearchMode) {
  const [params, setParams] = useSearchParams()
  const filters = useMemo(() => parseFilters(params, mode), [params, mode])

  const setFilters = useCallback(
    (patch: Partial<SearchFilters>) => {
      setParams(serializeFilters({ ...filters, ...patch }, mode))
    },
    [filters, mode, setParams],
  )

  const resetFilters = useCallback(() => {
    setParams(serializeFilters({ ...parseFilters(new URLSearchParams(), mode), q: filters.q }, mode))
  }, [filters.q, mode, setParams])

  return { filters, setFilters, resetFilters }
}
