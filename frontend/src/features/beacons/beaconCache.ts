import type { QueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/queryKeys'
import type { BeaconResponse, UpdateBeaconRequest } from '../../api/types'

export const BEACON_MUTATION_KEY = ['beacons', 'mutation']

export interface BeaconSnapshot {
  list: BeaconResponse[] | undefined
  edition: BeaconResponse | null | undefined
}

function editionKey(editionId: number) {
  return queryKeys.beaconForEdition(editionId)
}

function setEdition(qc: QueryClient, editionId: number, beacon: BeaconResponse | null) {
  qc.setQueryData<BeaconResponse | null>(editionKey(editionId), beacon)
}

function firstRegularIndex(list: BeaconResponse[]): number {
  const index = list.findIndex((item) => item.status !== 'TRIGGERED')
  return index === -1 ? list.length : index
}

export function snapshot(qc: QueryClient, editionId: number): BeaconSnapshot {
  return {
    list: qc.getQueryData<BeaconResponse[]>(queryKeys.beacons),
    edition: qc.getQueryData<BeaconResponse | null>(editionKey(editionId)),
  }
}

/** Drops a temporary item from the list and puts the edition entry back, if it still holds that item. */
export function discardBeacon(qc: QueryClient, tempId: number, editionId: number, savedEdition: BeaconResponse | null | undefined): void {
  qc.setQueryData<BeaconResponse[] | undefined>(queryKeys.beacons, (list) =>
    list && list.some((item) => item.id === tempId) ? list.filter((item) => item.id !== tempId) : list,
  )
  const current = qc.getQueryData<BeaconResponse | null>(editionKey(editionId))
  if (current?.id !== tempId) return
  if (savedEdition === undefined) qc.removeQueries({ queryKey: editionKey(editionId), exact: true })
  else setEdition(qc, editionId, savedEdition)
}

/** Replaces the item with `replaceId` (or with the same id), or inserts it after the triggered ones. */
export function upsertBeacon(qc: QueryClient, beacon: BeaconResponse, replaceId?: number): void {
  const targetId = replaceId ?? beacon.id
  qc.setQueryData<BeaconResponse[] | undefined>(queryKeys.beacons, (list) => {
    if (!list) return list
    if (list.some((item) => item.id === targetId)) {
      return list.map((item) => (item.id === targetId ? beacon : item))
    }
    const next = [...list]
    next.splice(firstRegularIndex(list), 0, beacon)
    return next
  })
  setEdition(qc, beacon.editionId ?? 0, beacon)
}

export function applyPatch(beacon: BeaconResponse, patch: UpdateBeaconRequest, now: string): BeaconResponse {
  let next: BeaconResponse = { ...beacon }
  if (patch.conditionType) {
    delete next.targetPrice
    delete next.targetPercent
    next = {
      ...next,
      conditionType: patch.conditionType,
      ...(patch.conditionType === 'BELOW_PRICE' ? { targetPrice: patch.targetPrice } : {}),
      ...(patch.conditionType === 'DISCOUNT_PERCENT' ? { targetPercent: patch.targetPercent } : {}),
    }
  }
  if (patch.status) next = { ...next, status: patch.status, statusChangedAt: now }
  return next
}

export function findBeacon(qc: QueryClient, id: number): BeaconResponse | undefined {
  const fromList = qc.getQueryData<BeaconResponse[]>(queryKeys.beacons)?.find((item) => item.id === id)
  if (fromList) return fromList
  const entries = qc.getQueriesData<BeaconResponse | null>({ queryKey: ['beacons', 'edition'] })
  return entries.map(([, value]) => value).find((value) => value?.id === id) ?? undefined
}

export function patchBeacon(qc: QueryClient, id: number, patch: UpdateBeaconRequest): void {
  const current = findBeacon(qc, id)
  if (!current) return
  upsertBeacon(qc, applyPatch(current, patch, new Date().toISOString()))
}

/** Removes the beacon from both caches; returns its index in the list (or -1). */
export function removeBeacon(qc: QueryClient, beacon: BeaconResponse): number {
  const list = qc.getQueryData<BeaconResponse[]>(queryKeys.beacons)
  const index = list ? list.findIndex((item) => item.id === beacon.id) : -1
  if (list && index !== -1) qc.setQueryData(queryKeys.beacons, list.filter((item) => item.id !== beacon.id))
  if (beacon.editionId !== undefined) setEdition(qc, beacon.editionId, null)
  return index
}

export function insertBeaconAt(qc: QueryClient, beacon: BeaconResponse, index: number): void {
  qc.setQueryData<BeaconResponse[] | undefined>(queryKeys.beacons, (list) => {
    if (!list || list.some((item) => item.id === beacon.id)) return list
    const next = [...list]
    next.splice(index < 0 ? next.length : Math.min(index, next.length), 0, beacon)
    return next
  })
  if (beacon.editionId !== undefined) setEdition(qc, beacon.editionId, beacon)
}
