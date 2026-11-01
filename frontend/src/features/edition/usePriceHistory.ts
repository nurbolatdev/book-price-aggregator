import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchPriceHistory } from '../../api/editions'
import { queryKeys } from '../../api/queryKeys'
import type { PricePeriod } from '../../api/types'
import { STALE_TIME } from '../../app/queryClient'

export function usePriceHistory(id: number, period: PricePeriod = '3M') {
  return useQuery({
    queryKey: queryKeys.priceHistory(id, period),
    queryFn: ({ signal }) => fetchPriceHistory(id, period, signal),
    staleTime: STALE_TIME.edition,
    placeholderData: keepPreviousData,
    enabled: id > 0,
  })
}
