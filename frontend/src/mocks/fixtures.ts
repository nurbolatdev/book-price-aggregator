import type {
  DropItem,
  EditionDetailResponse,
  SearchFacets,
  SearchItem,
  SearchPage,
  StoreResponse,
} from '../api/types'

export const stores: StoreResponse[] = [
  { code: 'KASPI', name: 'Kaspi.kz', enabled: false },
  { code: 'FLIP', name: 'Flip.kz', enabled: true },
  { code: 'MELOMAN', name: 'Meloman', enabled: true },
  { code: 'WILDBERRIES', name: 'Wildberries', enabled: false },
  { code: 'OZON', name: 'Ozon', enabled: true },
]

export const popular: string[] = [
  'Мастер и Маргарита',
  'Атомные привычки',
  'Абай жолы',
  'Чистый код',
]

export const drops: DropItem[] = [
  {
    editionId: 6,
    bookId: 1,
    title: 'Мастер и Маргарита',
    author: 'Михаил Булгаков',
    store: 'FLIP',
    price: 3150,
    previousPrice: 3840,
    dropPercent: 18,
    storesCount: 3,
  },
  {
    editionId: 7,
    bookId: 2,
    title: 'Атомные привычки',
    author: 'Джеймс Клир',
    store: 'OZON',
    price: 5290,
    previousPrice: 6010,
    dropPercent: 12,
    storesCount: 3,
  },
  {
    editionId: 8,
    bookId: 3,
    title: 'Абай жолы',
    author: 'Мұхтар Әуезов',
    store: 'MELOMAN',
    price: 6400,
    previousPrice: 7110,
    dropPercent: 10,
    storesCount: 2,
  },
  {
    editionId: 9,
    bookId: 4,
    title: 'Чистый код',
    author: 'Роберт Мартин',
    store: 'FLIP',
    price: 8900,
    previousPrice: 9780,
    dropPercent: 9,
    storesCount: 3,
  },
]

const UPDATED_AT = '2026-10-29T10:00:00Z'

const firstPage: SearchItem[] = [
  {
    editionId: 12,
    bookId: 1,
    title: 'Мастер и Маргарита',
    author: 'Михаил Булгаков',
    publisher: 'АСТ',
    year: 2023,
    binding: 'HARDCOVER',
    bestPrice: { store: 'FLIP', price: 3150, inStock: true },
    offers: [
      { store: 'FLIP', price: 3150 },
      { store: 'OZON', price: 3720 },
      { store: 'MELOMAN', price: 3990 },
    ],
    storesCount: 3,
    updatedAt: UPDATED_AT,
  },
  {
    editionId: 16,
    bookId: 1,
    title: 'Мастер и Маргарита',
    author: 'Михаил Булгаков',
    publisher: 'Азбука',
    year: 2022,
    binding: 'PAPERBACK',
    bestPrice: { store: 'OZON', price: 1890, inStock: true },
    offers: [
      { store: 'OZON', price: 1890 },
      { store: 'FLIP', price: 2040 },
    ],
    storesCount: 2,
    updatedAt: UPDATED_AT,
  },
  {
    editionId: 17,
    bookId: 1,
    title: 'Мастер и Маргарита (подарочное издание)',
    author: 'Михаил Булгаков',
    publisher: 'Эксмо',
    year: 2021,
    binding: 'UNKNOWN',
    bestPrice: { store: 'MELOMAN', price: 9400, inStock: true },
    offers: [
      { store: 'MELOMAN', price: 9400 },
      { store: 'FLIP', price: 10200 },
    ],
    storesCount: 2,
    updatedAt: UPDATED_AT,
  },
]

const secondPageIds = [13, 14, 15, 18, 19, 20, 21, 22, 23]

const secondPage: SearchItem[] = secondPageIds.map((editionId, index) => ({
  editionId,
  bookId: 1,
  title: index === 0 ? 'Мастер и Маргарита' : `Мастер и Маргарита, издание ${editionId}`,
  author: 'Михаил Булгаков',
  publisher: index === 0 ? 'Махаон' : 'Эксмо',
  year: index === 0 ? 2020 : 2015 + (index % 8),
  binding: index % 2 === 0 ? 'HARDCOVER' : 'PAPERBACK',
  bestPrice: { store: 'FLIP', price: index === 0 ? 4200 : 3000 + index * 150, inStock: true },
  offers: [
    { store: 'FLIP', price: index === 0 ? 4200 : 3000 + index * 150 },
    { store: 'OZON', price: (index === 0 ? 4200 : 3000 + index * 150) + 400 },
  ],
  storesCount: 2,
  updatedAt: UPDATED_AT,
}))

export const searchItems: SearchItem[] = [...firstPage, ...secondPage]

export function searchPage(page: number): SearchPage {
  const content = page === 0 ? firstPage : page === 1 ? secondPage : []
  return {
    content,
    page,
    size: 20,
    totalElements: searchItems.length,
    totalPages: 2,
  }
}

export const emptySearchPage: SearchPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

export const facets: SearchFacets = {
  stores: [
    { code: 'FLIP', count: 6 },
    { code: 'MELOMAN', count: 4 },
    { code: 'OZON', count: 8 },
  ],
  bindings: [
    { value: 'HARDCOVER', count: 7 },
    { value: 'PAPERBACK', count: 5 },
  ],
  languages: [
    { value: 'ru', count: 11 },
    { value: 'kk', count: 1 },
    { value: 'en', count: 0 },
  ],
}

export function suggestions(q: string): { bookId: number; title: string; author: string; label: string }[] {
  const needle = q.toLowerCase()
  const seen = new Set<string>()
  const result: { bookId: number; title: string; author: string; label: string }[] = []
  for (const item of [...searchItems, ...drops]) {
    const title = item.title ?? ''
    const author = item.author ?? ''
    if (!title.toLowerCase().includes(needle) || seen.has(title)) continue
    seen.add(title)
    result.push({ bookId: item.bookId ?? 0, title, author, label: `${title} — ${author}` })
    if (result.length === 8) break
  }
  return result
}

export function editionDetail(id: number): EditionDetailResponse | undefined {
  const item = searchItems.find((e) => e.editionId === id)
  if (item) {
    return {
      editionId: id,
      book: { id: item.bookId, title: item.title, author: item.author },
      publisher: item.publisher,
      year: item.year,
      binding: item.binding,
      language: 'ru',
    }
  }
  const drop = drops.find((e) => e.editionId === id)
  if (drop) {
    return {
      editionId: id,
      book: { id: drop.bookId, title: drop.title, author: drop.author },
      language: 'ru',
    }
  }
  return undefined
}
