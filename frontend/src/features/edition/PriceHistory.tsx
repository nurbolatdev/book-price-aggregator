import { lazy, Suspense, useState } from 'react'
import type { PricePeriod } from '../../api/types'
import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { Skeleton } from '../../components/ui/Skeleton'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import { usePriceHistory } from './usePriceHistory'

const PriceHistoryChart = lazy(() => import('./PriceHistoryChart'))

const CHART_BOX = 'aspect-[10/3] min-h-[200px] w-full'

const PERIODS: { value: PricePeriod; label: string }[] = [
  { value: '1M', label: '1 мес' },
  { value: '3M', label: '3 мес' },
  { value: '6M', label: '6 мес' },
]

export function PriceHistory({ editionId }: { editionId: number }) {
  const [period, setPeriod] = useState<PricePeriod>('3M')
  const { data, isPending, isError, isPlaceholderData, refetch } = usePriceHistory(editionId, period)
  const points = data?.points ?? []
  const enough = points.length >= 2

  return (
    <section
      aria-labelledby="history-title"
      aria-busy={isPlaceholderData || undefined}
      className="flex flex-col gap-4 rounded-card border border-line bg-surface p-[22px]"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="history-title" className="font-display text-lg font-medium md:text-[22px]">
          История минимальной цены
        </h2>
        <div role="group" aria-label="Период" className="flex gap-1 rounded-control bg-canvas p-1">
          {PERIODS.map((p) => (
            <button
              key={p.value}
              type="button"
              aria-pressed={period === p.value}
              onClick={() => setPeriod(p.value)}
              className={cn(
                'min-h-touch rounded-lg px-3 text-sm',
                period === p.value ? 'bg-surface font-semibold' : 'text-muted',
              )}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {isPending && (
        <>
          <Skeleton className="h-5 w-72 max-w-full" />
          <Skeleton className={CHART_BOX} />
        </>
      )}

      {isError && !data && (
        <ErrorBlock message="Не удалось загрузить историю цены." onRetry={() => void refetch()} />
      )}

      {data && !enough && (
        <p className="text-sm text-muted">История появится после нескольких проверок цены</p>
      )}

      {data && enough && (
        <>
          <p className="flex flex-wrap gap-x-6 gap-y-1 text-sm">
            <span>
              Минимум: <strong>{data.min != null ? formatPrice(data.min) : '—'}</strong>
            </span>
            <span>
              Средняя: <strong>{data.avg != null ? formatPrice(data.avg) : '—'}</strong>
            </span>
            <span>
              Максимум: <strong>{data.max != null ? formatPrice(data.max) : '—'}</strong>
            </span>
          </p>
          <Suspense fallback={<Skeleton className={CHART_BOX} />}>
            <PriceHistoryChart points={points} period={period} className={CHART_BOX} />
          </Suspense>
        </>
      )}
    </section>
  )
}
