import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateMe } from '../../api/me'
import { toast } from '../../stores/toast'
import { writeUser } from './userCache'

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => updateMe({ name }),
    meta: { silentError: true },
    onSuccess: (user) => {
      writeUser(queryClient, user)
      toast.success('Изменения сохранены')
    },
  })
}
