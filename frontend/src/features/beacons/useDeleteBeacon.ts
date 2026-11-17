import { useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useRef } from 'react'
import { deleteBeacon } from '../../api/beacons'
import { ApiError } from '../../api/errors'
import type { BeaconResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { toast, useToastStore } from '../../stores/toast'
import { insertBeaconAt, removeBeacon } from './beaconCache'
import { pendingDeletes } from './useBeacons'

const UNDO_MS = 5000

interface Pending {
  beacon: BeaconResponse
  index: number
}

async function commit(queryClient: QueryClient, { beacon, index }: Pending): Promise<void> {
  try {
    await deleteBeacon(beacon.id ?? 0)
  } catch (error) {
    if (!(error instanceof ApiError && error.code === 'NOT_FOUND')) {
      pendingDeletes.delete(beacon.id ?? 0)
      insertBeaconAt(queryClient, beacon, index)
      toast.error('Не удалось удалить маячок')
    }
  } finally {
    pendingDeletes.delete(beacon.id ?? 0)
    void queryClient.invalidateQueries({ queryKey: ['beacons'] })
  }
}

export function useDeleteBeacon() {
  const queryClient = useQueryClient()
  const pending = useRef(new Map<number, Pending>())

  const remove = useCallback(
    (beacon: BeaconResponse) => {
      const entry: Pending = { beacon, index: removeBeacon(queryClient, beacon) }
      const id = beacon.id ?? 0
      pendingDeletes.add(id)
      const toastId = toast.info('Маячок удалён', {
        duration: UNDO_MS,
        action: { label: 'Отменить', onClick: () => {} },
        onClose: (reason) => {
          pending.current.delete(toastId)
          if (reason === 'action') {
            pendingDeletes.delete(id)
            insertBeaconAt(queryClient, entry.beacon, entry.index)
          } else {
            void commit(queryClient, entry)
          }
        },
      })
      pending.current.set(toastId, entry)
    },
    [queryClient],
  )

  useEffect(() => {
    const entries = pending.current
    const flush = () => {
      const authenticated = useAuthStore.getState().status === 'authenticated'
      for (const [toastId, entry] of [...entries]) {
        entries.delete(toastId)
        useToastStore.getState().remove(toastId)
        if (authenticated) void commit(queryClient, entry)
        else pendingDeletes.delete(entry.beacon.id ?? 0)
      }
    }
    window.addEventListener('pagehide', flush)
    return () => {
      window.removeEventListener('pagehide', flush)
      flush()
    }
  }, [queryClient])

  return { remove }
}
