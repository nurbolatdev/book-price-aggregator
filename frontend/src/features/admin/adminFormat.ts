export type AdminPeriod = '24h' | '7d' | '30d'

export const ADMIN_PERIODS: { value: AdminPeriod; label: string }[] = [
  { value: '24h', label: '24 ч' },
  { value: '7d', label: '7 дней' },
  { value: '30d', label: '30 дней' },
]

export function periodLabel(period: AdminPeriod): string {
  return ADMIN_PERIODS.find((p) => p.value === period)?.label ?? '24 ч'
}

interface Parts {
  year: number
  month: number
  day: number
  hour: number
  minute: number
}

function parts(ms: number, timeZone?: string): Parts {
  const fmt = new Intl.DateTimeFormat('en-GB', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  })
  const out: Record<string, number> = {}
  const get = (k: string) => out[k] ?? 0
  for (const p of fmt.formatToParts(new Date(ms))) {
    if (p.type !== 'literal') out[p.type] = Number(p.value)
  }
  return { year: get('year'), month: get('month'), day: get('day'), hour: get('hour'), minute: get('minute') }
}

const two = (n: number) => String(n).padStart(2, '0')
const dayKey = (p: Parts) => Date.UTC(p.year, p.month - 1, p.day)
const DAY = 86_400_000

export function formatClock(value: number | string, timeZone?: string): string {
  const p = parts(typeof value === 'number' ? value : Date.parse(value), timeZone)
  return `${two(p.hour)}:${two(p.minute)}`
}

function dayWord(iso: string, now: Date, timeZone?: string): { word: string | null; date: string } {
  const p = parts(Date.parse(iso), timeZone)
  const n = parts(now.getTime(), timeZone)
  const diff = Math.round((dayKey(n) - dayKey(p)) / DAY)
  const short = `${two(p.day)}.${two(p.month)}`
  if (diff === 0) return { word: 'сегодня', date: short }
  if (diff === 1) return { word: 'вчера', date: short }
  return { word: null, date: p.year === n.year ? short : `${short}.${p.year}` }
}

export function formatAdminTime(iso: string, now: Date = new Date(), timeZone?: string): string {
  const { word, date } = dayWord(iso, now, timeZone)
  return word ? `${word} ${formatClock(iso, timeZone)}` : date
}

export function formatFound(iso: string, now: Date = new Date(), timeZone?: string): string {
  const { word, date } = dayWord(iso, now, timeZone)
  return `найдено ${word ?? date}`
}

export function formatDate(iso: string, timeZone?: string): string {
  const p = parts(Date.parse(iso), timeZone)
  return `${two(p.day)}.${two(p.month)}.${p.year}`
}

export function perMinute(ackRatePerSecond: number): number {
  return Math.round(ackRatePerSecond * 60)
}

export function formatMb(bytes: number): number {
  return Math.round(bytes / 1_048_576)
}
