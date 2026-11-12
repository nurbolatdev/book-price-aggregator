import type { QueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/queryKeys'
import type { UserResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

/** The one place that updates the signed-in user in both the auth store and the `me` cache. */
export function writeUser(queryClient: QueryClient, user: UserResponse): void {
  queryClient.setQueryData(queryKeys.me, user)
  useAuthStore.getState().setUser(user)
}
