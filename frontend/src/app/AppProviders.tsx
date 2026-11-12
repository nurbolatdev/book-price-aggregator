import { useEffect, useState, type ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { USER_SCOPED_KEYS } from '../api/queryKeys'
import { Toaster } from '../components/ui/Toaster'
import { useAuthStore } from '../stores/auth'
import { createQueryClient } from './queryClient'

export function AppProviders({ children }: { children: ReactNode }) {
  const [client] = useState(createQueryClient)
  useEffect(
    () =>
      useAuthStore.subscribe((state, previous) => {
        if (previous.status === 'authenticated' && state.status === 'anonymous') {
          for (const queryKey of USER_SCOPED_KEYS) client.removeQueries({ queryKey })
        }
      }),
    [client],
  )
  return (
    <QueryClientProvider client={client}>
      {children}
      <Toaster />
    </QueryClientProvider>
  )
}
