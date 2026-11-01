import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { EditionDescription } from './EditionDescription'

function stubHeights(scroll: number, client: number) {
  vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(scroll)
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(client)
}

describe('EditionDescription', () => {
  afterEach(() => vi.restoreAllMocks())

  it('toggles a clamped text', () => {
    stubHeights(200, 100)
    render(<EditionDescription text="Длинное описание" />)
    const toggle = screen.getByRole('button', { name: 'Читать полностью' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByText('Длинное описание')).toHaveClass('line-clamp-4')
    fireEvent.click(toggle)
    expect(screen.getByRole('button', { name: 'Свернуть' })).toHaveAttribute(
      'aria-expanded',
      'true',
    )
    expect(screen.getByText('Длинное описание')).not.toHaveClass('line-clamp-4')
  })

  it('has no button when the text fits', () => {
    stubHeights(100, 100)
    render(<EditionDescription text="Коротко" />)
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('renders nothing without a text', () => {
    const { container } = render(<EditionDescription />)
    expect(container).toBeEmptyDOMElement()
  })
})
