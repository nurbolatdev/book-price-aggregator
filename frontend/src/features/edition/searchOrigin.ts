export interface SearchOrigin {
  href: string
  label: string
}

export function readSearchOrigin(state: unknown): SearchOrigin | null {
  if (typeof state !== 'object' || state === null) return null
  const from = (state as { from?: unknown }).from
  if (typeof from !== 'string') return null
  if (from.startsWith('/catalog')) return { href: from, label: 'Каталог' }
  if (!from.startsWith('/search')) return null
  const q = new URLSearchParams(from.split('?')[1] ?? '').get('q')?.trim()
  return { href: from, label: q ? `Поиск «${q}»` : 'Поиск' }
}
