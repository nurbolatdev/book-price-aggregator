import type { ReactNode } from 'react'
import { usePageMeta } from '../../lib/usePageMeta'
import { formatClock } from './adminFormat'

interface AdminPageHeaderProps {
  title: string
  subtitle?: ReactNode
  updatedAt?: number
  actions?: ReactNode
}

export function AdminPageHeader({ title, subtitle, updatedAt, actions }: AdminPageHeaderProps) {
  usePageMeta({ title: `${title} · Админ-панель | KitapRadar`, description: 'Админ-панель KitapRadar' })
  const line =
    updatedAt !== undefined
      ? `Обновляется каждые 30 секунд, пока вкладка открыта${updatedAt > 0 ? ` · последнее обновление ${formatClock(updatedAt)}` : ''}`
      : subtitle
  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 className="font-display text-[26px] font-medium">{title}</h1>
        {line && <p className="mt-1 text-sm text-muted">{line}</p>}
      </div>
      {actions}
    </div>
  )
}
