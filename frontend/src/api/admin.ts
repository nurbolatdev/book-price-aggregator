import { api } from './client'
import type {
  AdminActivityDay,
  AdminEventPage,
  AdminHealth,
  AdminParser,
  AdminPeriod,
  AdminStats,
  EventLevel,
  ParserRunQueued,
  QueueStats,
  SearchQueryStat,
  StoreCode,
  StoreState,
} from './types'

export async function fetchAdminStats(
  period: AdminPeriod,
  signal?: AbortSignal,
): Promise<AdminStats> {
  const { data } = await api.get<AdminStats>('/admin/stats', {
    params: { period },
    signal,
  })
  return data
}

export async function fetchAdminHealth(signal?: AbortSignal): Promise<AdminHealth> {
  const { data } = await api.get<AdminHealth>('/admin/health', { signal })
  return data
}

export async function fetchAdminParsers(signal?: AbortSignal): Promise<AdminParser[]> {
  const { data } = await api.get<AdminParser[]>('/admin/parsers', { signal })
  return data
}

export async function runParser(code: StoreCode): Promise<ParserRunQueued> {
  const { data } = await api.post<ParserRunQueued>(`/admin/parsers/${code}/run`)
  return data
}

export async function fetchAdminQueues(signal?: AbortSignal): Promise<QueueStats[]> {
  const { data } = await api.get<QueueStats[]>('/admin/queues', { signal })
  return data
}

export async function fetchAdminActivity(
  days: number,
  signal?: AbortSignal,
): Promise<AdminActivityDay[]> {
  const { data } = await api.get<AdminActivityDay[]>('/admin/activity', {
    params: { days },
    signal,
  })
  return data
}

export interface AdminEventsParams {
  level?: EventLevel
  page: number
  size: number
}

export async function fetchAdminEvents(
  { level, page, size }: AdminEventsParams,
  signal?: AbortSignal,
): Promise<AdminEventPage> {
  const { data } = await api.get<AdminEventPage>('/admin/events', {
    params: { ...(level ? { level } : {}), page, size },
    signal,
  })
  return data
}

export async function fetchTopQueries(
  period: AdminPeriod,
  signal?: AbortSignal,
): Promise<SearchQueryStat[]> {
  const { data } = await api.get<SearchQueryStat[]>('/admin/search/top', {
    params: { period },
    signal,
  })
  return data
}

export async function setStoreEnabled(code: StoreCode, enabled: boolean): Promise<StoreState> {
  const { data } = await api.patch<StoreState>(`/admin/stores/${code}`, { enabled })
  return data
}
