import { Skeleton } from '../ui/Skeleton'

export function PageSkeleton() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10" role="status" aria-label="Загрузка">
      <Skeleton className="mb-6 h-9 w-1/2" />
      <Skeleton className="mb-3 h-4 w-full" />
      <Skeleton className="mb-3 h-4 w-full" />
      <Skeleton className="h-4 w-2/3" />
    </div>
  )
}
