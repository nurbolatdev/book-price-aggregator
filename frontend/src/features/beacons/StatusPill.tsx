import { cn } from '../../lib/cn'
import type { BeaconStatus } from '../../api/types'

const LOOK: Record<BeaconStatus, { label: string; classes: string }> = {
  TRIGGERED: { label: 'Сработал', classes: 'bg-accent text-white' },
  ACTIVE: { label: 'Ждёт', classes: 'bg-status-soft text-ink-soft' },
  PAUSED: { label: 'На паузе', classes: 'bg-paused-soft text-muted' },
}

export function StatusPill({ status, compact = false }: { status: BeaconStatus; compact?: boolean }) {
  const look = LOOK[status]
  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center whitespace-nowrap rounded-full font-semibold',
        compact ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1.5 text-[13px]',
        look.classes,
      )}
    >
      {look.label}
    </span>
  )
}
