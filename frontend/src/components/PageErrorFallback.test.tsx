import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PageErrorFallback } from './PageErrorFallback'

describe('PageErrorFallback', () => {
  it('shows the message and calls onRetry', () => {
    const onRetry = vi.fn()
    render(<PageErrorFallback onRetry={onRetry} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Что-то пошло не так')
    fireEvent.click(screen.getByRole('button', { name: 'Обновить страницу' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
