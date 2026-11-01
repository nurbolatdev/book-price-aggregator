import { api } from './client'
import type {
  BookResponse,
  PriceHistoryResponse,
  PricePeriod,
  SimilarEdition,
} from './types'

export async function fetchPriceHistory(
  id: number,
  period: PricePeriod,
  signal?: AbortSignal,
): Promise<PriceHistoryResponse> {
  const { data } = await api.get<PriceHistoryResponse>(`/editions/${id}/price-history`, {
    params: { period },
    signal,
  })
  return data
}

export async function fetchSimilar(
  id: number,
  limit: number,
  signal?: AbortSignal,
): Promise<SimilarEdition[]> {
  const { data } = await api.get<SimilarEdition[]>(`/editions/${id}/similar`, {
    params: { limit },
    signal,
  })
  return data
}

export async function fetchBook(id: number, signal?: AbortSignal): Promise<BookResponse> {
  const { data } = await api.get<BookResponse>(`/books/${id}`, { signal })
  return data
}
