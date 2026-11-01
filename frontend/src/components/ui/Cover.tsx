import { useState } from 'react'
import { cn } from '../../lib/cn'
import { coverColor } from '../../lib/cover'

interface CoverProps {
  editionId: number
  title: string
  coverUrl?: string | null
  size?: 'drop' | 'result' | 'detail'
  className?: string
}

const PLATE_SIZE = {
  drop: 'p-2.5 text-[13px] font-bold md:p-4 md:text-lg md:font-display md:font-medium',
  result: 'max-md:p-0 md:p-2 md:text-xs',
  detail: 'max-md:p-0 md:p-6 md:font-display md:text-2xl md:leading-[1.2]',
} as const

export function Cover({
  editionId,
  title,
  coverUrl,
  size = 'drop',
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
      <span
        className={cn(
          'line-clamp-4 break-words text-ink hyphens-auto',
          size !== 'detail' && 'leading-tight',
          size !== 'drop' && 'max-md:hidden',
        )}
      >
        {title}
      </span>
    </div>
  )
}
