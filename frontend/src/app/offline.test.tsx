import { QueryClientProvider, onlineManager, useMutation, useQuery } from '@tanstack/react-query'
import { act, render, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from './queryClient'

afterEach(() => onlineManager.setOnline(true))

describe('offline behaviour', () => {
  it('pauses a query offline and runs it once back online', async () => {
    onlineManager.setOnline(false)
    const queryFn = vi.fn().mockResolvedValue('data')
    let status = ''
    function Probe() {
      const q = useQuery({ queryKey: ['x'], queryFn })
      status = q.fetchStatus
      return null
    }
    render(
      <QueryClientProvider client={createQueryClient()}>
        <Probe />
      </QueryClientProvider>,
    )
    await waitFor(() => expect(status).toBe('paused'))
    expect(queryFn).not.toHaveBeenCalled()
    act(() => onlineManager.setOnline(true))
    await waitFor(() => expect(queryFn).toHaveBeenCalledTimes(1))
  })

  it('pauses a mutation offline and runs it once back online', async () => {
    onlineManager.setOnline(false)
    const mutationFn = vi.fn().mockResolvedValue('ok')
    let paused = false
    let mutate: () => void = () => undefined
    function Probe() {
      const m = useMutation({ mutationFn })
      paused = m.isPaused
      mutate = () => m.mutate()
      return null
    }
    render(
      <QueryClientProvider client={createQueryClient()}>
        <Probe />
      </QueryClientProvider>,
    )
    act(() => mutate())
    await waitFor(() => expect(paused).toBe(true))
    expect(mutationFn).not.toHaveBeenCalled()
    act(() => onlineManager.setOnline(true))
    await waitFor(() => expect(mutationFn).toHaveBeenCalledTimes(1))
  })
})
