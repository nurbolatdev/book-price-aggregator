import { useCallback, useState } from 'react'
import { ApiError } from '../../api/errors'
import { useCountdown } from '../../lib/useCountdown'

const DEFAULT_WAIT_SECONDS = 60

export interface RateLimit {
  blocked: boolean
  secondsLeft: number
  /** Initial wait, kept constant for the static alert text. */
  waitSeconds: number
  handle: (error: unknown) => boolean
}

export function useRateLimit(): RateLimit {
  const { secondsLeft, start } = useCountdown()
  const [waitSeconds, setWaitSeconds] = useState(0)

  const handle = useCallback(
    (error: unknown) => {
      if (!(error instanceof ApiError) || error.code !== 'RATE_LIMITED') return false
      const seconds = error.retryAfter ?? DEFAULT_WAIT_SECONDS
      setWaitSeconds(seconds)
      start(seconds)
      return true
    },
    [start],
  )

  return { blocked: secondsLeft > 0, secondsLeft, waitSeconds, handle }
}
