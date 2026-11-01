import { Link } from 'react-router-dom'
import type { DropItem } from '../../api/types'
import { Badge } from '../../components/ui/Badge'
import { Cover } from '../../components/ui/Cover'
import { focusRing } from '../../components/ui/focusRing'
import { PriceTag } from '../../components/ui/PriceTag'
import { cn } from '../../lib/cn'
import { pluralRu } from '../../lib/format'
import { usePrefetchEdition } from '../edition/usePrefetchEdition'
import { useStores } from '../search/useStores'

export function DropCard({
  drop,
  className,
}: {
  drop: DropItem
  className?: string
}) {
  const prefetch = usePrefetchEdition()
  const { storeName } = useStores()
  const id = drop.editionId ?? 0
  const title = drop.title ?? ''
  const others = (drop.storesCount ?? 1) - 1
  const store = storeName(drop.store ?? '')
  const storeLine =
    others > 0
      ? `${store} · ещё в ${others} ${pluralRu(others, ['магазине', 'магазинах', 'магазинах'])}`
      : store

  return (
    <Link
      to={`/editions/${id}`}
      onMouseEnter={() => prefetch(id)}
      onFocus={() => prefetch(id)}
      className={cn(
        'flex flex-col gap-3 rounded-[14px] border border-line bg-surface p-2 text-ink max-md:gap-1.5 md:rounded-card md:p-4',
        focusRing,
        className,
      )}
    >
      <Cover
        editionId={id}
        title={title}
        coverUrl={drop.coverUrl}
        size="drop"
        className="h-[150px] w-full rounded-control md:h-[220px]"
      />
      <div className="flex flex-col gap-1">
        <strong className="text-sm md:text-base">{title}</strong>
        <span className="text-xs text-muted md:text-sm">{drop.author}</span>
      </div>
      <div className="flex flex-col items-start gap-1 md:flex-row md:items-center md:justify-between">
        <PriceTag
          price={drop.price ?? 0}
          prefix="от "
          className="text-base md:text-xl"
        />
        <Badge tone="good">−{drop.dropPercent}% за 7 дней</Badge>
      </div>
      <span className="hidden text-[13px] text-muted md:block">
        {storeLine}
      </span>
    </Link>
  )
}
