import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SkeletonText } from './Skeleton'

describe('SkeletonText', () => {
  it('renders the requested number of bars and is hidden from assistive tech', () => {
    const { container } = render(<SkeletonText lines={4} />)
    const root = container.firstElementChild as HTMLElement
    expect(root).toHaveAttribute('aria-hidden', 'true')
    expect(root.children).toHaveLength(4)
  })
})
