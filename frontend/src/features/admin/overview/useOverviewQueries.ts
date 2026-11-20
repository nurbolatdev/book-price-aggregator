import { useQuery } from '@tanstack/react-query'
import {
  fetchAdminActivity,
  fetchAdminEvents,
  fetchAdminHealth,
  fetchAdminQueues,
  fetchAdminStats,
  fetchTopQueries,
} from '../../../api/admin'
import { queryKeys } from '../../../api/queryKeys'
import type { AdminPeriod } from '../../../api/types'
import { STALE_TIME } from '../../../app/queryClient'
import { ADMIN_POLL } from '../polling'

export function useAdminStats(period: AdminPeriod) {
  return useQuery({
    queryKey: queryKeys.admin.stats(period),
    queryFn: ({ signal }) => fetchAdminStats(period, signal),
    ...ADMIN_POLL,
  })
}

export function useAdminHealth() {
  return useQuery({
    queryKey: queryKeys.admin.health,
    queryFn: ({ signal }) => fetchAdminHealth(signal),
    ...ADMIN_POLL,
  })
}

export function useAdminQueues() {
  return useQuery({
    queryKey: queryKeys.admin.queues,
    queryFn: ({ signal }) => fetchAdminQueues(signal),
    ...ADMIN_POLL,
  })
}

export function useAdminActivity(days: number) {
  return useQuery({
    queryKey: queryKeys.admin.activity(days),
    queryFn: ({ signal }) => fetchAdminActivity(days, signal),
    staleTime: STALE_TIME.search,
  })
}

export function useRecentEvents(size: number) {
  return useQuery({
    queryKey: queryKeys.admin.recentEvents(size),
    queryFn: ({ signal }) => fetchAdminEvents({ page: 0, size }, signal),
    staleTime: STALE_TIME.search,
  })
}

export function useTopQueries(period: AdminPeriod) {
  return useQuery({
    queryKey: queryKeys.admin.topQueries(period),
    queryFn: ({ signal }) => fetchTopQueries(period, signal),
    staleTime: STALE_TIME.search,
  })
}
