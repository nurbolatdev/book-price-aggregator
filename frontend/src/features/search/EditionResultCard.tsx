import { Bell } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { SearchItem } from '../../api/types'
import { buttonClasses } from '../../components/ui/Button'
import { Cover } from '../../components/ui/Cover'
import { focusRing } from '../../components/ui/focusRing'
import { iconButtonClasses } from '../../components/ui/IconButton'
import { PriceTag } from '../../components/ui/PriceTag'
import { cn } from '../../lib/cn'
import { bindingName, formatPrice, pluralRu } from '../../lib/format'
import { usePrefetchEdition } from '../edition/usePrefetchEdition'
import { useStores } from './useStores'
import { useAuthStore } from '../../stores/auth'

const BINDING_TEXT: Record<string, string> = {
  HARDCOVER: 'твёрдый переплёт',
  PAPERBACK: 'мягкая обложка',
}

function metaLine(item: SearchItem): string {
  const publisherYear = [item.publisher, item.year].filter(Boolean).join(', ')
  const rest = [publisherYear, BINDING_TEXT[item.binding ?? '']]
    .filter(Boolean)
    .join(', ')
  return [item.author, rest].filter(Boolean).join(' · ')
}

function phoneMeta(item: SearchItem): string {
  const n = item.storesCount ?? item.offers?.length ?? 0
  return [
    [item.publisher, item.year].filter(Boolean).join(' '),
    item.binding && BINDING_TEXT[item.binding]
      ? bindingName(item.binding).toLowerCase()
      : '',
    n > 0 ? `${n} ${pluralRu(n, ['магазин', 'магазина', 'магазинов'])}` : '',
  ]
    .filter(Boolean)
    .join(' · ')
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
  const beaconHref = authed
    ? beaconPath
    : '/login?next=' + encodeURIComponent(beaconPath)

  return (
    <article className="grid grid-cols-[64px_minmax(0,1fr)_auto] gap-x-3 gap-y-1 rounded-card border border-line bg-surface p-3 md:flex md:gap-5 md:p-5">
      <Cover
        editionId={id}
        title={title}
        coverUrl={item.coverUrl}
        size="result"
        className="row-span-3 row-start-1 h-[92px] w-16 rounded-md md:h-40 md:w-[110px] md:rounded-lg"
      />
      <div className="flex min-w-0 flex-1 flex-col gap-2 max-md:contents">
        <Link
          to={detailPath}
          onMouseEnter={() => prefetch(id)}
          onFocus={() => prefetch(id)}
          className={cn(
            'text-base font-bold text-ink hover:text-link max-md:col-span-2 md:text-[19px]',
            focusRing,
          )}
        >
          {title}
        </Link>
        <p className="hidden text-sm text-muted md:block">{metaLine(item)}</p>
        <p className="text-[13px] text-muted max-md:col-span-2 md:hidden">
          {phoneMeta(item)}
        </p>
        <ul className="hidden flex-wrap gap-2 md:flex">
          {(item.offers ?? []).map((offer) => {
            const isBest =
              offer.store === best?.store && offer.price === best?.price
            return (
              <li
                key={offer.store}
                className={cn(
                  'rounded-lg border px-2.5 py-1.5 text-sm',
                  isBest ? 'border-transparent bg-deal-soft' : 'border-line',
                )}
                data-best={isBest || undefined}
              >
                {storeName(offer.store ?? '')}{' '}
                <strong>{formatPrice(offer.price ?? 0)}</strong>
              </li>
            )
          })}
        </ul>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1 text-right max-md:contents md:w-[220px] md:items-stretch md:text-left">
        <span className="hidden text-[13px] text-muted md:block">
          Лучшая цена
        </span>
        <div className="flex items-baseline gap-1.5 self-end max-md:col-start-2 max-md:row-start-3 md:contents">
          <PriceTag
            price={best?.price ?? 0}
            className="text-lg md:text-[26px]"
          />
          {best?.store && (
            <span
              className={cn(
                'text-[13px] md:text-sm',
                best.inStock ? 'font-semibold text-deal' : 'text-muted',
              )}
            >
              {storeName(best.store)}
              <span className="max-md:hidden">
                {' '}
                · {best.inStock ? 'в наличии' : 'нет в наличии'}
              </span>
            </span>
          )}
        </div>
        <div className="flex justify-end gap-2 max-md:col-start-3 max-md:row-start-3 max-md:self-end md:mt-auto md:justify-start md:pt-3">
          <Link
            to={detailPath}
            onMouseEnter={() => prefetch(id)}
            onFocus={() => prefetch(id)}
            className={buttonClasses({
              variant: 'dark',
              className: 'hidden flex-1 md:inline-flex',
            })}
          >
            Сравнить
          </Link>
          <Link
            to={beaconHref}
            aria-label="Поставить маячок"
            className={iconButtonClasses({ tone: 'accent' })}
          >
            <Bell aria-hidden="true" className="h-5 w-5" />
          </Link>
        </div>
      </div>
    </article>
  )
}
