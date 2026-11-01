import { ListFilter } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Button } from '../../components/ui/Button'
import { Sheet } from '../../components/ui/Sheet'
import { pluralRu } from '../../lib/format'
import { useMediaQuery } from '../../lib/useMediaQuery'
import { FiltersForm } from './FiltersForm'
import { useFacets } from './useFacets'
import { useSearchResults } from './useSearchResults'
import {
  countActiveFilters,
  parseFilters,
  toApiParams,
  toFacetParams,
  type SearchFilters,
  type SearchMode,
} from './useSearchFilters'
import { useStores } from './useStores'

const EDITIONS: [string, string, string] = ['издание', 'издания', 'изданий']

interface SearchFiltersSheetProps {
  mode: SearchMode
  filters: SearchFilters
  onApply: (filters: SearchFilters) => void
}

interface BodyProps extends SearchFiltersSheetProps {
  onClose: () => void
}

function FiltersSheetBody({ mode, filters, onApply, onClose }: BodyProps) {
  const [draft, setDraft] = useState<SearchFilters>(filters)
  const { enabledStores } = useStores()
  const facets = useFacets(toFacetParams(draft, mode))
  const results = useSearchResults(toApiParams(draft, mode))
  const total = results.data?.pages[0]?.totalElements

  function reset() {
    setDraft({ ...parseFilters(new URLSearchParams(), mode), q: draft.q, sort: draft.sort })
  }

  return (
    <>
      <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-4">
        <FiltersForm
          layout="chips"
          idPrefix="sheet-filters"
          value={draft}
          onChange={(patch) => setDraft((d) => ({ ...d, ...patch }))}
          facets={facets.data}
          stores={enabledStores}
        />
      </div>
      <div className="flex gap-3 border-t border-divider bg-surface p-4">
        <Button variant="secondary" size="lg" className="flex-1" onClick={reset}>
          Сбросить
        </Button>
        <Button
          variant="dark"
          size="lg"
          className="flex-[2]"
          onClick={() => {
            onApply(draft)
            onClose()
          }}
        >
          {total === undefined ? 'Показать издания' : `Показать ${total} ${pluralRu(total, EDITIONS)}`}
        </Button>
      </div>
    </>
  )
}

export function SearchFiltersSheet({ mode, filters, onApply }: SearchFiltersSheetProps) {
  const [open, setOpen] = useState(false)
  const wide = useMediaQuery('(min-width: 768px)')
  const active = countActiveFilters(filters)

  useEffect(() => {
    if (wide) setOpen(false)
  }, [wide])

  return (
    <>
      <Button variant="secondary" size="sm" className="md:hidden" onClick={() => setOpen(true)}>
        <ListFilter aria-hidden="true" className="h-4 w-4" />
        {active > 0 ? `Фильтры · ${active}` : 'Фильтры'}
      </Button>
      <Sheet open={open} onClose={() => setOpen(false)} title="Фильтры" side="bottom">
        <FiltersSheetBody
          mode={mode}
          filters={filters}
          onApply={onApply}
          onClose={() => setOpen(false)}
        />
      </Sheet>
    </>
  )
}
