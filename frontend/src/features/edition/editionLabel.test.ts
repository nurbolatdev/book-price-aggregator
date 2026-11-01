import { describe, expect, it } from 'vitest'
import { editionLabel } from './editionLabel'

describe('editionLabel', () => {
  it('joins publisher, year and binding', () => {
    expect(editionLabel({ publisher: 'АСТ', year: 2023, binding: 'HARDCOVER' })).toBe(
      'АСТ 2023 · твёрдый',
    )
  })
  it('names paperback', () => {
    expect(editionLabel({ publisher: 'Азбука', year: 2022, binding: 'PAPERBACK' })).toBe(
      'Азбука 2022 · мягкий',
    )
  })
  it('skips unknown binding and missing year', () => {
    expect(editionLabel({ publisher: 'Эксмо', binding: 'UNKNOWN' })).toBe('Эксмо')
  })
})
