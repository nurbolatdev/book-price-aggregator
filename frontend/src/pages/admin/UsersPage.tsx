import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchAdminUsers } from '../../api/admin'
import { queryKeys } from '../../api/queryKeys'
import { Badge } from '../../components/ui/Badge'
import { EmptyState } from '../../components/ui/EmptyState'
import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { Input } from '../../components/ui/Input'
import { Skeleton } from '../../components/ui/Skeleton'
import { formatNumber } from '../../lib/format'
import { useDebounce } from '../../lib/useDebounce'
import { formatDate } from '../../features/admin/adminFormat'
import { AdminPageHeader } from '../../features/admin/AdminPageHeader'
import { AdminSection } from '../../features/admin/AdminSection'
import { FilterChips } from '../../features/admin/FilterChips'
import { useAdminStats } from '../../features/admin/overview/useOverviewQueries'
import { Pagination } from '../../features/admin/Pagination'
import { row, table, tableWrap, td, th } from '../../features/admin/table'
import { useAdminPageParam } from '../../features/admin/useAdminPageParam'

const PAGE_SIZE = 20

type VerifiedFilter = 'all' | 'true' | 'false'

const VERIFIED_OPTIONS: { value: VerifiedFilter; label: string }[] = [
  { value: 'all', label: 'Все' },
  { value: 'true', label: 'Подтверждённые' },
  { value: 'false', label: 'Не подтверждённые' },
]

function parseVerified(raw: string | null): VerifiedFilter {
  return raw === 'true' || raw === 'false' ? raw : 'all'
}

export default function UsersPage() {
  const [params, setParams] = useSearchParams()
  const q = (params.get('q') ?? '').slice(0, 100)
  const verifiedFilter = parseVerified(params.get('verified'))
  const [page, changePage] = useAdminPageParam()

  const [text, setText] = useState(q)
  const debounced = useDebounce(text.trim(), 300)
  const urlQ = useRef(q)
  urlQ.current = q

  useEffect(() => {
    if (debounced === urlQ.current) return
    setParams(
      (prev) => {
        const out = new URLSearchParams(prev)
        if (debounced) out.set('q', debounced)
        else out.delete('q')
        out.delete('page')
        return out
      },
      { replace: true },
    )
  }, [debounced, setParams])

  function changeVerified(value: VerifiedFilter) {
    setParams((prev) => {
      const out = new URLSearchParams(prev)
      if (value === 'all') out.delete('verified')
      else out.set('verified', value)
      out.delete('page')
      return out
    })
  }

  const verified = verifiedFilter === 'all' ? undefined : verifiedFilter === 'true'
  const users = useQuery({
    queryKey: queryKeys.admin.users({ q: q || undefined, verified, page }),
    queryFn: ({ signal }) =>
      fetchAdminUsers({ q: q || undefined, verified, page, size: PAGE_SIZE }, signal),
    placeholderData: keepPreviousData,
  })
  const stats = useAdminStats('7d').data

  const list = users.data?.content ?? []
  const subtitle = stats
    ? `Всего ${formatNumber(stats.users?.total ?? 0)} · новых за неделю ${formatNumber(stats.users?.newInPeriod ?? 0)} · подтвердили почту ${formatNumber(stats.users?.verified ?? 0)}`
    : undefined

  return (
    <>
      <AdminPageHeader title="Пользователи" subtitle={subtitle} />
      <AdminSection>
        <div className="flex flex-wrap items-center gap-3.5">
          <form
            role="search"
            className="min-w-[260px] flex-1"
            onSubmit={(e) => e.preventDefault()}
          >
            <label htmlFor="admin-users-search" className="sr-only">
              Поиск по email или имени
            </label>
            <Input
              id="admin-users-search"
              type="search"
              placeholder="Поиск по email или имени"
              maxLength={100}
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          </form>
          <FilterChips
            label="Фильтр по почте"
            options={VERIFIED_OPTIONS}
            value={verifiedFilter}
            onChange={changeVerified}
          />
        </div>

        {users.isPending && <Skeleton className="h-72 w-full" />}
        {users.isError && !users.data && <ErrorBlock onRetry={() => void users.refetch()} />}
        {users.data && list.length === 0 && <EmptyState title="Никого не нашли" />}
        {users.data && list.length > 0 && (
          <>
            <div className={tableWrap}>
              <table className={`${table} min-w-[760px]`}>
                <thead>
                  <tr>
                    <th scope="col" className={th}>
                      Email
                    </th>
                    <th scope="col" className={th}>
                      Имя
                    </th>
                    <th scope="col" className={th}>
                      Регистрация
                    </th>
                    <th scope="col" className={th}>
                      Почта
                    </th>
                    <th scope="col" className={th}>
                      Маячков
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((u) => (
                    <tr key={u.id} className={row}>
                      <td className={td}>{u.email}</td>
                      <td className={td}>{u.name}</td>
                      <td className={td}>{u.createdAt ? formatDate(u.createdAt) : '—'}</td>
                      <td className={td}>
                        <Badge
                          tone={u.verified ? 'ok' : 'warn'}
                          className="font-semibold whitespace-nowrap"
                        >
                          {u.verified ? 'подтверждён' : 'не подтверждён'}
                        </Badge>
                      </td>
                      <td className={td}>{formatNumber(u.beacons ?? 0)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={users.data.page ?? page}
              size={PAGE_SIZE}
              totalElements={users.data.totalElements ?? 0}
              totalPages={users.data.totalPages ?? 0}
              onChange={changePage}
              caption={(from, to, total) => `Показаны ${from}–${to} из ${total} · по 20 на странице`}
            />
          </>
        )}
      </AdminSection>
    </>
  )
}
