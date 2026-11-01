import { toFacetParams, type SearchFilters, type SearchMode } from './useSearchFilters'
import { FiltersForm } from './FiltersForm'
import { useFacets } from './useFacets'
import { useStores } from './useStores'

interface FiltersPanelProps {
  mode: SearchMode
  filters: SearchFilters
  onChange: (patch: Partial<SearchFilters>) => void
}

export function FiltersPanel({ mode, filters, onChange }: FiltersPanelProps) {
  const { enabledStores } = useStores()
  const facets = useFacets(toFacetParams(filters, mode))
  return (
    <aside
      aria-label="Фильтры"
      className="hidden max-w-[280px] grow basis-60 rounded-card border border-line bg-surface p-5 md:flex md:flex-col"
    >
      <FiltersForm layout="list" value={filters} onChange={onChange} facets={facets.data} stores={enabledStores} />
    </aside>
  )
}
