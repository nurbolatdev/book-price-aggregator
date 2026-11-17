import type { EditionDetailResponse } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { BeaconModal } from '../beacons/BeaconModal'
import { bookFromEdition } from '../beacons/beaconBook'
import { useBeaconForEdition } from './useBeaconForEdition'

interface EditionBeaconModalProps {
  edition: EditionDetailResponse
  editionId: number
  open: boolean
  onClose: () => void
}

/** Opens only after the lookup, so the window never flips from create to edit. */
export function EditionBeaconModal({ edition, editionId, open, onClose }: EditionBeaconModalProps) {
  const authenticated = useAuthStore((s) => s.status === 'authenticated')
  const query = useBeaconForEdition(editionId)
  if (!open || !authenticated || query.isPending) return null
  return <BeaconModal open book={bookFromEdition(edition)} beacon={query.data ?? null} onClose={onClose} />
}
