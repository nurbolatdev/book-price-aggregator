import { Pause, Pencil, Play, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { BeaconResponse } from '../../api/types'
import { Cover } from '../../components/ui/Cover'
import { iconButtonClasses } from '../../components/ui/IconButton'
import { focusRing } from '../../components/ui/focusRing'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import { useMediaQuery } from '../../lib/useMediaQuery'
import { goalText, priceCaption } from './beaconFormat'
import { StatusPill } from './StatusPill'

interface BeaconRowProps {
  beacon: BeaconResponse
  now?: Date
  onEdit: (beacon: BeaconResponse) => void
  onToggle: (beacon: BeaconResponse) => void
  onDelete: (beacon: BeaconResponse) => void
}

export function BeaconRow({ beacon, now, onEdit, onToggle, onDelete }: BeaconRowProps) {
  const desktop = useMediaQuery('(min-width: 768px)')
  const paused = beacon.status === 'PAUSED'
  const pending = (beacon.id ?? 0) < 0
  const title = beacon.title ?? ''
  const status = beacon.status ?? 'ACTIVE'
  const price = beacon.currentBestPrice != null ? formatPrice(beacon.currentBestPrice) : '—'
  const caption = priceCaption(beacon, now)

  const buttons = (
    <div className="ml-auto flex shrink-0 gap-1.5 md:ml-0">
      <button
        type="button"
        aria-label="Изменить условие"
        disabled={pending}
        className={cn(iconButtonClasses(), 'disabled:opacity-50')}
        onClick={() => onEdit(beacon)}
      >
        <Pencil size={18} aria-hidden="true" />
      </button>
      <button
        type="button"
        aria-label={paused ? 'Возобновить' : 'Поставить на паузу'}
        disabled={pending}
        className={cn(iconButtonClasses(), 'disabled:opacity-50')}
        onClick={() => onToggle(beacon)}
      >
        {paused ? <Play size={18} aria-hidden="true" /> : <Pause size={18} aria-hidden="true" />}
      </button>
      <button
        type="button"
        aria-label="Удалить маячок"
        disabled={pending}
        className={cn(iconButtonClasses(), 'disabled:opacity-50')}
        onClick={() => onDelete(beacon)}
      >
        <Trash2 size={18} aria-hidden="true" />
      </button>
    </div>
  )

  const titleLink = (
    <Link
      to={`/editions/${beacon.editionId}`}
      className={cn('rounded-sm text-base font-bold text-ink hover:underline md:text-[17px]', focusRing)}
    >
      {title}
    </Link>
  )

  if (!desktop) {
    return (
      <article className="flex flex-col gap-3 rounded-card border border-line bg-surface p-3.5">
        <div className="flex gap-3">
          <Cover
            editionId={beacon.editionId ?? 0}
            title={title}
            coverUrl={beacon.coverUrl}
            size="result"
            className="h-[74px] w-[52px] rounded-md"
          />
          <div className="flex min-w-0 flex-1 flex-col gap-1">
            <div className="flex items-start justify-between gap-2">
              {titleLink}
              <StatusPill status={status} compact />
            </div>
            <span className="text-[13px] text-muted">{beacon.author}</span>
            <span className="text-[13px]">{goalText(beacon)}</span>
          </div>
        </div>
        <div className="flex items-center gap-3 border-t border-divider pt-3">
          <div className="flex min-w-0 flex-col">
            <span className="text-lg font-bold">{price}</span>
            <span className="text-xs text-muted">{caption}</span>
          </div>
          {buttons}
        </div>
      </article>
    )
  }

  return (
    <article className="flex flex-wrap items-center gap-[18px] rounded-card border border-line bg-surface p-[18px]">
      <Cover
        editionId={beacon.editionId ?? 0}
        title={title}
        coverUrl={beacon.coverUrl}
        size="result"
        className="h-[92px] w-16 rounded-md"
      />
      <div className="flex min-w-0 flex-[1_1_240px] flex-col gap-1">
        {titleLink}
        <span className="text-sm text-muted">{beacon.author}</span>
        <span className="text-sm">{goalText(beacon)}</span>
      </div>
      <div className="flex flex-[0_1_180px] flex-col">
        <span className="text-[13px] text-muted">Сейчас лучшая</span>
        <span className="text-xl font-bold">{price}</span>
        <span className="text-[13px] text-ink-soft">{caption}</span>
      </div>
      <StatusPill status={status} />
      {buttons}
    </article>
  )
}
