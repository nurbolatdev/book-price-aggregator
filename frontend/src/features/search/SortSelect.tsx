import type { SortKey } from '../../api/search'
import { Select } from '../../components/ui/Select'
import type { SearchMode } from './useSearchFilters'

const OPTIONS: { value: SortKey; label: string }[] = [
  { value: 'PRICE_ASC', label: 'Сначала дешевле' },
  { value: 'DISCOUNT_DESC', label: 'Максимальная скидка' },
  { value: 'RELEVANCE', label: 'По релевантности' },
  { value: 'STORES_DESC', label: 'Больше магазинов' },
]

interface SortSelectProps {
  mode: SearchMode
  value: SortKey
  onChange: (sort: SortKey) => void
}

export function SortSelect({ mode, value, onChange }: SortSelectProps) {
  return (
    <label className="flex items-center gap-2 text-sm text-muted">
      <span>Сортировка</span>
      <Select value={value} onChange={(e) => onChange(e.target.value as SortKey)} className="text-ink">
        {OPTIONS.filter((o) => mode === 'search' || o.value !== 'RELEVANCE').map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </Select>
    </label>
  )
}
