import { describe, expect, it } from 'vitest'
import { nextOfferId } from './nextOfferId'

const list = [{ id: 1 }, { id: 2 }, { id: 3 }]

describe('nextOfferId', () => {
  it('picks the following offer from the middle', () => {
    expect(nextOfferId(list, 2)).toBe(3)
  })

  it('picks the previous offer from the end', () => {
    expect(nextOfferId(list, 3)).toBe(2)
  })

  it('returns null for a single offer', () => {
    expect(nextOfferId([{ id: 1 }], 1)).toBeNull()
  })

  it('falls back to the first offer for an unknown id', () => {
    expect(nextOfferId(list, 99)).toBe(1)
  })
})
