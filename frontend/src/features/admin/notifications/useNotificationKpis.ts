import { useQuery } from '@tanstack/react-query'
import { fetchAdminNotifications } from '../../../api/admin'
import { queryKeys } from '../../../api/queryKeys'

const QUEUED = { status: 'QUEUED', page: 0, size: 1 } as const
const QUEUED_DIGEST = {
  status: 'QUEUED',
  delivery: 'DIGEST',
  page: 0,
  size: 1,
} as const

export const QUEUED_COUNT_KEYS = [
  queryKeys.admin.notifications(QUEUED),
  queryKeys.admin.notifications(QUEUED_DIGEST),
] as const

/** The stats endpoint has no queue counters, so they come from two one-row log requests. */
export function useNotificationKpis() {
  const queued = useQuery({
    queryKey: queryKeys.admin.notifications(QUEUED),
    queryFn: ({ signal }) => fetchAdminNotifications(QUEUED, signal),
  })
  const queuedDigest = useQuery({
    queryKey: queryKeys.admin.notifications(QUEUED_DIGEST),
    queryFn: ({ signal }) => fetchAdminNotifications(QUEUED_DIGEST, signal),
  })
  return { queued, queuedDigest }
}
