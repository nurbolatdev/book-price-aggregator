import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setViewport } from '../../test/viewport'
import { ConfirmDialog } from './ConfirmDialog'

function setup(pending = false) {
  const onConfirm = vi.fn()
  const onClose = vi.fn()
  render(
    <ConfirmDialog
      open
      title="Выключить Meloman?"
      text="Магазин пропадёт из поиска."
      confirmLabel="Выключить"
      tone="danger"
      pending={pending}
      onConfirm={onConfirm}
      onClose={onClose}
    />,
  )
  return { onConfirm, onClose }
}

describe('ConfirmDialog', () => {
  beforeEach(() => setViewport(1280))

  it('shows title, text and both buttons', () => {
    setup()
    expect(screen.getByRole('dialog', { name: 'Выключить Meloman?' })).toBeInTheDocument()
    expect(screen.getByText('Магазин пропадёт из поиска.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Отмена' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Выключить' })).toBeInTheDocument()
  })

  it('cancel closes without confirming', () => {
    const { onConfirm, onClose } = setup()
    fireEvent.click(screen.getByRole('button', { name: 'Отмена' }))
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('confirm calls onConfirm', () => {
    const { onConfirm } = setup()
    fireEvent.click(screen.getByRole('button', { name: 'Выключить' }))
    expect(onConfirm).toHaveBeenCalledTimes(1)
  })

  it('marks the confirm button busy while pending', () => {
    setup(true)
    expect(screen.getByRole('button', { name: 'Выключить' })).toHaveAttribute('aria-busy', 'true')
  })
})
