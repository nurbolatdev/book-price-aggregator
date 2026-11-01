import { useState } from 'react'
import { cn } from '../../lib/cn'
import { coverColor } from '../../lib/cover'

interface CoverProps {
  editionId: number
  title: string
  coverUrl?: string | null
  size?: 'lg' | 'md' | 'sm'
  className?: string
}

const PLATE_SIZE = {
  lg: 'p-4 text-lg font-display',
  md: 'p-2.5 text-[13px]',
  sm: 'p-1.5 text-[9px]',
} as const

export function Cover({
  editionId,
  title,
  coverUrl,
  size = 'md',
  className,
}: CoverProps) {
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
    <div
      className={cn(
        box,
        'flex items-end',
        PLATE_SIZE[size],
        coverColor(editionId),
      )}
      data-testid="cover-plate"
    >
      <span className="line-clamp-4 break-words font-display font-medium leading-tight text-ink hyphens-auto">
        {title}
      </span>
    </div>
  )
}
