import { useEffect } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { beaconLoginHref } from './useBeaconAction'

/** Handles `?beacon=1`: guests go to login, signed-in users get the parameter removed. */
export function useBeaconParam(editionId: number): void {
  const [params, setParams] = useSearchParams()
  const status = useAuthStore((s) => s.status)
  const navigate = useNavigate()
  const { state } = useLocation()
  const requested = params.has('beacon')

  useEffect(() => {
    if (!requested || editionId <= 0 || status === 'restoring') return
    if (status === 'anonymous') {
      navigate(beaconLoginHref(editionId), { replace: true })
      return
    }
    const next = new URLSearchParams(params)
    next.delete('beacon')
    setParams(next, { replace: true, state })
  }, [requested, editionId, status, navigate, params, setParams, state])
}
