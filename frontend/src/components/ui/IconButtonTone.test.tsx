import { describe, expect, it } from 'vitest'
import { iconButtonClasses } from './IconButton'

describe('iconButtonClasses tone', () => {
  it('defaults to ink', () => {
    expect(iconButtonClasses()).toContain('text-ink')
  })

  it('accent tone swaps the text colour', () => {
    const c = iconButtonClasses({ tone: 'accent' })
    expect(c).toContain('text-accent')
    expect(c).not.toContain('text-ink')
  })
})
