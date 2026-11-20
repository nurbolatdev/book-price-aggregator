import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { retryNotification } from '../../../api/admin'
import { getErrorMessage, toApiError } from '../../../api/errors'
import type { AdminNotification, AdminNotificationPage } from '../../../api/types'
import { QUEUED_COUNT_KEYS } from './useNotificationKpis'

function rowMessage(error: unknown): string {
  const apiError = toApiError(error)
  if (apiError.status === 409) {
    return 'Повторить нельзя: письмо уже не в статусе «Ошибка» или почта не подтверждена'
  }
  return getErrorMessage(apiError)
}

export function useRetryNotification() {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: retryNotification,
    meta: { silentError: true },
  })
  const [rowError, setRowError] = useState<Record<number, string>>({})
  const [retrying, setRetrying] = useState<Set<number>>(new Set())

  function setBusy(id: number, busy: boolean) {
    setRetrying((prev) => {
      const next = new Set(prev)
      if (busy) next.add(id)
      else next.delete(id)
      return next
    })
  }

  async function retry(id: number): Promise<void> {
    setRowError((prev) => {
      const next = { ...prev }
      delete next[id]
      return next
    })
    setBusy(id, true)
    try {
      const updated: AdminNotification = await mutation.mutateAsync(id)
      queryClient.setQueriesData<AdminNotificationPage>(
        { queryKey: ['admin', 'notifications'] },
        (page) =>
          page?.content
            ? {
                ...page,
                content: page.content.map((n) => (n.id === id ? updated : n)),
              }
            : page,
      )
      for (const queryKey of QUEUED_COUNT_KEYS) {
        void queryClient.invalidateQueries({ queryKey })
      }
    } catch (error) {
      setRowError((prev) => ({ ...prev, [id]: rowMessage(error) }))
    } finally {
      setBusy(id, false)
    }
  }

  return { retry, rowError, retrying }
}
