import type { PricePeriod } from './types'
import type { FacetParams, SearchParams } from './search'

export const queryKeys = {
  me: ['me'],
  stores: ['stores'],
  popular: (limit: number) => ['search', 'popular', limit],
  suggest: (q: string) => ['search', 'suggest', q],
  search: (params: SearchParams) => ['search', 'results', params],
  facets: (params: FacetParams) => ['search', 'facets', params],
  drops: (limit: number) => ['home', 'drops', limit],
  edition: (id: number) => ['editions', id],
  priceHistory: (id: number, period: PricePeriod) => ['editions', id, 'price-history', period],
  similar: (id: number, limit: number) => ['editions', id, 'similar', limit],
  book: (id: number) => ['books', id],
  beaconForEdition: (id: number) => ['beacons', 'edition', id],
} as const

export const USER_SCOPED_KEYS: readonly (readonly string[])[] = [['me'], ['beacons']]
