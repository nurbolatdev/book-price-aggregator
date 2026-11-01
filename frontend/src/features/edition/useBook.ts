import { useQuery } from '@tanstack/react-query'
import { fetchBook } from '../../api/editions'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

export function useBook(id: number) {
  return useQuery({
    queryKey: queryKeys.book(id),
    queryFn: ({ signal }) => fetchBook(id, signal),
    staleTime: STALE_TIME.edition,
    enabled: id > 0,
  })
}
