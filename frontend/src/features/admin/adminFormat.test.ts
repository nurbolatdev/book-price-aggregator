import { describe, expect, it } from 'vitest'
import {
  ADMIN_PERIODS,
  formatAdminTime,
  formatClock,
  formatDate,
  formatFound,
  formatMb,
  periodLabel,
  perMinute,
} from './adminFormat'

const now = new Date('2026-11-17T18:30:00Z')
const tz = 'UTC'

describe('adminFormat', () => {
  it('formats the clock', () => {
    expect(formatClock('2026-11-17T18:10:00Z', tz)).toBe('18:10')
    expect(formatClock(Date.parse('2026-11-17T09:05:00Z'), tz)).toBe('09:05')
  })

  it('formats admin time relative to now', () => {
    expect(formatAdminTime('2026-11-17T18:00:00Z', now, tz)).toBe('сегодня 18:00')
    expect(formatAdminTime('2026-11-16T09:00:00Z', now, tz)).toBe('вчера 09:00')
    expect(formatAdminTime('2026-10-24T09:00:00Z', now, tz)).toBe('24.10')
    expect(formatAdminTime('2025-10-24T09:00:00Z', now, tz)).toBe('24.10.2025')
  })

  it('handles the year boundary', () => {
    const jan = new Date('2027-01-01T10:00:00Z')
    expect(formatAdminTime('2026-12-31T23:00:00Z', jan, tz)).toBe('вчера 23:00')
    expect(formatAdminTime('2026-12-30T23:00:00Z', jan, tz)).toBe('30.12.2026')
  })

  it('formats found dates', () => {
    expect(formatFound('2026-11-17T01:00:00Z', now, tz)).toBe('найдено сегодня')
    expect(formatFound('2026-11-16T01:00:00Z', now, tz)).toBe('найдено вчера')
    expect(formatFound('2026-10-24T01:00:00Z', now, tz)).toBe('найдено 24.10')
  })

  it('formats dates', () => {
    expect(formatDate('2026-10-20T12:00:00Z', tz)).toBe('20.10.2026')
  })

  it('converts rates and sizes', () => {
    expect(perMinute(0.63)).toBe(38)
    expect(perMinute(0)).toBe(0)
    expect(formatMb(12_582_912)).toBe(12)
  })

  it('labels periods', () => {
    expect(periodLabel('24h')).toBe('24 ч')
    expect(periodLabel('7d')).toBe('7 дней')
    expect(periodLabel('30d')).toBe('30 дней')
    expect(ADMIN_PERIODS.map((p) => p.value)).toEqual(['24h', '7d', '30d'])
  })
})
