import { useQuery } from '@tanstack/react-query'
import { fetchStores } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'

export function useStores() {
  const query = useQuery({
    queryKey: queryKeys.stores,
    queryFn: ({ signal }) => fetchStores(signal),
    staleTime: STALE_TIME.reference,
  })
  const stores = query.data ?? []
  const enabledStores = stores.filter((s) => s.enabled)

  function storeName(code: string): string {
    return stores.find((s) => s.code === code)?.name ?? code
  }

  return { stores, enabledStores, storeName, isError: query.isError, isLoading: query.isLoading }
}
