import { api } from './client'
import type {
  BeaconResponse,
  CreateBeaconRequest,
  UnsubscribeResponse,
  UpdateBeaconRequest,
} from './types'

export async function fetchBeaconForEdition(
  editionId: number,
  signal?: AbortSignal,
): Promise<BeaconResponse | null> {
  const { data } = await api.get<BeaconResponse[]>('/beacons', { params: { editionId }, signal })
  return data[0] ?? null
}

export async function fetchBeacons(signal?: AbortSignal): Promise<BeaconResponse[]> {
  const { data } = await api.get<BeaconResponse[]>('/beacons', { signal })
  return data
}

export async function createBeacon(body: CreateBeaconRequest): Promise<BeaconResponse> {
  const { data } = await api.post<BeaconResponse>('/beacons', body)
  return data
}

export async function updateBeacon(id: number, body: UpdateBeaconRequest): Promise<BeaconResponse> {
  const { data } = await api.patch<BeaconResponse>(`/beacons/${id}`, body)
  return data
}

export async function deleteBeacon(id: number): Promise<void> {
  await api.delete(`/beacons/${id}`)
}

export async function unsubscribeBeacon(token: string): Promise<UnsubscribeResponse> {
  const { data } = await api.post<UnsubscribeResponse>('/beacons/unsubscribe', { token })
  return data
}
