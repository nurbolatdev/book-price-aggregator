const priceFormatter = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 })

export function formatNumber(value: number): string {
  return priceFormatter.format(value)
}

export function formatPrice(value: number): string {
  return `${formatNumber(value)} ₸`
}

/** Fixed 3-1-2-6-1 grouping for readability; real registrant ranges need the ISBN range table. */
export function formatIsbn13(isbn: string): string {
  if (!/^\d{13}$/.test(isbn)) return isbn
  return `${isbn.slice(0, 3)}-${isbn.slice(3, 4)}-${isbn.slice(4, 6)}-${isbn.slice(6, 12)}-${isbn.slice(12)}`
}

export function pluralRu(n: number, [one, few, many]: [string, string, string]): string {
  const abs = Math.abs(n)
  const mod100 = abs % 100
  const mod10 = abs % 10
  if (mod100 >= 11 && mod100 <= 14) return many
  if (mod10 === 1) return one
  if (mod10 >= 2 && mod10 <= 4) return few
  return many
}

const relativeFormatter = new Intl.RelativeTimeFormat('ru', { numeric: 'auto', style: 'short' })
const dateFormatter = new Intl.DateTimeFormat('ru-RU')

export function formatRelative(iso: string, now: Date = new Date()): string {
  const date = new Date(iso)
  const diffMs = now.getTime() - date.getTime()
  const minutes = Math.floor(diffMs / 60_000)
  if (minutes < 60) return relativeFormatter.format(-Math.max(minutes, 0), 'minute')
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return relativeFormatter.format(-hours, 'hour')
  const days = Math.floor(hours / 24)
  if (days < 7) return relativeFormatter.format(-days, 'day')
  return dateFormatter.format(date)
}

export function languageName(code: string): string {
  switch (code) {
    case 'ru':
      return 'Русский'
    case 'kk':
      return 'Қазақша'
    case 'en':
      return 'English'
    default:
      return code
  }
}

export function bindingName(binding: string): string {
  switch (binding) {
    case 'HARDCOVER':
      return 'Твёрдый'
    case 'PAPERBACK':
      return 'Мягкий'
    default:
      return binding
  }
}
