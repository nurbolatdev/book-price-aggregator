import { Bell, ExternalLink as ExternalLinkIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { EditionDetailResponse } from '../../api/types'
import { buttonClasses } from '../../components/ui/Button'
import { ExternalLink } from '../../components/ui/ExternalLink'
import { formatRelative, formatPrice } from '../../lib/format'
import { lastSeen, storeNameOf } from './offers'
import { PriceVerdict } from './PriceVerdict'
import { useBeaconAction } from './useBeaconAction'

interface BestPriceCardProps {
  edition: EditionDetailResponse
  editionId: number
  className?: string
}

export function BestPriceCard({ edition, editionId, className }: BestPriceCardProps) {
  const { bestPrice, offers, priceVerdict } = edition
  const beacon = useBeaconAction(editionId)
  const store = storeNameOf(offers, bestPrice)
  const seen = lastSeen(offers)

  return (
    <aside
      aria-labelledby="best-price-title"
      className={`flex flex-col gap-3.5 rounded-card border border-line bg-surface p-[22px] ${className ?? ''}`}
    >
      <h2 id="best-price-title" className="font-sans text-sm font-normal text-muted">
        Лучшая цена сейчас
      </h2>
      {bestPrice?.price != null ? (
        <>
          <p className="text-[40px] font-bold leading-none text-ink">{formatPrice(bestPrice.price)}</p>
          <p className="text-[15px] text-ink">
            <strong>{store}</strong> · в наличии
            {bestPrice.deliveryText ? ` · доставка ${bestPrice.deliveryText}` : ''}
          </p>
          <PriceVerdict verdict={priceVerdict} size="md" />
          {bestPrice.url && (
            <ExternalLink
              href={bestPrice.url}
              className={buttonClasses({ variant: 'dark', size: 'xl', className: 'w-full' })}
            >
              Перейти в {store}
              <ExternalLinkIcon size={16} aria-hidden="true" />
            </ExternalLink>
          )}
        </>
      ) : (
        <p className="text-base font-semibold text-ink">Сейчас нет в наличии ни в одном магазине</p>
      )}
      <Link
        to={beacon.href}
        className={buttonClasses({
          variant: beacon.hasBeacon ? 'secondary' : 'primary',
          size: 'lg',
          className: 'w-full',
        })}
      >
        <Bell size={18} aria-hidden="true" />
        {beacon.label}
      </Link>
      {seen && (
        <p className="text-[13px] text-muted">
          Обновлено {formatRelative(seen)}. Цена на сайте магазина может отличаться.
        </p>
      )}
    </aside>
  )
}
