import { MutationCache, QueryClient } from '@tanstack/react-query'
import { ApiError, getErrorMessage } from '../api/errors'
import { toast } from '../stores/toast'

declare module '@tanstack/react-query' {
  interface Register {
    mutationMeta: { silentError?: boolean }
  }
}

export const STALE_TIME = { search: 60_000, edition: 300_000, reference: 3_600_000 } as const

export function shouldRetry(count: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false
  return count < 2
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: shouldRetry, refetchOnWindowFocus: false },
    },
    mutationCache: new MutationCache({
      onError: (error, _variables, _context, mutation) => {
        if (mutation.meta?.silentError) return
        toast.error(getErrorMessage(error))
      },
    }),
  })
}
