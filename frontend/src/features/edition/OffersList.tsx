import type { EditionDetailResponse, OfferInfo } from '../../api/types'
import { ExternalLink } from '../../components/ui/ExternalLink'
import { focusRing } from '../../components/ui/focusRing'
import { cn } from '../../lib/cn'
import { formatPrice, formatRelative } from '../../lib/format'
import { bestOfferId, sortOffers } from './offers'

function OfferCard({ offer, best }: { offer: OfferInfo; best: boolean }) {
  const inStock = offer.inStock !== false
  const parts: string[] = []
  if (offer.deliveryText) parts.push(offer.deliveryText)
  if (best && offer.lastSeenAt) parts.push(`обновлено ${formatRelative(offer.lastSeenAt)}`)
  if (!inStock) parts.push('нет в наличии')
  const className = cn(
    'flex items-center justify-between rounded-xl px-3.5 py-3 text-ink',
    focusRing,
    best ? 'border-2 border-deal bg-best-row' : 'border border-line bg-surface',
  )
  const content = (
    <>
      <span className="flex min-w-0 flex-col gap-0.5">
        <strong>{offer.storeName}</strong>
        {parts.length > 0 && <span className="text-[13px] text-muted">{parts.join(' · ')}</span>}
      </span>
      {offer.price != null && <span className="text-lg font-bold">{formatPrice(offer.price)}</span>}
    </>
  )
  if (!offer.url) {
    return (
      <div className={className} data-best={best || undefined}>
        {content}
      </div>
    )
  }
  return (
    <ExternalLink
      href={offer.url}
      className={className}
      data-best={best || undefined}
    >
      {content}
    </ExternalLink>
  )
}

export function OffersList({ edition }: { edition: EditionDetailResponse }) {
  const offers = sortOffers(edition.offers)
  const bestId = bestOfferId(edition.offers, edition.bestPrice)
  return (
    <div className="flex flex-col gap-2">
      {offers.map((o) => (
        <OfferCard key={o.offerId} offer={o} best={o.offerId === bestId} />
      ))}
    </div>
  )
}
