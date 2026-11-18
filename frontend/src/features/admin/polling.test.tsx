import { QueryClient, QueryClientProvider, focusManager, useQuery } from '@tanstack/react-query'
import { renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ADMIN_POLL } from './polling'

function setup() {
  const queryFn = vi.fn().mockResolvedValue('x')
  const client = new QueryClient()
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
  renderHook(() => useQuery({ queryKey: ['poll'], queryFn, ...ADMIN_POLL }), { wrapper })
  return queryFn
}

describe('ADMIN_POLL', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    focusManager.setFocused(true)
  })
  afterEach(() => {
    focusManager.setFocused(undefined)
    vi.useRealTimers()
  })

  it('refetches every 30 seconds while the tab is visible', async () => {
    const queryFn = setup()
    await vi.advanceTimersByTimeAsync(0)
    expect(queryFn).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(30_000)
    expect(queryFn).toHaveBeenCalledTimes(2)
    await vi.advanceTimersByTimeAsync(30_000)
    expect(queryFn).toHaveBeenCalledTimes(3)
  })

  it('stops while hidden and refetches at once on return', async () => {
    const queryFn = setup()
    await vi.advanceTimersByTimeAsync(0)
    focusManager.setFocused(false)
    await vi.advanceTimersByTimeAsync(90_000)
    expect(queryFn).toHaveBeenCalledTimes(1)
    focusManager.setFocused(true)
    await vi.advanceTimersByTimeAsync(0)
    expect(queryFn).toHaveBeenCalledTimes(2)
    await vi.advanceTimersByTimeAsync(30_000)
    expect(queryFn).toHaveBeenCalledTimes(3)
  })
})
