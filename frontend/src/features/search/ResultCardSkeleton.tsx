import { Skeleton } from '../../components/ui/Skeleton'

export function ResultCardSkeleton() {
  return (
    <div aria-hidden="true" className="flex gap-5 rounded-card border border-line bg-surface p-5">
      <Skeleton className="h-40 w-[110px] shrink-0 rounded-lg" />
      <div className="flex flex-1 flex-col gap-3">
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-8 w-3/4" />
      </div>
      <div className="flex w-[180px] shrink-0 flex-col gap-3">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-7 w-28" />
        <Skeleton className="h-11 w-full" />
      </div>
    </div>
  )
}
