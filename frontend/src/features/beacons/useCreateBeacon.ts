import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { createBeacon } from '../../api/beacons'
import { ApiError } from '../../api/errors'
import type { BeaconResponse, CreateBeaconRequest } from '../../api/types'
import { toast } from '../../stores/toast'
import type { BeaconBook } from './beaconBook'
import { applyPatch, restore, snapshot, upsertBeacon, type BeaconSnapshot } from './beaconCache'

export interface CreateBeaconVariables {
  request: CreateBeaconRequest
  book: BeaconBook
}

interface Context {
  saved: BeaconSnapshot
  tempId: number
}

export function useCreateBeacon() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation<BeaconResponse, unknown, CreateBeaconVariables, Context>({
    mutationFn: ({ request }) => createBeacon(request),
    meta: { silentError: true },
    onMutate: async ({ request, book }) => {
      await queryClient.cancelQueries({ queryKey: ['beacons'] })
      const saved = snapshot(queryClient, request.editionId)
      const now = new Date().toISOString()
      const tempId = -Date.now()
      const base: BeaconResponse = {
        id: tempId,
        editionId: request.editionId,
        title: book.title,
        coverUrl: book.coverUrl,
        status: 'ACTIVE',
        currentBestPrice: book.bestPrice ?? undefined,
        baselinePrice: book.bestPrice ?? undefined,
        createdAt: now,
        statusChangedAt: now,
      }
      upsertBeacon(queryClient, applyPatch(base, request, now))
      return { saved, tempId }
    },
    onError: (error, { request }, context) => {
      if (context) restore(queryClient, context.saved, request.editionId)
      if (error instanceof ApiError && error.code === 'CONFLICT') {
        toast.info('На эту книгу маячок уже стоит', {
          action: { label: 'Изменить', onClick: () => navigate('/beacons') },
        })
      } else if (!(error instanceof ApiError && error.code === 'VALIDATION_FAILED')) {
        toast.error('Не удалось сохранить. Изменения отменены.')
      }
    },
    onSuccess: (beacon, _variables, context) => {
      upsertBeacon(queryClient, beacon, context?.tempId)
      toast.success('Маячок поставлен', {
        action: { label: 'Мои маячки', onClick: () => navigate('/beacons') },
      })
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['beacons'] }),
  })
}
