import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PriceVerdict } from './PriceVerdict'

describe('PriceVerdict', () => {
  it('shows the md below-average text with deal colours', () => {
    render(<PriceVerdict verdict={{ label: 'BELOW_AVERAGE', diffPercent: -12 }} />)
    expect(screen.getByText('Ниже средней за 3 месяца на 12%')).toHaveClass(
      'bg-deal-soft',
      'text-deal',
    )
  })

  it('shows the sm below-average text', () => {
    render(<PriceVerdict size="sm" verdict={{ label: 'BELOW_AVERAGE', diffPercent: -12 }} />)
    expect(screen.getByText('Ниже средней на 12%')).toHaveClass('bg-deal-soft', 'text-deal')
  })

  it('shows above-average with muted colours', () => {
    render(<PriceVerdict verdict={{ label: 'ABOVE_AVERAGE', diffPercent: 7 }} />)
    expect(screen.getByText('Выше средней на 7%')).toHaveClass('bg-canvas', 'text-muted')
  })

  it('shows the average text', () => {
    render(<PriceVerdict verdict={{ label: 'AVERAGE', diffPercent: 1 }} />)
    expect(screen.getByText('Обычная цена')).toBeInTheDocument()
  })

  it('renders nothing without enough data', () => {
    const { container, rerender } = render(
      <PriceVerdict verdict={{ label: 'NOT_ENOUGH_DATA' }} />,
    )
    expect(container).toBeEmptyDOMElement()
    rerender(<PriceVerdict />)
    expect(container).toBeEmptyDOMElement()
  })
})
