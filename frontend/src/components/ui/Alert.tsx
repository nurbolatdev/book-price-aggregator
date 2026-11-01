import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

export type AlertTone = 'info' | 'warn' | 'danger' | 'offline'

const tones: Record<AlertTone, string> = {
  info: 'border-link/30 bg-canvas text-ink',
  warn: 'border-warn/30 bg-warn-soft text-warn',
  danger: 'border-danger/30 bg-danger-soft text-danger',
  offline: 'border-line bg-ink text-white',
}

interface AlertProps {
  tone?: AlertTone
  action?: ReactNode
  className?: string
  children: ReactNode
}

export function Alert({ tone = 'info', action, className, children }: AlertProps) {
  const role = tone === 'danger' || tone === 'warn' ? 'alert' : 'status'
  return (
    <div
      role={role}
      className={cn(
        'flex flex-wrap items-center gap-3 rounded-xl border px-4 py-3 text-[15px]',
        tones[tone],
        className,
      )}
    >
      <div className="flex min-w-0 flex-1 items-center gap-2">{children}</div>
      {action}
    </div>
  )
}
