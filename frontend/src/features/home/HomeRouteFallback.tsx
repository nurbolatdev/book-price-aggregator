import { RouteFallback } from '../../components/layout/RouteFallback'
import { Skeleton } from '../../components/ui/Skeleton'
import { DropGridSkeleton } from './DropGrid'

export function HomeRouteFallback() {
  return (
    <RouteFallback>
      <div className="mx-auto flex w-full max-w-page flex-col gap-3.5 px-4 pb-5 pt-7 md:gap-7 md:px-6 md:pb-12 md:pt-[72px]">
        <Skeleton className="h-[72px] w-4/5 md:h-[120px]" />
        <Skeleton className="h-12 w-full max-w-[720px]" />
        <Skeleton className="h-14 w-full max-w-[720px]" />
      </div>
      <div className="mx-auto w-full max-w-page px-4 pb-10 md:px-6 md:pb-14">
        <Skeleton className="mb-4 h-7 w-2/3 md:mb-6" />
        <DropGridSkeleton />
      </div>
    </RouteFallback>
  )
}
