import { useCallback, useRef } from 'react'

/** Ignores calls made while the previous one is still running. */
export function useSingleSubmit<A extends unknown[]>(
  handler: (...args: A) => Promise<void>,
): (...args: A) => Promise<void> {
  const running = useRef(false)
  return useCallback(
    async (...args: A) => {
      if (running.current) return
      running.current = true
      try {
        await handler(...args)
      } finally {
        running.current = false
      }
    },
    [handler],
  )
}
