import { useCallback, useEffect } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'

export function beaconLoginHref(editionId: number): string {
  return `/login?next=${encodeURIComponent(`/editions/${editionId}?beacon=1`)}`
}

/**
 * `?beacon=1` is the state of the beacon window: present and signed in means open.
 * Guests are sent to login and come back to the same address.
 */
export function useBeaconParam(editionId: number): { open: boolean; close: () => void } {
  const [params, setParams] = useSearchParams()
  const status = useAuthStore((s) => s.status)
  const navigate = useNavigate()
  const { state } = useLocation()
  const requested = params.has('beacon')

  useEffect(() => {
    if (!requested || editionId <= 0 || status !== 'anonymous') return
    navigate(beaconLoginHref(editionId), { replace: true })
  }, [requested, editionId, status, navigate])

  const close = useCallback(() => {
    const next = new URLSearchParams(params)
    next.delete('beacon')
    setParams(next, { replace: true, state })
  }, [params, setParams, state])

  return { open: requested && editionId > 0 && status === 'authenticated', close }
}

/** Opens the beacon window by adding `?beacon=1` to the current address. */
export function useOpenBeacon(): () => void {
  const [params, setParams] = useSearchParams()
  const { state } = useLocation()
  return useCallback(() => {
    const next = new URLSearchParams(params)
    next.set('beacon', '1')
    setParams(next, { replace: true, state })
  }, [params, setParams, state])
}
