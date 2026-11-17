import { describe, expect, it } from 'vitest'
import type { BeaconResponse } from '../../api/types'
import { discountPercent, formatTriggered, goalText, priceCaption, sortBeacons } from './beaconFormat'

const norm = (text: string) => text.replace(/\s/g, ' ')

const now = new Date('2026-11-17T12:00:00Z')

const base: BeaconResponse = {
  id: 1,
  editionId: 7,
  title: 'T',
  status: 'ACTIVE',
  conditionType: 'ANY_DROP',
  currentBestPrice: 5290,
  baselinePrice: 5990,
  statusChangedAt: '2026-10-12T08:00:00Z',
}

describe('goalText', () => {
  it('describes the three conditions', () => {
    expect(norm(goalText(base))).toBe('Цель: любое снижение')
    expect(norm(goalText({ ...base, conditionType: 'BELOW_PRICE', targetPrice: 3200 }))).toBe('Цель: ниже 3 200 ₸')
    expect(norm(goalText({ ...base, conditionType: 'DISCOUNT_PERCENT', targetPercent: 15 }))).toBe('Цель: скидка от 15%')
  })
})

describe('priceCaption', () => {
  it('covers every case', () => {
    expect(norm(priceCaption(base, now, 'UTC'))).toBe('было 5 990 ₸ при установке')
    const below = { ...base, conditionType: 'BELOW_PRICE' as const, targetPrice: 8000, currentBestPrice: 8900 }
    expect(norm(priceCaption(below, now, 'UTC'))).toBe('ещё 900 ₸ до цели')
    expect(norm(priceCaption({ ...below, currentBestPrice: 7900 }, now, 'UTC'))).toBe('цель достигнута')
    const pct = { ...base, conditionType: 'DISCOUNT_PERCENT' as const, targetPercent: 15, baselinePrice: 7110, currentBestPrice: 6400 }
    expect(discountPercent(pct)).toBe(10)
    expect(norm(priceCaption(pct, now, 'UTC'))).toBe('сейчас скидка 10%')
    expect(norm(priceCaption({ ...pct, currentBestPrice: 7110 }, now, 'UTC'))).toBe('скидки пока нет')
    expect(norm(priceCaption({ ...base, status: 'PAUSED' }, now, 'UTC'))).toBe('пауза с 12 октября')
    expect(norm(priceCaption({ ...base, currentBestPrice: undefined }, now, 'UTC'))).toBe('нет в наличии')
  })
})

describe('formatTriggered', () => {
  it('names today, yesterday and older dates', () => {
    expect(formatTriggered('2026-11-17T09:12:00Z', now, 'UTC')).toBe('сегодня в 09:12')
    expect(formatTriggered('2026-11-16T09:12:00Z', now, 'UTC')).toBe('вчера в 09:12')
    expect(formatTriggered('2026-10-20T09:12:00Z', now, 'UTC')).toBe('20 октября в 09:12')
    expect(formatTriggered('2025-10-20T09:12:00Z', now, 'UTC')).toBe('20 октября 2025 в 09:12')
  })
})

describe('sortBeacons', () => {
  it('puts triggered first and keeps the order of the rest', () => {
    const list = [
      { ...base, id: 1 },
      { ...base, id: 2, status: 'TRIGGERED' as const },
      { ...base, id: 3, status: 'PAUSED' as const },
    ]
    expect(sortBeacons(list).map((b) => b.id)).toEqual([2, 1, 3])
  })
})
