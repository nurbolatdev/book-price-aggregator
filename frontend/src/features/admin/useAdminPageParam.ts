import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

export function parsePageParam(raw: string | null): number {
  const n = Number(raw)
  return Number.isInteger(n) && n >= 1 ? n - 1 : 0
}

/** `?page=` is 1-based in the URL and 0-based in the API. */
export function useAdminPageParam(): [number, (page: number) => void] {
  const [params, setParams] = useSearchParams()
  const page = parsePageParam(params.get('page'))
  const setPage = useCallback(
    (next: number) => {
      setParams((prev) => {
        const out = new URLSearchParams(prev)
        if (next <= 0) out.delete('page')
        else out.set('page', String(next + 1))
        return out
      })
    },
    [setParams],
  )
  return [page, setPage]
}
