import type { BeaconResponse, EditionDetailResponse, SearchItem } from '../../api/types'
import { editionLabel } from '../edition/editionLabel'

export interface BeaconBook {
  editionId: number
  title: string
  coverUrl?: string
  subtitle: string
  bestPrice: number | null
}

export function bookFromEdition(edition: EditionDetailResponse): BeaconBook {
  return {
    editionId: edition.editionId ?? 0,
    title: edition.book?.title ?? '',
    coverUrl: edition.coverUrl,
    subtitle: editionLabel(edition),
    bestPrice: edition.bestPrice?.price ?? null,
  }
}

export function bookFromSearchItem(item: SearchItem): BeaconBook {
  return {
    editionId: item.editionId ?? 0,
    title: item.title ?? '',
    coverUrl: item.coverUrl,
    subtitle: editionLabel(item),
    bestPrice: item.bestPrice?.price ?? null,
  }
}

/** The list response has no publisher, year or binding, so the author stands in for the label. */
export function bookFromBeacon(beacon: BeaconResponse): BeaconBook {
  return {
    editionId: beacon.editionId ?? 0,
    title: beacon.title ?? '',
    coverUrl: beacon.coverUrl,
    subtitle: beacon.author ?? '',
    bestPrice: beacon.currentBestPrice ?? null,
  }
}
