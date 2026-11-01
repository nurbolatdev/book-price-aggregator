import { useQuery } from '@tanstack/react-query'
import { STALE_TIME } from '../../app/queryClient'
import { fetchSuggestions } from '../../api/search'
import { queryKeys } from '../../api/queryKeys'
import type { Suggestion } from '../../api/types'
import { useDebounce } from '../../lib/useDebounce'

export const SUGGEST_DEBOUNCE_MS = 300
export const MIN_QUERY_LENGTH = 2

export function useSuggestions(input: string, typed: boolean): Suggestion[] {
  const debounced = useDebounce(input, SUGGEST_DEBOUNCE_MS).trim()
  const enabled = typed && debounced.length >= MIN_QUERY_LENGTH
  const { data } = useQuery({
    queryKey: queryKeys.suggest(debounced),
    queryFn: ({ signal }) => fetchSuggestions(debounced, signal),
    enabled,
    staleTime: STALE_TIME.search,
  })
  return enabled && data ? data.slice(0, 8) : []
}
