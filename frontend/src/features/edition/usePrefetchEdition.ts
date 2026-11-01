import { useQueryClient } from '@tanstack/react-query'
import { fetchEdition } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

export function usePrefetchEdition(): (editionId: number) => void {
  const queryClient = useQueryClient()
  return (editionId) => {
    void queryClient.prefetchQuery({
      queryKey: queryKeys.edition(editionId),
      queryFn: ({ signal }) => fetchEdition(editionId, signal),
      staleTime: STALE_TIME.edition,
    })
  }
}
