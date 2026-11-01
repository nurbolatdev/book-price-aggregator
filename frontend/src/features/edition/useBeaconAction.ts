import { useAuthStore } from '../../stores/auth'
import { useBeaconForEdition } from './useBeaconForEdition'

export function beaconLoginHref(editionId: number): string {
  return `/login?next=${encodeURIComponent(`/editions/${editionId}?beacon=1`)}`
}

export function useBeaconAction(editionId: number) {
  const status = useAuthStore((s) => s.status)
  const { data } = useBeaconForEdition(editionId)
  if (status !== 'authenticated') {
    return {
      href: beaconLoginHref(editionId),
      label: 'Следить за ценой',
      shortLabel: 'Следить',
      hasBeacon: false,
    }
  }
  const hasBeacon = Boolean(data)
  return {
    href: '/beacons',
    label: hasBeacon ? 'Маячок стоит · изменить' : 'Следить за ценой',
    shortLabel: hasBeacon ? 'Маячок стоит' : 'Следить',
    hasBeacon,
  }
}
