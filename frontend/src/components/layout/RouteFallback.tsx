import type { ReactNode } from 'react'

// Reserves the height of a loaded page so the footer stays below the fold while
// the route chunk and the data load (avoids a footer shift).
export const ROUTE_MIN_HEIGHT = 'min-h-[1200px]'

export function RouteFallback({ children }: { children: ReactNode }) {
  return (
    <div role="status" aria-label="Загрузка" className={ROUTE_MIN_HEIGHT}>
      {children}
    </div>
  )
}
