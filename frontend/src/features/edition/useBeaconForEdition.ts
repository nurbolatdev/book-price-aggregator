import { useQuery } from '@tanstack/react-query'
import { fetchBeaconForEdition } from '../../api/beacons'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'
import { useAuthStore } from '../../stores/auth'

export function useBeaconForEdition(id: number) {
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: queryKeys.beaconForEdition(id),
    queryFn: ({ signal }) => fetchBeaconForEdition(id, signal),
    staleTime: STALE_TIME.search,
    enabled: id > 0 && status === 'authenticated',
  })
}
