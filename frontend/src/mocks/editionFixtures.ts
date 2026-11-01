import type {
  BookResponse,
  EditionDetailResponse,
  EditionResponse,
  OfferInfo,
  PriceHistoryResponse,
  PricePeriod,
  PricePoint,
  SimilarEdition,
  StoreCode,
} from '../api/types'
import { drops, searchItems, stores } from './fixtures'

const HOUR = 3_600_000
const DAY = 24 * HOUR

const DELIVERY: Partial<Record<StoreCode, string>> = {
  FLIP: '1–2 дня, Алматы',
  OZON: '5–7 дней',
  MELOMAN: 'Самовывоз',
}

function storeName(code: StoreCode): string {
  return stores.find((s) => s.code === code)?.name ?? code
}

const DESCRIPTION =
  'Роман о визите Воланда в Москву 1930-х, о судьбе Мастера и любви Маргариты. ' +
  'Одна из главных книг русской литературы XX века: сатира, мистика и история о выборе и милосердии. ' +
  'В издании сохранена авторская редакция текста, добавлены комментарии и иллюстрации.'

function toStore(editionId: number, store: StoreCode): string {
  return `https://example.com/${store.toLowerCase()}/${editionId}`
}

function offer(
  editionId: number,
  index: number,
  store: StoreCode,
  price: number,
  extra: Partial<OfferInfo>,
  seenHoursAgo: number,
): OfferInfo {
  return {
    offerId: editionId * 10 + index,
    store,
    storeName: storeName(store),
    price,
    inStock: true,
    deliveryText: DELIVERY[store],
    url: toStore(editionId, store),
    lastSeenAt: new Date(Date.now() - seenHoursAgo * HOUR).toISOString(),
    ...extra,
  }
}

const MASTER_ID = 41

function masterAndMargarita(): EditionDetailResponse {
  return {
    editionId: MASTER_ID,
    book: {
      id: 1,
      title: 'Мастер и Маргарита',
      author: 'Михаил Булгаков',
      description: DESCRIPTION,
    },
    isbn13: '9785170906304',
    publisher: 'АСТ',
    year: 2023,
    binding: 'HARDCOVER',
    language: 'ru',
    pages: 480,
    otherEditions: [
      { editionId: 16, label: 'Азбука 2022 · мягкий', bestPrice: 1890 },
      { editionId: 17, label: 'Эксмо 2021 · подарочное', bestPrice: 9400 },
    ],
    bestPrice: { store: 'FLIP', price: 3150, deliveryText: '1–2 дня', url: toStore(MASTER_ID, 'FLIP') },
    priceVerdict: { avg90d: 3580, diffPercent: -12, label: 'BELOW_AVERAGE' },
    offers: [
      offer(MASTER_ID, 1, 'FLIP', 3150, { oldPrice: 3850 }, 2),
      offer(MASTER_ID, 2, 'OZON', 3720, {}, 4),
      offer(MASTER_ID, 3, 'MELOMAN', 3990, { inStock: false }, 5),
    ],
  }
}

export const similarEditions: SimilarEdition[] = [
  { bookId: 5, editionId: 51, title: 'Собачье сердце', author: 'М. Булгаков', bestPrice: 1450 },
  { bookId: 6, editionId: 52, title: 'Белая гвардия', author: 'М. Булгаков', bestPrice: 2100 },
  { bookId: 7, editionId: 53, title: 'Двенадцать стульев', author: 'И. Ильф, Е. Петров', bestPrice: 2350 },
  { bookId: 8, editionId: 54, title: 'Пикник на обочине', author: 'А. и Б. Стругацкие', bestPrice: 1990 },
]

interface Known {
  editionId: number
  bookId: number
  title: string
  author: string
  publisher?: string
  year?: number
  binding?: EditionResponse['binding']
  offers: { store: StoreCode; price: number }[]
}

function knownEditions(): Known[] {
  const fromSearch: Known[] = searchItems.map((item) => ({
    editionId: item.editionId ?? 0,
    bookId: item.bookId ?? 0,
    title: item.title ?? '',
    author: item.author ?? '',
    publisher: item.publisher,
    year: item.year,
    binding: item.binding,
    offers: (item.offers ?? []).map((o) => ({ store: o.store as StoreCode, price: o.price ?? 0 })),
  }))
  const fromDrops: Known[] = drops.map((d) => ({
    editionId: d.editionId ?? 0,
    bookId: d.bookId ?? 0,
    title: d.title ?? '',
    author: d.author ?? '',
    offers: [{ store: d.store as StoreCode, price: d.price ?? 0 }],
  }))
  const fromSimilar: Known[] = similarEditions.map((s) => ({
    editionId: s.editionId ?? 0,
    bookId: s.bookId ?? 0,
    title: s.title ?? '',
    author: s.author ?? '',
    offers: [{ store: 'FLIP', price: s.bestPrice ?? 0 }],
  }))
  return [...fromSearch, ...fromDrops, ...fromSimilar]
}

