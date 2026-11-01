import { useQuery } from '@tanstack/react-query'
import { fetchDrops } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

export function useDrops(limit: number) {
  return useQuery({
    queryKey: queryKeys.drops(limit),
    queryFn: ({ signal }) => fetchDrops(limit, signal),
    staleTime: STALE_TIME.search,
  })
}
