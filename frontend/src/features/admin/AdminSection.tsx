import { useId, type ReactNode } from 'react'
import { cn } from '../../lib/cn'

interface AdminSectionProps {
  title?: string
  actions?: ReactNode
  id?: string
  className?: string
  tone?: 'danger'
  children: ReactNode
}

export function AdminSection({ title, actions, id, className, tone, children }: AdminSectionProps) {
  const headingId = useId()
  return (
    <section
      id={id}
      aria-labelledby={title ? headingId : undefined}
      className={cn(
        'flex flex-col gap-3.5 rounded-card border bg-surface p-5',
        tone === 'danger' ? 'border-danger-line' : 'border-line',
        className,
      )}
    >
      {(title || actions) && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          {title && (
            <h2 id={headingId} className="font-sans text-lg font-bold">
              {title}
            </h2>
          )}
          {actions}
        </div>
      )}
      {children}
    </section>
  )
}
