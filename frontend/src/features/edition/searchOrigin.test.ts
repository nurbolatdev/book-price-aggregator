import { describe, expect, it } from 'vitest'
import { readSearchOrigin } from './searchOrigin'

describe('readSearchOrigin', () => {
  it('reads a search url', () => {
    const from = '/search?q=' + encodeURIComponent('Мастер и Маргарита') + '&sort=PRICE_ASC'
    expect(readSearchOrigin({ from })).toEqual({
      href: from,
      label: 'Поиск «Мастер и Маргарита»',
    })
  })

  it('reads a catalog url', () => {
    expect(readSearchOrigin({ from: '/catalog?stores=FLIP' })).toEqual({
      href: '/catalog?stores=FLIP',
      label: 'Каталог',
    })
  })

  it('rejects anything else', () => {
    expect(readSearchOrigin({ from: '/admin' })).toBeNull()
    expect(readSearchOrigin({ from: 42 })).toBeNull()
    expect(readSearchOrigin(42)).toBeNull()
    expect(readSearchOrigin(undefined)).toBeNull()
  })
})
