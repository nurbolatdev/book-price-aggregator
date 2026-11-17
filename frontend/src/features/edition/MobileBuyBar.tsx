import { Bell } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { EditionDetailResponse } from '../../api/types'
import { buttonClasses } from '../../components/ui/Button'
import { ExternalLink } from '../../components/ui/ExternalLink'
import { formatPrice } from '../../lib/format'
import { storeNameOf } from './offers'
import { useBeaconAction } from './useBeaconAction'

interface MobileBuyBarProps {
  edition: EditionDetailResponse
  editionId: number
}

export function MobileBuyBar({ edition, editionId }: MobileBuyBarProps) {
  const { bestPrice, offers } = edition
  const beacon = useBeaconAction(editionId)
  const beaconClasses = buttonClasses({ variant: 'outline-accent', size: 'bar', className: 'flex-1' })
  const store = storeNameOf(offers, bestPrice)
  const showBuy = bestPrice?.price != null && Boolean(bestPrice.url)
  const label = showBuy ? `${formatPrice(bestPrice.price!)} в ${store}` : ''

  return (
    <div className="sticky bottom-0 z-10 flex gap-2.5 border-t border-line bg-surface px-4 pt-3 pb-[calc(20px+env(safe-area-inset-bottom))]">
      {beacon.kind === 'link' ? (
        <Link to={beacon.href} className={beaconClasses}>
          <Bell size={18} aria-hidden="true" />
          {beacon.shortLabel}
        </Link>
      ) : (
        <button type="button" onClick={beacon.onOpen} className={beaconClasses}>
          <Bell size={18} aria-hidden="true" />
          {beacon.shortLabel}
        </button>
      )}
      {showBuy && (
        <ExternalLink
          href={bestPrice.url}
          aria-label={`Купить за ${label}`}
          className={buttonClasses({ variant: 'dark', size: 'bar', className: 'flex-[1.4] min-w-0' })}
        >
          <span className="truncate">{label}</span>
        </ExternalLink>
      )}
    </div>
  )
}
