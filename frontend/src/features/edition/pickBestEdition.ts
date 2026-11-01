import type { BookResponse } from '../../api/types'

export function pickBestEdition(book: BookResponse): number | null {
  const editions = book.editions ?? []
  let best: { id: number; price: number } | null = null
  for (const edition of editions) {
    if (edition.editionId == null || edition.bestPrice == null) continue
    if (best === null || edition.bestPrice < best.price) {
      best = { id: edition.editionId, price: edition.bestPrice }
    }
  }
  if (best) return best.id
  return editions[0]?.editionId ?? null
}
