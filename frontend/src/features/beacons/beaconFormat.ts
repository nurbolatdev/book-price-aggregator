import type { BeaconResponse } from '../../api/types'
import { formatPrice } from '../../lib/format'

export function goalText(b: BeaconResponse): string {
  switch (b.conditionType) {
    case 'BELOW_PRICE':
      return `Цель: ниже ${formatPrice(b.targetPrice ?? 0)}`
    case 'DISCOUNT_PERCENT':
      return `Цель: скидка от ${b.targetPercent ?? 0}%`
    default:
      return 'Цель: любое снижение'
  }
}

export function discountPercent(b: BeaconResponse): number {
  const base = b.baselinePrice
  const current = b.currentBestPrice
  if (!base || current == null) return 0
  return Math.round(((base - current) / base) * 100)
}

function yearIn(date: Date, timeZone?: string): number {
  return Number(new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric' }).format(date))
}

export function formatDayMonth(iso: string, now: Date = new Date(), timeZone?: string): string {
  const date = new Date(iso)
  const base = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', timeZone }).format(date)
  const year = yearIn(date, timeZone)
  return year === yearIn(now, timeZone) ? base : `${base} ${year}`
}

function dayNumber(date: Date, timeZone?: string): number {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)
  const get = (type: string) => Number(parts.find((p) => p.type === type)?.value)
  return Date.UTC(get('year'), get('month') - 1, get('day')) / 86_400_000
}

export function formatTriggered(iso: string, now: Date = new Date(), timeZone?: string): string {
  const date = new Date(iso)
  const time = new Intl.DateTimeFormat('ru-RU', {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone,
  }).format(date)
  const diff = dayNumber(now, timeZone) - dayNumber(date, timeZone)
  if (diff === 0) return `сегодня в ${time}`
  if (diff === 1) return `вчера в ${time}`
  return `${formatDayMonth(iso, now, timeZone)} в ${time}`
}

export function priceCaption(b: BeaconResponse, now: Date = new Date(), timeZone?: string): string {
  if (b.status === 'PAUSED') {
    return `пауза с ${formatDayMonth(b.statusChangedAt ?? b.createdAt ?? now.toISOString(), now, timeZone)}`
  }
  const current = b.currentBestPrice
  if (current == null) return 'нет в наличии'
  switch (b.conditionType) {
    case 'ANY_DROP':
      return `было ${formatPrice(b.baselinePrice ?? current)} при установке`
    case 'BELOW_PRICE': {
      const target = b.targetPrice ?? 0
      return current > target ? `ещё ${formatPrice(current - target)} до цели` : 'цель достигнута'
    }
    default: {
      const percent = discountPercent(b)
      return percent > 0 ? `сейчас скидка ${percent}%` : 'скидки пока нет'
    }
  }
}

export function sortBeacons(list: BeaconResponse[]): BeaconResponse[] {
  return [...list.filter((b) => b.status === 'TRIGGERED'), ...list.filter((b) => b.status !== 'TRIGGERED')]
}
