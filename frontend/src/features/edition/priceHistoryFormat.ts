import type { PricePeriod, PricePoint } from '../../api/types'
import { formatNumber } from '../../lib/format'

const plain = (n: number) => formatNumber(n).replace(/\u00a0/g, ' ')

const PERIOD_TEXT: Record<PricePeriod, string> = {
  '1M': 'за месяц',
  '3M': 'за 3 месяца',
  '6M': 'за 6 месяцев',
}

const monthFormatter = new Intl.DateTimeFormat('ru-RU', { month: 'long', timeZone: 'UTC' })

export function historyLabel(points: PricePoint[], period: PricePeriod): string {
  const first = points[0]?.price ?? 0
  const last = points[points.length - 1]?.price ?? 0
  if (first === last) return `График: минимальная цена не изменилась: ${plain(last)} тенге`
  const verb = last < first ? 'снизилась' : 'выросла'
  return `График: минимальная цена ${verb} с ${plain(first)} до ${plain(last)} тенге ${PERIOD_TEXT[period]}`
}

export function monthTicks(points: PricePoint[]): string[] {
  const seen = new Set<string>()
  const ticks: string[] = []
  let lastTickIndex = -2
  points.forEach((p, i) => {
    if (!p.date) return
    const month = p.date.slice(0, 7)
    if (seen.has(month)) return
    seen.add(month)
    if (i - lastTickIndex < 2) return
    lastTickIndex = i
    ticks.push(p.date)
  })
  return ticks
}

export function monthName(date: string): string {
  return monthFormatter.format(new Date(`${date}T00:00:00Z`))
}

export function formatDate(date: string): string {
  const [y, m, d] = date.split('-')
  return `${d}.${m}.${y}`
}
