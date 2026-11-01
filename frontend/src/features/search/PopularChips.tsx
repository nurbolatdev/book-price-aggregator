import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchPopular } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'
import { chipClasses } from '../../components/ui/Chip'
import { Skeleton } from '../../components/ui/Skeleton'
import { cn } from '../../lib/cn'

interface PopularChipsProps {
  limit?: number
  scrollOnPhone?: boolean
}

export function PopularChips({ limit = 6, scrollOnPhone = false }: PopularChipsProps) {
  const { data, isLoading } = useQuery({
    queryKey: queryKeys.popular(limit),
    queryFn: ({ signal }) => fetchPopular(limit, signal),
    staleTime: STALE_TIME.reference,
  })

  if (isLoading) {
    return (
      <div className="flex h-9 items-center gap-2.5" aria-hidden="true">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-9 w-32 rounded-full" />
        ))}
      </div>
    )
  }
  if (!data || data.length === 0) return null

  return (
    <div className="flex flex-col gap-2 md:flex-row md:flex-wrap md:items-center md:gap-2.5">
      <span className="text-[13px] text-muted md:text-sm">Часто ищут:</span>
      <div
        className={cn(
          'flex gap-2 md:flex-wrap md:gap-2.5',
          scrollOnPhone && 'overflow-x-auto whitespace-nowrap max-md:pb-1 md:overflow-visible',
        )}
      >
        {data.map((q) => (
          <Link
            key={q}
            to={'/search?q=' + encodeURIComponent(q)}
            className={chipClasses({ size: 'sm', className: 'shrink-0' })}
          >
            {q}
          </Link>
        ))}
      </div>
    </div>
  )
}
