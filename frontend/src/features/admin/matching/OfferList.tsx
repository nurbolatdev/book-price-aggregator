import type { ReviewOffer } from '../../../api/types'
import { cn } from '../../../lib/cn'
import { formatPrice } from '../../../lib/format'
import { focusRing } from '../../../components/ui/focusRing'
import { useStores } from '../../search/useStores'
import { formatFound } from '../adminFormat'

interface OfferListProps {
  items: ReviewOffer[]
  selectedId: number | null
  onSelect: (id: number) => void
}

export function OfferList({ items, selectedId, onSelect }: OfferListProps) {
  const { storeName } = useStores()
  const now = new Date()
  return (
    <ul className="flex flex-col gap-2.5">
      {items.map((offer) => {
        const selected = offer.id === selectedId
        return (
          <li key={offer.id}>
            <button
              type="button"
              aria-pressed={selected}
              onClick={() => offer.id !== undefined && onSelect(offer.id)}
              className={cn(
                'flex w-full flex-col gap-1 rounded-xl p-3 text-left',
                focusRing,
                selected ? 'border-2 border-ink' : 'border border-line',
              )}
            >
              <span className="text-xs font-semibold text-muted">
                {storeName(offer.store ?? '')}
                {offer.firstSeenAt ? ` · ${formatFound(offer.firstSeenAt, now)}` : ''}
              </span>
              <span className="font-semibold">{offer.title}</span>
              {offer.price !== undefined && (
                <span className="font-bold">{formatPrice(offer.price)}</span>
              )}
            </button>
          </li>
        )
      })}
    </ul>
  )
}
