import { useQuery } from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'
import { fetchBeacons } from '../../api/beacons'
import { queryKeys } from '../../api/queryKeys'
import { STALE_TIME } from '../../app/queryClient'
import { useAuthStore } from '../../stores/auth'

let version = 0
const listeners = new Set<() => void>()

function notify() {
  version += 1
  listeners.forEach((listener) => listener())
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

class PendingSet extends Set<number> {
  override add(id: number): this {
    super.add(id)
    notify()
    return this
  }

  override delete(id: number): boolean {
    const had = super.delete(id)
    if (had) notify()
    return had
  }

  override clear(): void {
    const had = this.size > 0
    super.clear()
    if (had) notify()
  }
}

/** Ids removed from the screen whose DELETE has not finished yet (undo window and the request itself). */
export const pendingDeletes: Set<number> = new PendingSet()

export function useBeacons() {
  const status = useAuthStore((s) => s.status)
  useSyncExternalStore(subscribe, () => version)
  return useQuery({
    queryKey: queryKeys.beacons,
    queryFn: ({ signal }) => fetchBeacons(signal),
    enabled: status === 'authenticated',
    staleTime: STALE_TIME.search,
    select: (list) => list.filter((item) => item.id === undefined || !pendingDeletes.has(item.id)),
  })
}
