import { EmptyState } from '../../components/ui/EmptyState'
import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { Skeleton } from '../../components/ui/Skeleton'
import { formatNumber } from '../../lib/format'
import { AdminPageHeader } from '../../features/admin/AdminPageHeader'
import { AdminSection } from '../../features/admin/AdminSection'
import { OfferList } from '../../features/admin/matching/OfferList'
import { OfferPanel } from '../../features/admin/matching/OfferPanel'
import { PENDING_PAGE_SIZE, useMatching } from '../../features/admin/matching/useMatching'
import { useAdminStats } from '../../features/admin/overview/useOverviewQueries'
import { Pagination } from '../../features/admin/Pagination'
import { useAdminPageParam } from '../../features/admin/useAdminPageParam'
import { useStepBackWhenEmpty } from '../../features/admin/useStepBackWhenEmpty'

export default function MatchingPage() {
  const [page, changePage] = useAdminPageParam()
  const { query, selected, select, match, reject, pending } = useMatching(page)
  const stats = useAdminStats('24h')
  const catalog = stats.data?.catalog
  const data = query.data
  const items = data?.content ?? []
  useStepBackWhenEmpty(data, query.isPlaceholderData, page, PENDING_PAGE_SIZE, changePage)

  return (
    <>
      <AdminPageHeader
        title="Каталог и сопоставление"
        subtitle={
          catalog
            ? `Книг ${formatNumber(catalog.books ?? 0)} · изданий ${formatNumber(catalog.editions ?? 0)} · сопоставлено предложений ${formatNumber(catalog.matchedOffers ?? 0)}`
            : undefined
        }
      />

      {query.isPending && <Skeleton className="h-96 w-full" />}
      {query.isError && !data && <ErrorBlock onRetry={() => void query.refetch()} />}
      {data && items.length === 0 && (data.totalElements ?? 0) === 0 && (
        <AdminSection>
          <EmptyState title="Все предложения проверены" />
        </AdminSection>
      )}
      {data && items.length > 0 && (
        <div className="flex flex-wrap items-start gap-[22px]">
          <AdminSection
            title={`Ждут проверки · ${formatNumber(data.totalElements ?? 0)}`}
            className="max-w-[420px] flex-[1_1_340px]"
          >
            <p className="text-sm text-muted">
              Предложения, которые парсер не смог уверенно привязать к изданию.
            </p>
            <OfferList items={items} selectedId={selected?.id ?? null} onSelect={select} />
            <Pagination
              page={data.page ?? page}
              size={PENDING_PAGE_SIZE}
              totalElements={data.totalElements ?? 0}
              totalPages={data.totalPages ?? 0}
              onChange={changePage}
              caption={(from, to, total) => `${from}–${to} из ${total}`}
            />
          </AdminSection>
          <AdminSection className="flex-[999_1_420px]">
            {selected && (
              <OfferPanel
                key={selected.id}
                offer={selected}
                pending={pending}
                onMatch={(id) => void match(id)}
                onReject={() => void reject()}
              />
            )}
          </AdminSection>
        </div>
      )}
    </>
  )
}
