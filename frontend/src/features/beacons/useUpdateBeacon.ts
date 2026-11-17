import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateBeacon } from '../../api/beacons'
import { ApiError } from '../../api/errors'
import type { BeaconResponse, UpdateBeaconRequest } from '../../api/types'
import { BEACON_MUTATION_KEY } from './beaconCache'
import { toast } from '../../stores/toast'
import { findBeacon, patchBeacon, upsertBeacon } from './beaconCache'

export interface UpdateBeaconVariables {
  id: number
  editionId: number
  patch: UpdateBeaconRequest
}

export function useUpdateBeacon() {
  const queryClient = useQueryClient()
  return useMutation<BeaconResponse, unknown, UpdateBeaconVariables, { previous: BeaconResponse | undefined }>({
    mutationFn: ({ id, patch }) => updateBeacon(id, patch),
    mutationKey: BEACON_MUTATION_KEY,
    meta: { silentError: true },
    onMutate: async ({ id, patch }) => {
      await queryClient.cancelQueries({ queryKey: ['beacons'] })
      const previous = findBeacon(queryClient, id)
      patchBeacon(queryClient, id, patch)
      return { previous }
    },
    onError: (error, _variables, context) => {
      if (context?.previous) upsertBeacon(queryClient, context.previous)
      if (!(error instanceof ApiError && error.code === 'VALIDATION_FAILED')) {
        toast.error('Не удалось сохранить. Изменения отменены.')
      }
    },
    onSuccess: (beacon) => upsertBeacon(queryClient, beacon),
    onSettled: () => {
      // a refetch while another beacon mutation is in flight would overwrite its optimistic state
      if (queryClient.isMutating({ mutationKey: BEACON_MUTATION_KEY }) <= 1) {
        return queryClient.invalidateQueries({ queryKey: ['beacons'] })
      }
    },
  })
}
