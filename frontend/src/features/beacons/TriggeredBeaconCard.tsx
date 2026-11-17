import { Link } from 'react-router-dom'
import type { BeaconResponse } from '../../api/types'
import { Button, buttonClasses } from '../../components/ui/Button'
import { ExternalLink } from '../../components/ui/ExternalLink'
import { focusRing } from '../../components/ui/focusRing'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import { formatTriggered, goalText } from './beaconFormat'

interface TriggeredBeaconCardProps {
  beacon: BeaconResponse
  storeName: (code: string) => string
  now?: Date
  onFollow: (beacon: BeaconResponse) => void
}

export function TriggeredBeaconCard({ beacon, storeName, now, onFollow }: TriggeredBeaconCardProps) {
  const price = beacon.currentBestPrice != null ? formatPrice(beacon.currentBestPrice) : null
  return (
    <article className="flex flex-col gap-2.5 rounded-card border-2 border-accent bg-accent-soft p-3.5 md:flex-row md:flex-wrap md:items-center md:justify-between md:gap-4 md:p-[18px]">
      <div className="flex flex-col items-start gap-2.5 md:flex-[1_1_320px] md:gap-2">
        <span className="rounded-full bg-accent px-2.5 py-1 text-[13px] font-bold text-white">
          Сработал {beacon.lastTriggeredAt ? formatTriggered(beacon.lastTriggeredAt, now) : ''}
        </span>
        <Link
          to={`/editions/${beacon.editionId}`}
          className={cn('rounded-sm text-base font-bold text-ink hover:underline md:text-lg', focusRing)}
        >
          {beacon.title}
        </Link>
        <p className="text-sm">
          {goalText(beacon)}
          {price && (
            <>
              {' · сейчас '}
              <strong>
                {price}
                {beacon.currentBestStore ? ` на ${storeName(beacon.currentBestStore)}` : ''}
              </strong>
            </>
          )}
        </p>
      </div>
      <div className="grid grid-cols-2 gap-2 md:flex">
        {beacon.currentBestUrl && price && (
          <ExternalLink
            href={beacon.currentBestUrl}
            className={cn(buttonClasses({ variant: 'primary', size: 'lg' }), 'whitespace-nowrap')}
          >
            Купить за {price}
          </ExternalLink>
        )}
        <Button variant="secondary" size="lg" className="whitespace-nowrap" onClick={() => onFollow(beacon)}>
          Следить дальше
        </Button>
      </div>
    </article>
  )
}
