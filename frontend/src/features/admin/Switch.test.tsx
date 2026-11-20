import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Switch } from './Switch'

describe('Switch', () => {
  it('exposes role, state and name', () => {
    render(<Switch checked label="Магазин Flip.kz" onChange={() => {}} />)
    expect(screen.getByRole('switch', { name: 'Магазин Flip.kz' })).toHaveAttribute('aria-checked', 'true')
  })

  it('toggles through onChange', () => {
    const onChange = vi.fn()
    render(<Switch checked={false} label="S" onChange={onChange} />)
    fireEvent.click(screen.getByRole('switch'))
    expect(onChange).toHaveBeenCalledWith(true)
  })

  it('does nothing when disabled', () => {
    const onChange = vi.fn()
    render(<Switch checked label="S" disabled onChange={onChange} />)
    fireEvent.click(screen.getByRole('switch'))
    expect(onChange).not.toHaveBeenCalled()
  })
})