const MASTER_BOOK_EDITIONS = [41, 16, 17]

function genericDetail(item: Known): EditionDetailResponse {
  const sorted = [...item.offers].sort((a, b) => a.price - b.price)
  const best = sorted[0]
  const others =
    item.bookId === 1
      ? MASTER_BOOK_EDITIONS.filter((id) => id !== item.editionId).map((id) => {
          const other = id === MASTER_ID ? undefined : knownEditions().find((e) => e.editionId === id)
          return id === MASTER_ID
            ? { editionId: id, label: 'АСТ 2023 · твёрдый', bestPrice: 3150 }
            : {
                editionId: id,
                label: `${other?.publisher ?? ''} ${other?.year ?? ''}`.trim(),
                bestPrice: other?.offers[0]?.price,
              }
        })
      : []
  return {
    editionId: item.editionId,
    book: { id: item.bookId, title: item.title, author: item.author },
    publisher: item.publisher,
    year: item.year,
    binding: item.binding,
    language: 'ru',
    otherEditions: others,
    bestPrice: best
      ? {
          store: best.store,
          price: best.price,
          deliveryText: DELIVERY[best.store],
          url: toStore(item.editionId, best.store),
        }
      : undefined,
    priceVerdict: { label: 'NOT_ENOUGH_DATA' },
    offers: sorted.map((o, i) => offer(item.editionId, i + 1, o.store, o.price, {}, 2 + i)),
  }
}

export function editionDetail(id: number): EditionDetailResponse | undefined {
  if (id === MASTER_ID) return masterAndMargarita()
  const item = knownEditions().find((e) => e.editionId === id)
  return item ? genericDetail(item) : undefined
}

const MASTER_PRICES = [3990, 3990, 3850, 3850, 3720, 3990, 3610, 3610, 3390, 3610, 3380, 3150, 3150]
const LAST_DATE = Date.UTC(2026, 9, 20)

function weeklyPoints(prices: number[], endMs: number): PricePoint[] {
  return prices.map((price, i) => ({
    date: new Date(endMs - (prices.length - 1 - i) * 7 * DAY).toISOString().slice(0, 10),
    price,
  }))
}

function summarize(
  editionId: number,
  period: PricePeriod,
  points: PricePoint[],
): PriceHistoryResponse {
  const prices = points.map((p) => p.price ?? 0)
  const sum = prices.reduce((a, b) => a + b, 0)
  return {
    editionId,
    period,
    min: Math.min(...prices),
    avg: Math.round(sum / prices.length),
    max: Math.max(...prices),
    points,
  }
}

export function priceHistory(id: number, period: PricePeriod): PriceHistoryResponse | undefined {
  const detail = editionDetail(id)
  if (!detail) return undefined
  if (id !== MASTER_ID) {
    const price = detail.bestPrice?.price ?? 0
    return summarize(id, period, [{ date: new Date(LAST_DATE).toISOString().slice(0, 10), price }])
  }
  const threeMonths = weeklyPoints(MASTER_PRICES, LAST_DATE)
  if (period === '3M') {
    return { editionId: id, period, min: 3150, avg: 3580, max: 3990, points: threeMonths }
  }
  if (period === '1M') return summarize(id, period, threeMonths.slice(-5))
  const earlierEnd = LAST_DATE - MASTER_PRICES.length * 7 * DAY
  const earlier = weeklyPoints(Array<number>(MASTER_PRICES.length).fill(3990), earlierEnd)
  return summarize(id, period, [...earlier, ...threeMonths])
}

export function similarFor(id: number): SimilarEdition[] | undefined {
  if (!editionDetail(id)) return undefined
  return id === MASTER_ID ? similarEditions : []
}

export function bookDetail(bookId: number): BookResponse | undefined {
  const items = knownEditions().filter((e) => e.bookId === bookId && e.editionId !== MASTER_ID)
  const includeMaster = bookId === 1
  if (items.length === 0 && !includeMaster) return undefined
  const editions: EditionResponse[] = items.map((e) => ({
    editionId: e.editionId,
    bestPrice: Math.min(...e.offers.map((o) => o.price)),
    publisher: e.publisher,
    year: e.year,
    binding: e.binding,
    language: 'ru',
  }))
  if (includeMaster) {
    editions.unshift({
      editionId: MASTER_ID,
      bestPrice: 3150,
      publisher: 'АСТ',
      year: 2023,
      binding: 'HARDCOVER',
      language: 'ru',
      pages: 480,
      isbn13: '9785170906304',
    })
  }
  const first = items[0]
  return {
    id: bookId,
    title: includeMaster ? 'Мастер и Маргарита' : first?.title,
    author: includeMaster ? 'Михаил Булгаков' : first?.author,
    description: includeMaster ? DESCRIPTION : undefined,
    editions,
  }
}
