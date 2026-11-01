import { useState } from 'react'
import { cn } from '../../lib/cn'
import { coverColor } from '../../lib/cover'

interface CoverProps {
  editionId: number
  title: string
  coverUrl?: string | null
  className?: string
}

export function Cover({ editionId, title, coverUrl, className }: CoverProps) {
  const [failed, setFailed] = useState(false)
  const box = cn('relative shrink-0 overflow-hidden', className)
  if (coverUrl && !failed) {
    return (
      <div className={box}>
        <img
          src={coverUrl}
          alt=""
          loading="lazy"
          decoding="async"
          className="h-full w-full object-cover"
          onError={() => setFailed(true)}
        />
      </div>
    )
  }
  return (
    <div className={cn(box, 'flex items-end p-2', coverColor(editionId))} data-testid="cover-plate">
      <span className="line-clamp-4 font-display text-[11px] font-medium leading-tight text-ink">{title}</span>
    </div>
  )
}
