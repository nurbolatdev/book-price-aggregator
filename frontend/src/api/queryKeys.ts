import type { FacetParams, SearchParams } from './search'

export const queryKeys = {
  stores: ['stores'],
  popular: (limit: number) => ['search', 'popular', limit],
  suggest: (q: string) => ['search', 'suggest', q],
  search: (params: SearchParams) => ['search', 'results', params],
  facets: (params: FacetParams) => ['search', 'facets', params],
  drops: (limit: number) => ['home', 'drops', limit],
  edition: (id: number) => ['editions', id],
} as const
