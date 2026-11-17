import { useQuery } from '@tanstack/react-query'
import { fetchBeacons } from '../../api/beacons'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'
import { useAuthStore } from '../../stores/auth'

export function useBeacons() {
  const status = useAuthStore((s) => s.status)
  return useQuery({
    queryKey: queryKeys.beacons,
    queryFn: ({ signal }) => fetchBeacons(signal),
    enabled: status === 'authenticated',
    staleTime: STALE_TIME.search,
  })
}
