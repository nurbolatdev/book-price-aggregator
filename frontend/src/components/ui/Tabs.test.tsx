import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Tabs } from './Tabs'

const items = [
  { id: 'all', label: 'Все' },
  { id: 'a', label: 'Первая' },
  { id: 'b', label: 'Вторая' },
]

function setup(value = 'a') {
  const onChange = vi.fn()
  render(<Tabs label="Фильтр" items={items} value={value} onChange={onChange} panelId="panel" idPrefix="t" />)
  return onChange
}

describe('Tabs', () => {
  it('exposes tablist semantics', () => {
    setup()
    expect(screen.getByRole('tablist', { name: 'Фильтр' })).toBeInTheDocument()
    const tabs = screen.getAllByRole('tab')
    expect(tabs.map((t) => t.getAttribute('aria-selected'))).toEqual(['false', 'true', 'false'])
    expect(tabs.map((t) => t.tabIndex)).toEqual([-1, 0, -1])
    tabs.forEach((t) => expect(t).toHaveAttribute('aria-controls', 'panel'))
  })

  it('selects on click', () => {
    const onChange = setup()
    fireEvent.click(screen.getByRole('tab', { name: 'Вторая' }))
    expect(onChange).toHaveBeenCalledWith('b')
  })

  it('wraps with arrows and jumps with Home/End', () => {
    const onChange = setup('b')
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Вторая' }), { key: 'ArrowRight' })
    expect(onChange).toHaveBeenLastCalledWith('all')
    expect(screen.getByRole('tab', { name: 'Все' })).toHaveFocus()

    fireEvent.keyDown(screen.getByRole('tab', { name: 'Все' }), { key: 'ArrowLeft' })
    expect(onChange).toHaveBeenLastCalledWith('b')
    expect(screen.getByRole('tab', { name: 'Вторая' })).toHaveFocus()

    fireEvent.keyDown(screen.getByRole('tab', { name: 'Первая' }), { key: 'Home' })
    expect(onChange).toHaveBeenLastCalledWith('all')
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Первая' }), { key: 'End' })
    expect(onChange).toHaveBeenLastCalledWith('b')
  })
})
