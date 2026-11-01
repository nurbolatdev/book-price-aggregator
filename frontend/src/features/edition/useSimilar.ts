import { useQuery } from '@tanstack/react-query'
import { fetchSimilar } from '../../api/editions'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

const SIMILAR_LIMIT = 4

export function useSimilar(id: number) {
  return useQuery({
    queryKey: queryKeys.similar(id, SIMILAR_LIMIT),
    queryFn: ({ signal }) => fetchSimilar(id, SIMILAR_LIMIT, signal),
    staleTime: STALE_TIME.reference,
    enabled: id > 0,
  })
}
