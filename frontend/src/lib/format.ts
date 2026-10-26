const priceFormatter = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 })

export function formatPrice(value: number): string {
  return `${priceFormatter.format(value)} ₸`
}
