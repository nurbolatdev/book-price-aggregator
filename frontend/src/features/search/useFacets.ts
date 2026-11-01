import { useQuery } from '@tanstack/react-query'
import { fetchFacets, type FacetParams } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

export function useFacets(params: FacetParams) {
  return useQuery({
    queryKey: queryKeys.facets(params),
    queryFn: ({ signal }) => fetchFacets(params, signal),
    staleTime: STALE_TIME.search,
  })
}
