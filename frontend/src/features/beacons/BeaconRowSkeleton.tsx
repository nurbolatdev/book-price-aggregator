import { Skeleton } from '../../components/ui/Skeleton'

export function BeaconRowSkeleton() {
  return (
    <div aria-hidden="true" className="flex items-center gap-3.5 rounded-card border border-line bg-surface p-3.5 md:gap-[18px] md:p-[18px]">
      <Skeleton className="h-[74px] w-[52px] shrink-0 md:h-[92px] md:w-16" />
      <div className="flex flex-1 flex-col gap-2">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-3.5 w-1/3" />
        <Skeleton className="h-3.5 w-1/2" />
      </div>
    </div>
  )
}
