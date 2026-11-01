import { Skeleton } from '../../components/ui/Skeleton'

export function DropCardSkeleton() {
  return (
    <div
      aria-hidden="true"
      className="flex flex-col gap-3 rounded-[14px] border border-line bg-surface p-2 max-md:gap-1.5 md:rounded-card md:p-4"
    >
      <Skeleton className="h-[150px] w-full md:h-[220px]" />
      <Skeleton className="h-4 w-3/4" />
      <Skeleton className="h-4 w-1/2" />
      <Skeleton className="h-6 w-2/3" />
    </div>
  )
}
