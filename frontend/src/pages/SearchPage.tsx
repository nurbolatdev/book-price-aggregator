import { Navigate, useLocation, useSearchParams } from 'react-router-dom'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { Button } from '../components/ui/Button'
import { FiltersPanel } from '../features/search/FiltersPanel'
import { EditionResultCard } from '../features/search/EditionResultCard'
import { ResultCardSkeleton } from '../features/search/ResultCardSkeleton'
import { SearchEmpty } from '../features/search/SearchEmpty'
import { SortSelect } from '../features/search/SortSelect'
import {
  countActiveFilters,
  toApiParams,
  useSearchFilters,
  type SearchMode,
} from '../features/search/useSearchFilters'
import { useSearchResults } from '../features/search/useSearchResults'
import { pluralRu } from '../lib/format'

const EDITIONS: [string, string, string] = ['издание', 'издания', 'изданий']

function SearchResults({ mode }: { mode: SearchMode }) {
  const { filters, setFilters, resetFilters } = useSearchFilters(mode)
  const query = useSearchResults(toApiParams(filters, mode))
  const items = query.data?.pages.flatMap((p) => p.content ?? []) ?? []
  const total = query.data?.pages[0]?.totalElements ?? 0
  const left = Math.max(total - items.length, 0)
  const q = filters.q.trim()

  return (
    <div className="mx-auto flex w-full max-w-page items-start gap-7 px-4 pb-14 pt-7 md:px-6">
      <FiltersPanel mode={mode} filters={filters} onChange={setFilters} />
      <main className="flex min-w-0 flex-[999_1_560px] flex-col gap-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-col gap-1">
            <h1 className="font-display text-2xl font-medium">{mode === 'catalog' ? 'Каталог' : `«${q}»`}</h1>
            {query.isSuccess && total > 0 && (
              <p className="text-[15px] text-muted">
                Найдено {total} {pluralRu(total, EDITIONS)}
              </p>
            )}
          </div>
          <SortSelect mode={mode} value={filters.sort} onChange={(sort) => setFilters({ sort })} />
        </div>

        {query.isPending ? (
          <div className="flex flex-col gap-4">
            {[0, 1, 2].map((i) => (
              <ResultCardSkeleton key={i} />
            ))}
          </div>
        ) : query.isError ? (
          <ErrorBlock message="Не удалось загрузить результаты." onRetry={() => void query.refetch()} />
        ) : items.length === 0 ? (
          <SearchEmpty
            query={mode === 'search' ? q : undefined}
            activeFilters={countActiveFilters(filters)}
            onReset={resetFilters}
          />
        ) : (
          <>
            <div className="flex flex-col gap-4">
              {items.map((item) => (
                <EditionResultCard key={item.editionId} item={item} />
              ))}
            </div>
            {query.hasNextPage && left > 0 && (
              <Button
                variant="secondary"
                size="lg"
                className="self-center"
                loading={query.isFetchingNextPage}
                onClick={() => void query.fetchNextPage()}
              >
                Показать ещё {left} {pluralRu(left, EDITIONS)}
              </Button>
            )}
          </>
        )}
      </main>
    </div>
  )
}

export default function SearchPage() {
  const { pathname } = useLocation()
  const [params] = useSearchParams()
  const mode: SearchMode = pathname.startsWith('/catalog') ? 'catalog' : 'search'

  if (mode === 'search' && (params.get('q') ?? '').trim().length < 2) {
    const rest = new URLSearchParams(params)
    rest.delete('q')
    const search = rest.toString()
    return <Navigate replace to={'/catalog' + (search ? '?' + search : '')} />
  }
  return <SearchResults mode={mode} />
}
