import { describe, expect, it } from 'vitest'
import { formatDate, historyLabel, monthName, monthTicks } from './priceHistoryFormat'

const pt = (date: string, price: number) => ({ date, price })

describe('priceHistoryFormat', () => {
  it('describes a falling price', () => {
    expect(historyLabel([pt('2026-07-28', 3990), pt('2026-10-20', 3150)], '3M')).toBe(
      'График: минимальная цена снизилась с 3 990 до 3 150 тенге за 3 месяца',
    )
  })

  it('describes a rising price', () => {
    expect(historyLabel([pt('2026-10-01', 3000), pt('2026-10-20', 3500)], '1M')).toBe(
      'График: минимальная цена выросла с 3 000 до 3 500 тенге за месяц',
    )
  })

  it('describes a flat price and the 6M wording', () => {
    expect(historyLabel([pt('2026-05-01', 3150), pt('2026-10-20', 3150)], '6M')).toBe(
      'График: минимальная цена не изменилась: 3 150 тенге',
    )
    expect(historyLabel([pt('2026-05-01', 3150), pt('2026-10-20', 3000)], '6M')).toContain(
      'за 6 месяцев',
    )
  })

  it('returns one tick per month', () => {
    const ticks = monthTicks([
      pt('2026-07-28', 1),
      pt('2026-08-04', 1),
      pt('2026-08-11', 1),
      pt('2026-09-01', 1),
    ])
    expect(ticks).toEqual(['2026-07-28', '2026-08-04', '2026-09-01'])
  })

  it('formats month name and date', () => {
    expect(monthName('2026-07-28')).toBe('июль')
    expect(formatDate('2026-07-28')).toBe('28.07.2026')
  })
})
