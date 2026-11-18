import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { fetchAdminNotifications } from '../../api/admin'
import { queryKeys } from '../../api/queryKeys'
import type { NotificationStatus } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { EmptyState } from '../../components/ui/EmptyState'
import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { Skeleton } from '../../components/ui/Skeleton'
import { formatNumber } from '../../lib/format'
import { AdminPageHeader } from '../../features/admin/AdminPageHeader'
import { AdminSection } from '../../features/admin/AdminSection'
import { FilterChips } from '../../features/admin/FilterChips'
import { KpiCard, KpiGrid } from '../../features/admin/KpiCard'
import { NotificationsTable } from '../../features/admin/notifications/NotificationsTable'
import { useNotificationKpis } from '../../features/admin/notifications/useNotificationKpis'
import { useRetryNotification } from '../../features/admin/notifications/useRetryNotification'
import { useAdminStats } from '../../features/admin/overview/useOverviewQueries'
import { Pagination } from '../../features/admin/Pagination'
import { useAdminPageParam } from '../../features/admin/useAdminPageParam'
import { useStepBackWhenEmpty } from '../../features/admin/useStepBackWhenEmpty'

const PAGE_SIZE = 20

type StatusFilter = 'all' | NotificationStatus

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: 'Все' },
  { value: 'QUEUED', label: 'В очереди' },
  { value: 'SENT', label: 'Отправлено' },
  { value: 'FAILED', label: 'Ошибка' },
]

function parseStatus(raw: string | null): StatusFilter {
  return raw === 'QUEUED' || raw === 'SENT' || raw === 'FAILED' ? raw : 'all'
}

export default function NotificationsPage() {
  const [params, setParams] = useSearchParams()
  const statusFilter = parseStatus(params.get('status'))
  const status = statusFilter === 'all' ? undefined : statusFilter
  const [page, changePage] = useAdminPageParam()

  function changeStatus(value: StatusFilter) {
    setParams((prev) => {
      const out = new URLSearchParams(prev)
      if (value === 'all') out.delete('status')
      else out.set('status', value)
      out.delete('page')
      return out
    })
  }

  const list = useQuery({
    queryKey: queryKeys.admin.notifications({ status, page, size: PAGE_SIZE }),
    queryFn: ({ signal }) => fetchAdminNotifications({ status, page, size: PAGE_SIZE }, signal),
    placeholderData: keepPreviousData,
  })
  const stats = useAdminStats('24h')
  const { queued, queuedDigest } = useNotificationKpis()
  const actions = useRetryNotification()

  useStepBackWhenEmpty(list.data, list.isPlaceholderData, page, PAGE_SIZE, changePage)
  const items = list.data?.content ?? []

  return (
    <>
      <AdminPageHeader
        title="Маячки и письма"
        subtitle="За 24 часа"
        actions={
          <Button
            variant="secondary"
            size="sm"
            loading={list.isFetching && !list.isPending}
            onClick={() => void list.refetch()}
          >
            Обновить
          </Button>
        }
      />

      {stats.isError && !stats.data ? (
        <ErrorBlock onRetry={() => void stats.refetch()} />
      ) : (
        <KpiGrid>
          <KpiCard
            label="Активные маячки"
            value={stats.data ? formatNumber(stats.data.beacons?.active ?? 0) : '…'}
            caption={
              stats.data
                ? `сработали за 24 ч: ${formatNumber(stats.data.beacons?.triggeredInPeriod ?? 0)}`
                : undefined
            }
          />
          <KpiCard
            label="Писем отправлено за 24 ч"
            value={stats.data ? formatNumber(stats.data.emails?.sent ?? 0) : '…'}
            caption={
              stats.data ? `ошибок: ${formatNumber(stats.data.emails?.failed ?? 0)}` : undefined
            }
          />
          <KpiCard
            label="Ждут отправки"
            value={queued.data ? formatNumber(queued.data.totalElements ?? 0) : '…'}
            caption={
              queuedDigest.data
                ? `из них в сводке: ${formatNumber(queuedDigest.data.totalElements ?? 0)}`
                : undefined
            }
          />
        </KpiGrid>
      )}

      <AdminSection
        title="Журнал писем"
        actions={
          <FilterChips
            label="Статус письма"
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={changeStatus}
          />
        }
      >
        {list.isPending && <Skeleton className="h-72 w-full" />}
        {list.isError && !list.data && <ErrorBlock onRetry={() => void list.refetch()} />}
        {list.data && items.length === 0 && <EmptyState title="Писем нет" />}
        {list.data && items.length > 0 && (
          <>
            <NotificationsTable items={items} actions={actions} />
            <Pagination
              page={list.data.page ?? page}
              size={PAGE_SIZE}
              totalElements={list.data.totalElements ?? 0}
              totalPages={list.data.totalPages ?? 0}
              onChange={changePage}
            />
          </>
        )}
      </AdminSection>
    </>
  )
}
