import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { fetchPendingOffers, matchOffer, rejectOffer } from '../../../api/admin'
import { getErrorMessage, toApiError } from '../../../api/errors'
import { queryKeys } from '../../../api/queryKeys'
import type { ReviewOffer, ReviewOfferPage } from '../../../api/types'
import { toast } from '../../../stores/toast'
import { nextOfferId } from './nextOfferId'

export const PENDING_PAGE_SIZE = 20

export function useMatching(page: number) {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const query = useQuery({
    queryKey: queryKeys.admin.pendingOffers(page),
    queryFn: ({ signal }) => fetchPendingOffers(page, PENDING_PAGE_SIZE, signal),
    placeholderData: keepPreviousData,
  })

  const items = query.data?.content ?? []
  const selected: ReviewOffer | null = items.find((o) => o.id === selectedId) ?? items[0] ?? null

  const matchMutation = useMutation({
    mutationFn: ({ id, editionId }: { id: number; editionId: number }) =>
      matchOffer(id, editionId),
    meta: { silentError: true },
  })
  const rejectMutation = useMutation({
    mutationFn: (id: number) => rejectOffer(id),
    meta: { silentError: true },
  })

  async function finish(id: number, run: () => Promise<unknown>, message: string) {
    try {
      await run()
    } catch (error) {
      if (toApiError(error).status === 404) {
        toast.error('Предложение уже обработано')
        void queryClient.invalidateQueries({ queryKey: queryKeys.admin.pendingOffers(page) })
      } else {
        toast.error(getErrorMessage(toApiError(error)))
      }
      return
    }
    setSelectedId(nextOfferId(items, id))
    queryClient.setQueryData<ReviewOfferPage>(queryKeys.admin.pendingOffers(page), (data) =>
      data?.content
        ? {
            ...data,
            content: data.content.filter((o) => o.id !== id),
            totalElements: Math.max(0, (data.totalElements ?? 1) - 1),
          }
        : data,
    )
    toast.success(message)
    void queryClient.invalidateQueries({ queryKey: ['admin', 'pending-offers'] })
    void queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] })
  }

  async function match(editionId: number): Promise<void> {
    if (!selected?.id) return
    const id = selected.id
    await finish(id, () => matchMutation.mutateAsync({ id, editionId }), 'Предложение привязано к изданию')
  }

  async function reject(): Promise<void> {
    if (!selected?.id) return
    const id = selected.id
    await finish(id, () => rejectMutation.mutateAsync(id), 'Предложение отклонено')
  }

  return {
    query,
    selected,
    select: setSelectedId,
    match,
    reject,
    pending: matchMutation.isPending || rejectMutation.isPending,
  }
}
