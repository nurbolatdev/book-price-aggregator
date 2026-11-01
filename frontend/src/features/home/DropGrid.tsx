import type { DropItem } from '../../api/types'
import { cn } from '../../lib/cn'
import { DropCard } from './DropCard'
import { DropCardSkeleton } from './DropCardSkeleton'

const grid = 'grid grid-cols-2 gap-2.5 md:grid-cols-3 md:gap-5 lg:grid-cols-4'

export function DropGrid({ drops, phoneLimit }: { drops: DropItem[]; phoneLimit?: number }) {
  return (
    <div className={grid}>
      {drops.map((d, i) => (
        <DropCard
          key={d.editionId}
          drop={d}
          className={cn(phoneLimit !== undefined && i >= phoneLimit && 'max-md:hidden')}
        />
      ))}
    </div>
  )
}

export function DropGridSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div className={grid}>
      {Array.from({ length: count }, (_, i) => (
        <DropCardSkeleton key={i} />
      ))}
    </div>
  )
}
