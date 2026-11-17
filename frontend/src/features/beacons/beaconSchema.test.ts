import { describe, expect, it } from 'vitest'
import { formatPrice } from '../../lib/format'
import { beaconFormSchema, defaultTargetPrice, defaultValues, toConditionRequest } from './beaconSchema'

const base = { conditionType: 'BELOW_PRICE' as const, targetPrice: '', targetPercent: '15' }

function messages(values: typeof base | { conditionType: 'DISCOUNT_PERCENT' | 'ANY_DROP'; targetPrice: string; targetPercent: string }, best: number | null = 3150) {
  const result = beaconFormSchema(best).safeParse(values)
  return result.success ? [] : result.error.issues.map((issue) => issue.message)
}

describe('defaultTargetPrice', () => {
  it('is 10 percent below the best price rounded to ten', () => {
    expect(defaultTargetPrice(3610)).toBe('3250')
    expect(defaultTargetPrice(3150)).toBe('2840')
  })

  it('is empty when no useful target exists', () => {
    expect(defaultTargetPrice(10)).toBe('')
  })
})

describe('beaconFormSchema', () => {
  it('reports each price problem', () => {
    expect(messages({ ...base, targetPrice: '' })).toEqual(['Укажите цену'])
    expect(messages({ ...base, targetPrice: '12.5' })).toEqual(['Цена — целое число в тенге'])
    expect(messages({ ...base, targetPrice: '0' })).toEqual(['Цена должна быть больше 0'])
    expect(messages({ ...base, targetPrice: '3150' })).toEqual([`Укажите цену ниже текущей — сейчас ${formatPrice(3150)}`])
  })

  it('accepts a price below the current one, or any price when there is none', () => {
    expect(messages({ ...base, targetPrice: '2840' })).toEqual([])
    expect(messages({ ...base, targetPrice: '9000' }, null)).toEqual([])
  })

  it('checks the percent range', () => {
    const percent = (targetPercent: string) => ({ conditionType: 'DISCOUNT_PERCENT' as const, targetPrice: '', targetPercent })
    expect(messages(percent('15'))).toEqual([])
    expect(messages(percent('0'))).toEqual(['Скидка — от 1 до 90%'])
    expect(messages(percent('91'))).toEqual(['Скидка — от 1 до 90%'])
  })

  it('lets ANY_DROP pass with empty fields', () => {
    expect(messages({ conditionType: 'ANY_DROP', targetPrice: '', targetPercent: '' })).toEqual([])
  })
})

describe('toConditionRequest', () => {
  it('sends only the selected field as a number', () => {
    expect(toConditionRequest({ conditionType: 'BELOW_PRICE', targetPrice: '2840', targetPercent: '15' })).toEqual({
      conditionType: 'BELOW_PRICE',
      targetPrice: 2840,
    })
    expect(toConditionRequest({ conditionType: 'DISCOUNT_PERCENT', targetPrice: '2840', targetPercent: '20' })).toEqual({
      conditionType: 'DISCOUNT_PERCENT',
      targetPercent: 20,
    })
    expect(toConditionRequest({ conditionType: 'ANY_DROP', targetPrice: '2840', targetPercent: '20' })).toEqual({
      conditionType: 'ANY_DROP',
    })
  })
})

describe('defaultValues', () => {
  const book = { editionId: 41, title: 'T', subtitle: '', bestPrice: 3150 }

  it('starts create mode on BELOW_PRICE, or ANY_DROP without a price', () => {
    expect(defaultValues(book, null)).toEqual({ conditionType: 'BELOW_PRICE', targetPrice: '2840', targetPercent: '15' })
    expect(defaultValues({ ...book, bestPrice: null }, null).conditionType).toBe('ANY_DROP')
  })

  it('uses the beacon values in edit mode', () => {
    expect(defaultValues(book, { id: 3, conditionType: 'DISCOUNT_PERCENT', targetPercent: 20 })).toEqual({
      conditionType: 'DISCOUNT_PERCENT',
      targetPrice: '2840',
      targetPercent: '20',
    })
  })
})
