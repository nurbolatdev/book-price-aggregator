import { useQueryClient } from '@tanstack/react-query'
import { fetchEdition } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'
import { loadEditionPage } from './loadEditionPage'

export function usePrefetchEdition(): (editionId: number) => void {
  const queryClient = useQueryClient()
  return (editionId) => {
    void loadEditionPage()
    void queryClient.prefetchQuery({
      queryKey: queryKeys.edition(editionId),
      queryFn: ({ signal }) => fetchEdition(editionId, signal),
      staleTime: STALE_TIME.edition,
    })
  }
}
