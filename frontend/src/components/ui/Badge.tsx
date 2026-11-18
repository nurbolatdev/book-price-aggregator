import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

export type BadgeTone = 'ok' | 'warn' | 'danger' | 'neutral' | 'good' | 'off'

const tones: Record<BadgeTone, string> = {
  ok: 'bg-deal-soft text-deal',
  good: 'bg-deal-soft text-deal font-semibold',
  warn: 'bg-warn-soft text-warn',
  danger: 'bg-danger-soft text-danger',
  neutral: 'bg-canvas text-muted',
  off: 'bg-status-soft text-ink-soft',
}

export function Badge({
  tone = 'neutral',
  className,
  ...rest
}: HTMLAttributes<HTMLSpanElement> & { tone?: BadgeTone }) {
  return (
    <span
      className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-[13px]', tones[tone], className)}
      {...rest}
    />
  )
}
