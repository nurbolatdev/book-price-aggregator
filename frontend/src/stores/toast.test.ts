import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { toast, useToastStore } from './toast'

describe('toast store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    useToastStore.setState({ toasts: [] })
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('reports timeout after the custom duration', () => {
    const onClose = vi.fn()
    toast.info('a', { duration: 3000, onClose })
    vi.advanceTimersByTime(2999)
    expect(onClose).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onClose).toHaveBeenCalledWith('timeout')
    expect(useToastStore.getState().toasts).toHaveLength(0)
  })

  it('dismiss reports once and clears the timer', () => {
    const onClose = vi.fn()
    const id = toast.info('a', { onClose })
    useToastStore.getState().dismiss(id)
    vi.advanceTimersByTime(10000)
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onClose).toHaveBeenCalledWith('dismiss')
  })

  it('remove is silent', () => {
    const onClose = vi.fn()
    const id = toast.info('a', { onClose })
    useToastStore.getState().remove(id)
    vi.advanceTimersByTime(10000)
    expect(onClose).not.toHaveBeenCalled()
    expect(useToastStore.getState().toasts).toHaveLength(0)
  })

  it('keeps default durations', () => {
    toast.success('s')
    toast.error('e')
    vi.advanceTimersByTime(5000)
    expect(useToastStore.getState().toasts.map((t) => t.message)).toEqual(['e'])
    vi.advanceTimersByTime(2000)
    expect(useToastStore.getState().toasts).toHaveLength(0)
  })
})
