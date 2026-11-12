import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useCountdown } from './useCountdown'

describe('useCountdown', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  const tick = () =>
    act(() => {
      vi.advanceTimersByTime(1000)
    })

  it('counts down to zero and stops', () => {
    const { result } = renderHook(() => useCountdown())
    act(() => result.current.start(3))
    expect(result.current.secondsLeft).toBe(3)
    tick()
    expect(result.current.secondsLeft).toBe(2)
    tick()
    expect(result.current.secondsLeft).toBe(1)
    tick()
    expect(result.current.secondsLeft).toBe(0)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('restarts from the new value', () => {
    const { result } = renderHook(() => useCountdown())
    act(() => result.current.start(3))
    tick()
    act(() => result.current.start(10))
    expect(result.current.secondsLeft).toBe(10)
    tick()
    expect(result.current.secondsLeft).toBe(9)
  })

  it('clears the timer on unmount', () => {
    const { result, unmount } = renderHook(() => useCountdown())
    act(() => result.current.start(5))
    unmount()
    expect(vi.getTimerCount()).toBe(0)
  })
})
