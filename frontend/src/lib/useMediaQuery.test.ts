import { act, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useMediaQuery } from './useMediaQuery'

describe('useMediaQuery', () => {
  const original = window.matchMedia
  afterEach(() => {
    window.matchMedia = original
  })

  it('follows change events', () => {
    let matches = false
    const listeners = new Set<() => void>()
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      get matches() {
        return matches
      },
      media: query,
      addEventListener: (_: string, cb: () => void) => listeners.add(cb),
      removeEventListener: (_: string, cb: () => void) => listeners.delete(cb),
    }))

    const { result } = renderHook(() => useMediaQuery('(min-width: 768px)'))
    expect(result.current).toBe(false)
    act(() => {
      matches = true
      listeners.forEach((cb) => cb())
    })
    expect(result.current).toBe(true)
  })
})
