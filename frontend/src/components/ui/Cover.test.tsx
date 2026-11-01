import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { coverColor } from '../../lib/cover'
import { Cover } from './Cover'

describe('Cover', () => {
  it('renders a colour plate with the title when there is no url', () => {
    const { container } = render(<Cover editionId={7} title="Чистый код" className="h-10 w-10" />)
    expect(screen.getByText('Чистый код')).toBeInTheDocument()
    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByTestId('cover-plate')).toHaveClass(coverColor(7))
  })

  it('renders a lazy image when a url is given', () => {
    const { container } = render(<Cover editionId={1} title="T" coverUrl="http://x/c.jpg" />)
    expect(container.querySelector('img')).toHaveAttribute('loading', 'lazy')
  })

  it('falls back to the plate on image error', () => {
    const { container } = render(<Cover editionId={1} title="Fallback" coverUrl="http://x/c.jpg" />)
    fireEvent.error(container.querySelector('img')!)
    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByText('Fallback')).toBeInTheDocument()
  })
})
