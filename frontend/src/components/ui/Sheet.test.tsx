import { fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { Sheet } from './Sheet'

function Harness() {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button onClick={() => setOpen(true)}>Открыть</button>
      <Sheet open={open} onClose={() => setOpen(false)} title="Фильтры">
        <button>Первая</button>
        <button>Последняя</button>
      </Sheet>
    </>
  )
}

describe('Sheet', () => {
  it('is a dialog named by its title and focuses inside on open', () => {
    render(<Harness />)
    const opener = screen.getByRole('button', { name: 'Открыть' })
    opener.focus()
    fireEvent.click(opener)
    const dialog = screen.getByRole('dialog', { name: 'Фильтры' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog).toContainElement(document.activeElement as HTMLElement)
  })

  it('closes on Escape and returns focus to the opener', () => {
    render(<Harness />)
    const opener = screen.getByRole('button', { name: 'Открыть' })
    opener.focus()
    fireEvent.click(opener)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(opener).toHaveFocus()
  })

  it('traps Tab from the last element to the first', () => {
    render(<Harness />)
    fireEvent.click(screen.getByRole('button', { name: 'Открыть' }))
    const last = screen.getByRole('button', { name: 'Последняя' })
    last.focus()
    fireEvent.keyDown(last, { key: 'Tab' })
    expect(screen.getByRole('button', { name: 'Первая' })).toHaveFocus()
  })

  it('closes on overlay click', () => {
    render(<Harness />)
    fireEvent.click(screen.getByRole('button', { name: 'Открыть' }))
    fireEvent.click(screen.getByTestId('sheet-overlay'))
    expect(screen.queryByRole('dialog')).toBeNull()
  })
})
