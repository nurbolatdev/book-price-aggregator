import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ErrorBlock } from './ErrorBlock'

describe('ErrorBlock', () => {
  it('is an alert with a retry button', () => {
    const onRetry = vi.fn()
    render(<ErrorBlock onRetry={onRetry} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Не удалось загрузить данные.')
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
