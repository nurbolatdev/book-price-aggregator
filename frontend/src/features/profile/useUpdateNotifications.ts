import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateMe } from '../../api/me'
import { queryKeys } from '../../api/queryKeys'
import type { UpdateProfileRequest, UserResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { toast } from '../../stores/toast'
import { writeUser } from './userCache'

type NotificationPatch = Pick<UpdateProfileRequest, 'notifyFrequency' | 'weeklyDigest'>

export function useUpdateNotifications() {
  const queryClient = useQueryClient()
  return useMutation<UserResponse, unknown, NotificationPatch, { previous: UserResponse | null }>({
    mutationFn: updateMe,
    meta: { silentError: true },
    onMutate: async (patch) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.me })
      const previous = queryClient.getQueryData<UserResponse>(queryKeys.me) ?? useAuthStore.getState().user
      if (previous) writeUser(queryClient, { ...previous, ...patch })
      return { previous }
    },
    onError: (_error, _patch, context) => {
      if (context?.previous) writeUser(queryClient, context.previous)
      toast.error('Не удалось сохранить настройки')
    },
    onSuccess: (user) => writeUser(queryClient, user),
  })
}
