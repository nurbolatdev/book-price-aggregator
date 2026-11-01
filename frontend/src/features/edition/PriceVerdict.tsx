import { TrendingDown, TrendingUp } from 'lucide-react'
import type { PriceVerdict as PriceVerdictData } from '../../api/types'
import { cn } from '../../lib/cn'

interface PriceVerdictProps {
  verdict?: PriceVerdictData
  size?: 'md' | 'sm'
}

const SIZE = {
  md: 'inline-flex self-start items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-semibold',
  sm: 'self-start rounded-md px-2 py-1 text-xs font-semibold',
} as const

export function PriceVerdict({ verdict, size = 'md' }: PriceVerdictProps) {
  const label = verdict?.label
  if (!label || label === 'NOT_ENOUGH_DATA') return null
  const percent = Math.abs(verdict.diffPercent ?? 0)
  const md = size === 'md'

  if (label === 'AVERAGE') {
    return <span className={cn(SIZE[size], 'bg-canvas text-muted')}>Обычная цена</span>
  }
  if (label === 'BELOW_AVERAGE') {
    return (
      <span className={cn(SIZE[size], 'bg-deal-soft text-deal')}>
        {md && <TrendingDown size={16} aria-hidden="true" />}
        {md ? `Ниже средней за 3 месяца на ${percent}%` : `Ниже средней на ${percent}%`}
      </span>
    )
  }
  return (
    <span className={cn(SIZE[size], 'bg-canvas text-muted')}>
      {md && <TrendingUp size={16} aria-hidden="true" />}
      Выше средней на {percent}%
    </span>
  )
}
