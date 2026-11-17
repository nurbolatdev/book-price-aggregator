import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

export const BEACON_TABS = ['all', 'triggered', 'active', 'paused'] as const
export type BeaconTab = (typeof BEACON_TABS)[number]

export function useBeaconTab(): [BeaconTab, (tab: string) => void] {
  const [params, setParams] = useSearchParams()
  const raw = params.get('tab')
  const tab = (BEACON_TABS as readonly string[]).includes(raw ?? '') ? (raw as BeaconTab) : 'all'
  const setTab = useCallback(
    (next: string) => {
      setParams(
        (current) => {
          const copy = new URLSearchParams(current)
          if (next === 'all') copy.delete('tab')
          else copy.set('tab', next)
          return copy
        },
        { replace: true },
      )
    },
    [setParams],
  )
  return [tab, setTab]
}
