import { useEffect } from 'react'

interface PageLike {
  content?: unknown[]
  totalElements?: number
}

/** Moves to the last existing page when the current one is past the end of the list. */
export function useStepBackWhenEmpty(
  data: PageLike | undefined,
  isPlaceholder: boolean,
  page: number,
  size: number,
  changePage: (page: number, options?: { replace?: boolean }) => void,
): void {
  useEffect(() => {
    if (!data || isPlaceholder || page <= 0) return
    const total = data.totalElements ?? 0
    if ((data.content?.length ?? 0) > 0 || total <= 0) return
    const last = Math.max(0, Math.ceil(total / size) - 1)
    if (last < page) changePage(last, { replace: true })
  }, [data, isPlaceholder, page, size, changePage])
}
