import { describe, expect, it } from 'vitest'
import { bindingName, formatPrice, formatRelative, languageName, pluralRu } from './format'

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

describe('pluralRu', () => {
  const forms: [string, string, string] = ['издание', 'издания', 'изданий']
  it.each([
    [1, 'издание'],
    [2, 'издания'],
    [5, 'изданий'],
    [11, 'изданий'],
    [21, 'издание'],
    [22, 'издания'],
    [25, 'изданий'],
    [111, 'изданий'],
  ])('%i -> %s', (n, expected) => {
    expect(pluralRu(n, forms)).toBe(expected)
  })
})

describe('formatRelative', () => {
  const now = new Date('2026-10-30T12:00:00Z')
  const ago = (ms: number) => new Date(now.getTime() - ms).toISOString()
  const MIN = 60_000
  const HOUR = 60 * MIN
  const DAY = 24 * HOUR

  it('uses minutes under an hour', () => {
    expect(formatRelative(ago(5 * MIN), now)).toMatch(/5 мин/)
  })
  it('uses hours under a day', () => {
    expect(formatRelative(ago(2 * HOUR), now)).toBe('2 ч назад')
  })
  it('says yesterday for one day', () => {
    expect(formatRelative(ago(DAY), now)).toBe('вчера')
  })
  it('uses days under a week', () => {
    expect(formatRelative(ago(3 * DAY), now)).toMatch(/3 дн/)
  })
  it('uses a date after a week', () => {
    expect(formatRelative('2026-10-20T10:00:00Z', now)).toBe('20.10.2026')
  })
})

describe('languageName', () => {
  it('maps known codes and falls back to the code', () => {
    expect(languageName('ru')).toBe('Русский')
    expect(languageName('kk')).toBe('Қазақша')
    expect(languageName('en')).toBe('English')
    expect(languageName('xx')).toBe('xx')
  })
})

describe('bindingName', () => {
  it('maps bindings', () => {
    expect(bindingName('HARDCOVER')).toBe('Твёрдый')
    expect(bindingName('PAPERBACK')).toBe('Мягкий')
  })
})
