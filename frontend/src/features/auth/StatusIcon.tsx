import { Check, Clock, Mail, Pause } from 'lucide-react'

export type StatusIconKind = 'mail' | 'expired' | 'success' | 'paused'

export function StatusIcon({ kind }: { kind: StatusIconKind }) {
  if (kind === 'mail') {
    return (
      <span
        aria-hidden="true"
        className="flex h-16 w-16 items-center justify-center rounded-card bg-accent-soft text-accent"
      >
        <Mail size={32} />
      </span>
    )
  }
  if (kind === 'expired') {
    return (
      <span
        aria-hidden="true"
        className="flex h-16 w-16 items-center justify-center rounded-card bg-warn-soft text-warn"
      >
        <Clock size={30} />
      </span>
    )
  }
  if (kind === 'paused') {
    return (
      <span
        aria-hidden="true"
        className="flex h-16 w-16 items-center justify-center rounded-card bg-status-soft text-ink-soft"
      >
        <Pause size={30} />
      </span>
    )
  }
  return (
    <span aria-hidden="true" className="flex h-16 w-16 items-center justify-center rounded-card bg-deal-soft text-deal">
      <Check size={30} />
    </span>
  )
}
