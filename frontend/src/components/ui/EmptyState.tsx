import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  text?: string
  action?: ReactNode
  className?: string
}

export function EmptyState({ icon, title, text, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center gap-3 px-4 py-12 text-center', className)}>
      {icon && (
        <div
          aria-hidden="true"
          className="flex h-16 w-16 items-center justify-center rounded-2xl bg-accent-soft text-accent"
        >
          {icon}
        </div>
      )}
      <h2 className="font-display text-[22px] font-medium text-ink">{title}</h2>
      {text && <p className="max-w-md text-[15px] text-muted">{text}</p>}
      {action}
    </div>
  )
}
