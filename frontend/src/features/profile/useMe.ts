import { useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMe } from '../../api/me'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'
import { useAuthStore } from '../../stores/auth'
import { writeUser } from './userCache'

export function useMe() {
  const queryClient = useQueryClient()
  const status = useAuthStore((s) => s.status)
  const user = useAuthStore((s) => s.user)
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: async ({ signal }) => {
      const me = await fetchMe(signal)
      writeUser(queryClient, me)
      return me
    },
    enabled: status === 'authenticated',
    initialData: user ?? undefined,
    initialDataUpdatedAt: 0,
    staleTime: STALE_TIME.search,
  })
}
