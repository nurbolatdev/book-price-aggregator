import { fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { resetViewport, setViewport } from '../../test/viewport'
import { Modal } from './Modal'

function Harness({ onClose }: { onClose?: () => void }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button onClick={() => setOpen(true)}>Открыть</button>
      <Modal
        open={open}
        onClose={() => {
          onClose?.()
          setOpen(false)
        }}
        title="Удалить аккаунт?"
      >
        <button>Первая</button>
        <button>Последняя</button>
      </Modal>
    </>
  )
}

function openModal() {
  const opener = screen.getByRole('button', { name: 'Открыть' })
  opener.focus()
  fireEvent.click(opener)
  return opener
}

afterEach(() => {
  resetViewport()
})

describe('Modal on desktop', () => {
  it('is a modal dialog named by its title with focus on the first focusable', () => {
    setViewport(1280)
    render(<Harness />)
    openModal()
    const dialog = screen.getByRole('dialog', { name: 'Удалить аккаунт?' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog.className).toContain('max-w-[560px]')
    expect(screen.getByRole('button', { name: 'Закрыть' })).toHaveFocus()
  })

  it('wraps Tab from the last element to the first and Shift+Tab back', () => {
    setViewport(1280)
    render(<Harness />)
    openModal()
    const close = screen.getByRole('button', { name: 'Закрыть' })
    const last = screen.getByRole('button', { name: 'Последняя' })
    last.focus()
    fireEvent.keyDown(last, { key: 'Tab' })
    expect(close).toHaveFocus()
    fireEvent.keyDown(close, { key: 'Tab', shiftKey: true })
    expect(last).toHaveFocus()
  })

  it('closes on Escape and returns focus to the opener', () => {
    setViewport(1280)
    const onClose = vi.fn()
    render(<Harness onClose={onClose} />)
    const opener = openModal()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(opener).toHaveFocus()
  })

  it('closes on overlay click', () => {
    setViewport(1280)
    const onClose = vi.fn()
    render(<Harness onClose={onClose} />)
    openModal()
    fireEvent.click(screen.getByTestId('modal-overlay'))
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('locks body scroll while open and restores it after', () => {
    setViewport(1280)
    render(<Harness />)
    expect(document.body.style.overflow).toBe('')
    openModal()
    expect(document.body.style.overflow).toBe('hidden')
    fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }))
    expect(document.body.style.overflow).toBe('')
  })
})

describe('Modal on phone', () => {
  it('renders a bottom sheet with the same title', () => {
    render(<Harness />)
    openModal()
    const dialog = screen.getByRole('dialog', { name: 'Удалить аккаунт?' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog.className).toContain('bottom-0')
    expect(dialog.className).toContain('rounded-t-block')
    expect(screen.getByTestId('sheet-overlay')).toBeInTheDocument()
  })
})
