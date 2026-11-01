import { Skeleton } from '../../components/ui/Skeleton'

export function EditionSkeleton() {
  return (
    <div role="status" aria-label="Загружаем издание" className="flex flex-col gap-6">
      <Skeleton className="h-5 w-64 max-w-full" />
      <div className="flex flex-wrap items-start gap-8">
        <Skeleton className="h-[380px] w-[260px] max-w-full rounded-[14px]" />
        <div className="flex min-w-0 flex-[1_1_340px] flex-col gap-3.5">
          <Skeleton className="h-10 w-4/5" />
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
        <Skeleton className="h-[300px] max-w-[360px] flex-[1_1_300px] rounded-card" />
      </div>
      <Skeleton className="h-[260px] w-full rounded-card" />
    </div>
  )
}
