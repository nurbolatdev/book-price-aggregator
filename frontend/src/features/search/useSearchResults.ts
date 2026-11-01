import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchSearchPage, type SearchParams } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

export function useSearchResults(params: SearchParams) {
  return useInfiniteQuery({
    queryKey: queryKeys.search(params),
    queryFn: ({ pageParam, signal }) => fetchSearchPage(params, pageParam, signal),
    initialPageParam: 0,
    getNextPageParam: (last, all) => {
      const loaded = all.reduce((sum, p) => sum + (p.content?.length ?? 0), 0)
      return loaded < (last.totalElements ?? 0) ? (last.page ?? 0) + 1 : undefined
    },
    staleTime: STALE_TIME.search,
  })
}
