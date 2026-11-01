import type { BestOffer, OfferInfo } from '../../api/types'

export function sortOffers(offers: OfferInfo[] = []): OfferInfo[] {
  return [...offers].sort((a, b) => {
    const stock = Number(b.inStock !== false) - Number(a.inStock !== false)
    if (stock !== 0) return stock
    const price = (a.price ?? 0) - (b.price ?? 0)
    if (price !== 0) return price
    return (a.storeName ?? '').localeCompare(b.storeName ?? '', 'ru')
  })
}

export function bestOfferId(offers: OfferInfo[] = [], bestPrice?: BestOffer): number | null {
  const inStock = sortOffers(offers).filter((o) => o.inStock !== false)
  if (inStock.length === 0) return null
  const match = bestPrice ? inStock.find((o) => o.store === bestPrice.store) : undefined
  return (match ?? inStock[0])?.offerId ?? null
}

export function lastSeen(offers: OfferInfo[] = []): string | null {
  let latest: string | null = null
  for (const o of offers) {
    if (o.lastSeenAt && (latest === null || new Date(o.lastSeenAt) > new Date(latest))) {
      latest = o.lastSeenAt
    }
  }
  return latest
}

export function storeNameOf(offers: OfferInfo[] = [], bestPrice?: BestOffer): string {
  if (!bestPrice) return ''
  return offers.find((o) => o.store === bestPrice.store)?.storeName ?? bestPrice.store ?? ''
}
