import { useId, type ReactNode } from 'react'
import { cn } from '../../lib/cn'

interface AdminSectionProps {
  title?: string
  actions?: ReactNode
  id?: string
  className?: string
  children: ReactNode
}

export function AdminSection({ title, actions, id, className, children }: AdminSectionProps) {
  const headingId = useId()
  return (
    <section
      id={id}
      aria-labelledby={title ? headingId : undefined}
      className={cn('flex flex-col gap-3.5 rounded-card border border-line bg-surface p-5', className)}
    >
      {(title || actions) && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          {title && (
            <h2 id={headingId} className="text-lg font-bold">
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
