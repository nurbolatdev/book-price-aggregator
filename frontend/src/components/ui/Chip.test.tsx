import { describe, expect, it } from 'vitest'
import { chipClasses } from './Chip'

describe('chipClasses', () => {
  it('md keeps the touch height', () => {
    expect(chipClasses()).toContain('min-h-touch')
  })

  it('sm is a compact pill without touch height', () => {
    const sm = chipClasses({ size: 'sm' })
    expect(sm).not.toContain('min-h-touch')
    expect(sm).toContain('py-1.5')
  })
})
