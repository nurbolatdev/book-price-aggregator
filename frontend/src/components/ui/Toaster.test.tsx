import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { toast, useToastStore } from '../../stores/toast'
import { Toaster } from './Toaster'

describe('Toaster', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    useToastStore.setState({ toasts: [] })
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders a toast inside an aria-live container and auto-hides it', () => {
    render(<Toaster />)
    act(() => {
      toast.success('Сохранено')
    })
    const text = screen.getByText('Сохранено')
    expect(text.closest('[aria-live="polite"]')).not.toBeNull()
    act(() => {
      vi.advanceTimersByTime(5000)
    })
    expect(screen.queryByText('Сохранено')).toBeNull()
  })

  it('removes a toast immediately on close', () => {
    render(<Toaster />)
    act(() => {
      toast.info('Привет')
    })
    fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }))
    expect(screen.queryByText('Привет')).toBeNull()
  })

  it('styles variants differently', () => {
    render(<Toaster />)
    act(() => {
      toast.success('a')
      toast.error('b')
      toast.info('c')
    })
    const cls = ['a', 'b', 'c'].map((t) => screen.getByText(t).parentElement?.className)
    expect(new Set(cls).size).toBe(3)
  })
})
