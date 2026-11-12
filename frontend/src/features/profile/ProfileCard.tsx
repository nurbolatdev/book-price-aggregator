import { useId, type ReactNode } from 'react'
import { cn } from '../../lib/cn'

interface ProfileCardProps {
  title: string
  tone?: 'default' | 'danger'
  children: ReactNode
}

export function ProfileCard({ title, tone = 'default', children }: ProfileCardProps) {
  const id = useId()
  return (
    <section
      aria-labelledby={id}
      className={cn(
        'flex flex-col gap-3.5 rounded-card border bg-surface p-5',
        tone === 'danger' ? 'border-danger-line' : 'border-line',
      )}
    >
      <h2 id={id} className={cn('font-sans text-lg font-bold', tone === 'danger' ? 'text-danger' : 'text-ink')}>
        {title}
      </h2>
      {children}
    </section>
  )
}
