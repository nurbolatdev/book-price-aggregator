import { api } from './client'
import type { BeaconResponse } from './types'

export async function fetchBeaconForEdition(
  editionId: number,
  signal?: AbortSignal,
): Promise<BeaconResponse | null> {
  const { data } = await api.get<BeaconResponse[]>('/beacons', { params: { editionId }, signal })
  return data[0] ?? null
}
