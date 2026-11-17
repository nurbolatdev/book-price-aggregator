import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateBeacon } from '../../api/beacons'
import { ApiError } from '../../api/errors'
import type { BeaconResponse, UpdateBeaconRequest } from '../../api/types'
import { toast } from '../../stores/toast'
import { patchBeacon, restore, snapshot, upsertBeacon, type BeaconSnapshot } from './beaconCache'

export interface UpdateBeaconVariables {
  id: number
  editionId: number
  patch: UpdateBeaconRequest
}

export function useUpdateBeacon() {
  const queryClient = useQueryClient()
  return useMutation<BeaconResponse, unknown, UpdateBeaconVariables, { saved: BeaconSnapshot }>({
    mutationFn: ({ id, patch }) => updateBeacon(id, patch),
    meta: { silentError: true },
    onMutate: async ({ id, editionId, patch }) => {
      await queryClient.cancelQueries({ queryKey: ['beacons'] })
      const saved = snapshot(queryClient, editionId)
      patchBeacon(queryClient, id, patch)
      return { saved }
    },
    onError: (error, { editionId }, context) => {
      if (context) restore(queryClient, context.saved, editionId)
      if (!(error instanceof ApiError && error.code === 'VALIDATION_FAILED')) {
        toast.error('Не удалось сохранить. Изменения отменены.')
      }
    },
    onSuccess: (beacon) => upsertBeacon(queryClient, beacon),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['beacons'] }),
  })
}
