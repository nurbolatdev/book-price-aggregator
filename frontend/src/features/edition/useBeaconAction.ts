import { useAuthStore } from '../../stores/auth'
import { useBeaconForEdition } from './useBeaconForEdition'
import { beaconLoginHref, useOpenBeacon } from './useBeaconParam'

export type BeaconAction =
  | { kind: 'link'; href: string; label: string; shortLabel: string; hasBeacon: boolean }
  | { kind: 'button'; onOpen: () => void; label: string; shortLabel: string; hasBeacon: boolean }

export function useBeaconAction(editionId: number): BeaconAction {
  const status = useAuthStore((s) => s.status)
  const { data } = useBeaconForEdition(editionId)
  const onOpen = useOpenBeacon()
  if (status !== 'authenticated') {
    return {
      kind: 'link',
      href: beaconLoginHref(editionId),
      label: 'Следить за ценой',
      shortLabel: 'Следить',
      hasBeacon: false,
    }
  }
  const hasBeacon = Boolean(data)
  return {
    kind: 'button',
    onOpen,
    label: hasBeacon ? 'Маячок стоит · изменить' : 'Следить за ценой',
    shortLabel: hasBeacon ? 'Маячок стоит' : 'Следить',
    hasBeacon,
  }
}
