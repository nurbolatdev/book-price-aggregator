import { act, render, renderHook, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import { RateLimitAlert } from './RateLimitAlert'
import { useRateLimit } from './useRateLimit'
import { useSingleSubmit } from './useSingleSubmit'

const limited = (retryAfter: number | null) =>
  new ApiError(429, 'RATE_LIMITED', 'x', [], retryAfter)

describe('useRateLimit', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('blocks for retryAfter seconds and keeps the alert text static', () => {
    const { result, rerender } = renderHook(() => useRateLimit())
    let handled = false
    act(() => {
      handled = result.current.handle(limited(45))
    })
    expect(handled).toBe(true)
    expect(result.current.blocked).toBe(true)
    const view = render(<RateLimitAlert rateLimit={result.current} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Слишком много попыток. Подождите 45 секунд.')
    act(() => {
      vi.advanceTimersByTime(5000)
    })
    rerender()
    expect(result.current.secondsLeft).toBe(40)
    view.rerender(<RateLimitAlert rateLimit={result.current} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Подождите 45 секунд.')
  })

  it('falls back to 60 seconds without retryAfter', () => {
    const { result } = renderHook(() => useRateLimit())
    act(() => {
      result.current.handle(limited(null))
    })
    expect(result.current.secondsLeft).toBe(60)
  })

  it('ignores other errors', () => {
    const { result } = renderHook(() => useRateLimit())
    let handled = true
    act(() => {
      handled = result.current.handle(new ApiError(500, 'INTERNAL_ERROR', 'x'))
    })
    expect(handled).toBe(false)
    expect(result.current.blocked).toBe(false)
  })

  it('unblocks at zero and hides the alert', () => {
    const { result } = renderHook(() => useRateLimit())
    act(() => {
      result.current.handle(limited(2))
    })
    act(() => {
      vi.advanceTimersByTime(2000)
    })
    expect(result.current.blocked).toBe(false)
    render(<RateLimitAlert rateLimit={result.current} />)
    expect(screen.queryByRole('alert')).toBeNull()
  })
})

describe('useSingleSubmit', () => {
  it('runs the handler once while the first call is pending', async () => {
    let release: () => void = () => {}
    const handler = vi.fn(() => new Promise<void>((resolve) => (release = resolve)))
    const { result } = renderHook(() => useSingleSubmit(handler))
    let first: Promise<void> = Promise.resolve()
    act(() => {
      first = result.current()
      void result.current()
    })
    expect(handler).toHaveBeenCalledTimes(1)
    await act(async () => {
      release()
      await first
    })
    await act(async () => {
      void result.current()
    })
    expect(handler).toHaveBeenCalledTimes(2)
  })
})
