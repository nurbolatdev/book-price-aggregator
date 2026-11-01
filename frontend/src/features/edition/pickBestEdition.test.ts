import { describe, expect, it } from 'vitest'
import type { BookResponse } from '../../api/types'
import { pickBestEdition } from './pickBestEdition'

const book = (editions: BookResponse['editions']): BookResponse => ({
  id: 1,
  editions,
})

describe('pickBestEdition', () => {
  it('picks the cheapest priced edition', () => {
    expect(
      pickBestEdition(
        book([
          { editionId: 1, bestPrice: 500 },
          { editionId: 2, bestPrice: 300 },
        ]),
      ),
    ).toBe(2)
  })

  it('ignores unpriced editions', () => {
    expect(pickBestEdition(book([{ editionId: 1 }, { editionId: 2, bestPrice: 900 }]))).toBe(2)
  })

  it('falls back to the first edition when none is priced', () => {
    expect(pickBestEdition(book([{ editionId: 7 }, { editionId: 8 }]))).toBe(7)
  })

  it('returns null for an empty list', () => {
    expect(pickBestEdition(book([]))).toBeNull()
  })
})
