import { describe, expect, it } from 'vitest'
import { formatPrice } from './format'

const nbsp = ' '

describe('formatPrice', () => {
  it('groups thousands', () => {
    expect(formatPrice(3150)).toBe(`3${nbsp}150 ₸`)
  })
  it('keeps small values', () => {
    expect(formatPrice(990)).toBe('990 ₸')
  })
  it('groups millions', () => {
    expect(formatPrice(1234567)).toBe(`1${nbsp}234${nbsp}567 ₸`)
  })
  it('rounds to whole tenge', () => {
    expect(formatPrice(3150.4)).toBe(`3${nbsp}150 ₸`)
  })
})
