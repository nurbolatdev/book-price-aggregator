import { Bell } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { SearchItem } from '../../api/types'
import { buttonClasses } from '../../components/ui/Button'
import { Cover } from '../../components/ui/Cover'
import { focusRing } from '../../components/ui/focusRing'
import { iconButtonClasses } from '../../components/ui/IconButton'
import { PriceTag } from '../../components/ui/PriceTag'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import { usePrefetchEdition } from '../edition/usePrefetchEdition'
import { useStores } from './useStores'
import { useAuthStore } from '../../stores/auth'

const BINDING_TEXT: Record<string, string> = {
  HARDCOVER: 'твёрдый переплёт',
  PAPERBACK: 'мягкая обложка',
}

function metaLine(item: SearchItem): string {
  const publisherYear = [item.publisher, item.year].filter(Boolean).join(', ')
  const rest = [publisherYear, BINDING_TEXT[item.binding ?? '']].filter(Boolean).join(', ')
  return [item.author, rest].filter(Boolean).join(' · ')
}

export function EditionResultCard({ item }: { item: SearchItem }) {
  const prefetch = usePrefetchEdition()
  const { storeName } = useStores()
  const authed = useAuthStore((s) => s.status === 'authenticated')
  const id = item.editionId ?? 0
  const title = item.title ?? ''
  const best = item.bestPrice
  const detailPath = `/editions/${id}`
  const beaconPath = `${detailPath}?beacon=1`
  const beaconHref = authed ? beaconPath : '/login?next=' + encodeURIComponent(beaconPath)

  return (
    <article className="flex gap-5 rounded-card border border-line bg-surface p-5">
      <Cover editionId={id} title={title} coverUrl={item.coverUrl} className="h-40 w-[110px] rounded-lg" />
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <Link
          to={detailPath}
          onMouseEnter={() => prefetch(id)}
          onFocus={() => prefetch(id)}
          className={cn('text-[19px] font-bold text-ink hover:text-link', focusRing)}
        >
          {title}
        </Link>
        <p className="text-sm text-muted">{metaLine(item)}</p>
        <ul className="flex flex-wrap gap-2">
          {(item.offers ?? []).map((offer) => {
            const isBest = offer.store === best?.store && offer.price === best?.price
            return (
              <li
                key={offer.store}
                className={cn(
                  'rounded-lg border px-2.5 py-1.5 text-sm',
                  isBest ? 'border-transparent bg-deal-soft' : 'border-line',
                )}
                data-best={isBest || undefined}
              >
                {storeName(offer.store ?? '')} <strong>{formatPrice(offer.price ?? 0)}</strong>
              </li>
            )
          })}
        </ul>
      </div>
      <div className="flex w-[220px] shrink-0 flex-col gap-1">
        <span className="text-[13px] text-muted">Лучшая цена</span>
        <PriceTag price={best?.price ?? 0} className="text-[26px]" />
        {best?.store && (
          <span className={cn('text-sm', best.inStock ? 'font-semibold text-deal' : 'text-muted')}>
            {storeName(best.store)} · {best.inStock ? 'в наличии' : 'нет в наличии'}
          </span>
        )}
        <div className="mt-auto flex gap-2 pt-3">
          <Link
            to={detailPath}
            onMouseEnter={() => prefetch(id)}
            onFocus={() => prefetch(id)}
            className={buttonClasses({ variant: 'dark', className: 'flex-1' })}
          >
            Сравнить
          </Link>
          <Link
            to={beaconHref}
            aria-label="Поставить маячок"
            className={iconButtonClasses({ className: 'text-accent' })}
          >
            <Bell aria-hidden="true" className="h-5 w-5" />
          </Link>
        </div>
      </div>
    </article>
  )
}
