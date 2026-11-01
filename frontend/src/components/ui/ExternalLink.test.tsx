import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ExternalLink } from './ExternalLink'

describe('ExternalLink', () => {
  it('opens in a new tab without leaking the opener', () => {
    render(<ExternalLink href="https://shop.example/x">В магазин</ExternalLink>)
    const link = screen.getByRole('link', { name: 'В магазин' })
    expect(link).toHaveAttribute('href', 'https://shop.example/x')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })
})
