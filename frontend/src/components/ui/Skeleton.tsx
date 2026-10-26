export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden="true" className={'animate-pulse rounded-control bg-line ' + className} />
}

export function SkeletonText({ lines = 3 }: { lines?: number }) {
  return (
    <div aria-hidden="true" className="space-y-2">
      {Array.from({ length: lines }, (_, i) => (
        <Skeleton key={i} className={i === lines - 1 && lines > 1 ? 'h-4 w-2/3' : 'h-4 w-full'} />
      ))}
    </div>
  )
}

export function SkeletonCard() {
  return (
    <div aria-hidden="true" className="flex h-44 gap-4 rounded-card border border-line bg-surface p-4">
      <Skeleton className="h-36 w-24 shrink-0" />
      <div className="flex-1">
        <SkeletonText lines={3} />
      </div>
    </div>
  )
}
