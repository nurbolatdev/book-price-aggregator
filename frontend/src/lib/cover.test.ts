import { describe, expect, it } from 'vitest'
import { coverColor } from './cover'

describe('coverColor', () => {
  it.each([
    [0, 'bg-cover-0'],
    [5, 'bg-cover-5'],
    [6, 'bg-cover-0'],
    [13, 'bg-cover-1'],
  ])('edition %i -> %s', (id, expected) => {
    expect(coverColor(id)).toBe(expected)
  })
})
