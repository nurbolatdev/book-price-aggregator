import { MutationObserver } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/errors'
import { useToastStore } from '../stores/toast'
import { createQueryClient, shouldRetry } from './queryClient'

describe('queryClient', () => {
  it('does not retry 4xx errors', () => {
    expect(shouldRetry(0, new ApiError(404, 'NOT_FOUND', 'x'))).toBe(false)
    expect(shouldRetry(0, new ApiError(422, 'VALIDATION_FAILED', 'x'))).toBe(false)
  })

  it('retries 5xx and network errors while count < 2', () => {
    expect(shouldRetry(0, new ApiError(500, 'INTERNAL_ERROR', 'x'))).toBe(true)
    expect(shouldRetry(1, new ApiError(0, 'NETWORK_ERROR', 'x'))).toBe(true)
    expect(shouldRetry(2, new ApiError(500, 'INTERNAL_ERROR', 'x'))).toBe(false)
  })

  it('disables refetch on window focus', () => {
    expect(createQueryClient().getDefaultOptions().queries?.refetchOnWindowFocus).toBe(false)
  })

  it('shows an error toast for a failing mutation', async () => {
    useToastStore.setState({ toasts: [] })
    const client = createQueryClient()
    const observer = new MutationObserver(client, {
      mutationFn: () => Promise.reject(new ApiError(401, 'INVALID_CREDENTIALS', 'x')),
    })
    await observer.mutate().catch(() => undefined)
    expect(useToastStore.getState().toasts.map((t) => t.message)).toEqual(['Неверный email или пароль'])
  })

  it('stays silent when meta.silentError is set', async () => {
    useToastStore.setState({ toasts: [] })
    const client = createQueryClient()
    const observer = new MutationObserver(client, {
      mutationFn: () => Promise.reject(new ApiError(401, 'INVALID_CREDENTIALS', 'x')),
      meta: { silentError: true },
    })
    await observer.mutate().catch(() => undefined)
    expect(useToastStore.getState().toasts).toEqual([])
  })
})
