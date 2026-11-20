import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { setStoreEnabled } from '../../../api/admin'
import { queryKeys } from '../../../api/queryKeys'
import type { AdminParser, StoreCode } from '../../../api/types'
import { toast } from '../../../stores/toast'

export function useToggleStore() {
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState<AdminParser | null>(null)
  const [busy, setBusy] = useState<Set<StoreCode>>(new Set())

  const mutation = useMutation({
    mutationFn: ({ code, enabled }: { code: StoreCode; enabled: boolean }) =>
      setStoreEnabled(code, enabled),
  })

  async function apply(parser: AdminParser, enabled: boolean): Promise<void> {
    const code = parser.code as StoreCode
    const name = parser.name ?? code
    setBusy((prev) => new Set(prev).add(code))
    try {
      await mutation.mutateAsync({ code, enabled })
      toast.success(enabled ? `Магазин ${name} включён` : `Магазин ${name} выключен`)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.parsers }),
        queryClient.invalidateQueries({ queryKey: queryKeys.stores }),
      ])
    } catch {
      // the global mutation handler shows the error toast
    } finally {
      setBusy((prev) => {
        const next = new Set(prev)
        next.delete(code)
        return next
      })
      setConfirming(null)
    }
  }

  function toggle(parser: AdminParser): void {
    if (!parser.code) return
    if (parser.enabled) setConfirming(parser)
    else void apply(parser, true)
  }

  return {
    toggle,
    confirming,
    cancel: () => setConfirming(null),
    confirm: () => {
      if (confirming) void apply(confirming, false)
    },
    busy,
    pending: confirming?.code ? busy.has(confirming.code) : false,
  }
}

export type StoreToggle = ReturnType<typeof useToggleStore>
