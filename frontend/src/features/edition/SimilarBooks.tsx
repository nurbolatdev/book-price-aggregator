import { Link } from 'react-router-dom'
import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { focusRing } from '../../components/ui/focusRing'
import { PriceTag } from '../../components/ui/PriceTag'
import { Skeleton } from '../../components/ui/Skeleton'
import { cn } from '../../lib/cn'
import { usePrefetchEdition } from './usePrefetchEdition'
import { useSimilar } from './useSimilar'

const grid = 'grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-4'

export function SimilarBooks({ editionId }: { editionId: number }) {
  const { data, isPending, isError, refetch } = useSimilar(editionId)
  const prefetch = usePrefetchEdition()
  if (data && data.length === 0) return null

  return (
    <section aria-labelledby="similar-heading" className="flex flex-col gap-4">
      <h2 id="similar-heading" className="font-display text-xl font-bold text-ink">
        Похожие книги
      </h2>
      {isError ? (
        <ErrorBlock message="Не удалось загрузить похожие книги." onRetry={() => void refetch()} />
      ) : isPending ? (
        <div className={grid} role="status" aria-label="Загружаем похожие книги">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-[104px] rounded-[14px]" />
          ))}
        </div>
      ) : (
        <ul className={grid}>
          {data.map((s) => {
            const id = s.editionId ?? 0
            return (
              <li key={id} className="flex">
                <Link
                  to={`/editions/${id}`}
                  className={cn(
                    'flex w-full flex-col gap-1.5 rounded-[14px] border border-line bg-surface p-3.5 text-ink',
                    focusRing,
                  )}
                  onMouseEnter={() => prefetch(id)}
                  onFocus={() => prefetch(id)}
                >
                  <strong>{s.title}</strong>
                  {s.author && <span className="text-sm text-muted">{s.author}</span>}
                  {s.bestPrice != null && <PriceTag price={s.bestPrice} prefix="от " className="text-base" />}
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
