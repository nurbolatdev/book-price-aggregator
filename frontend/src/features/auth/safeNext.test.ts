import { describe, expect, it } from 'vitest'
import { safeNext } from './safeNext'

describe('safeNext', () => {
  it('keeps a local path with query', () => {
    expect(safeNext('/editions/41?beacon=1')).toBe('/editions/41?beacon=1')
  })

  it.each([
    ['https://evil.example'],
    ['//evil.example'],
    ['/\\evil.example'],
    ['javascript:alert(1)'],
    [''],
    [null],
    ['/x\u0000y'],
  ])('rejects %j', (value) => {
    expect(safeNext(value)).toBe('/')
  })
})
