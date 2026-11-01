import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { coverColor } from '../../lib/cover'
import { Cover } from './Cover'

describe('Cover', () => {
  it('renders a colour plate with the title when there is no url', () => {
    const { container } = render(
      <Cover editionId={7} title="Чистый код" className="h-10 w-10" />,
    )
    expect(screen.getByText('Чистый код')).toBeInTheDocument()
    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByTestId('cover-plate')).toHaveClass(coverColor(7))
  })

  it('renders a lazy image when a url is given', () => {
    const { container } = render(
      <Cover editionId={1} title="T" coverUrl="http://x/c.jpg" />,
    )
    expect(container.querySelector('img')).toHaveAttribute('loading', 'lazy')
  })

  it('falls back to the plate on image error', () => {
    const { container } = render(
      <Cover editionId={1} title="Fallback" coverUrl="http://x/c.jpg" />,
    )
    fireEvent.error(container.querySelector('img')!)
    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByText('Fallback')).toBeInTheDocument()
  })

  it('uses responsive presets per size prop', () => {
    const { rerender } = render(<Cover editionId={1} title="T" size="drop" />)
    expect(screen.getByTestId('cover-plate')).toHaveClass(
      'p-2.5',
      'text-[13px]',
      'font-bold',
      'md:p-4',
      'md:text-lg',
    )
    rerender(<Cover editionId={1} title="T" size="result" />)
    expect(screen.getByTestId('cover-plate')).toHaveClass('max-md:p-0', 'md:text-xs')
    expect(screen.getByText('T')).toHaveClass('max-md:hidden')
  })

  it('detail preset hides the title on phone and sets md typography', () => {
    render(<Cover editionId={1} title="Detail" size="detail" />)
    expect(screen.getByTestId('cover-plate')).toHaveClass(
      'max-md:p-0',
      'md:p-6',
      'md:font-display',
      'md:text-2xl',
    )
    expect(screen.getByText('Detail')).toHaveClass('max-md:hidden')
  })
})
