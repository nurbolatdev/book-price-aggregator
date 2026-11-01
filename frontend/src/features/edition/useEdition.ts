import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../../api/queryKeys'
import { fetchEdition } from '../../api/search'
import { STALE_TIME } from '../../app/queryClient'

export function useEdition(id: number) {
  return useQuery({
    queryKey: queryKeys.edition(id),
    queryFn: ({ signal }) => fetchEdition(id, signal),
    staleTime: STALE_TIME.edition,
    enabled: id > 0,
  })
}
