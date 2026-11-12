import { useCallback, useEffect, useRef, useState } from 'react'

export function useCountdown(): { secondsLeft: number; start: (seconds: number) => void } {
  const [secondsLeft, setSecondsLeft] = useState(0)
  const timer = useRef<ReturnType<typeof setInterval> | null>(null)

  const stop = useCallback(() => {
    if (timer.current !== null) {
      clearInterval(timer.current)
      timer.current = null
    }
  }, [])

  const start = useCallback(
    (seconds: number) => {
      stop()
      const endsAt = Date.now() + seconds * 1000
      setSecondsLeft(seconds)
      timer.current = setInterval(() => {
        const left = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000))
        setSecondsLeft(left)
        if (left === 0) stop()
      }, 1000)
    },
    [stop],
  )

  useEffect(() => stop, [stop])

  return { secondsLeft, start }
}
