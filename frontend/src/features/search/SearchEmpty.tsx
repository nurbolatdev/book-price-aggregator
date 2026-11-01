import { SearchX } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { EmptyState } from '../../components/ui/EmptyState'
import { PopularChips } from './PopularChips'

interface SearchEmptyProps {
  query?: string
  activeFilters: number
  onReset: () => void
}

export function SearchEmpty({ query, activeFilters, onReset }: SearchEmptyProps) {
  const title = query ? `По запросу «${query}» ничего не нашли` : 'Ничего не нашли'
  return (
    <div className="flex flex-col items-center gap-6 rounded-card border border-line bg-surface px-4 py-8">
      <EmptyState
        icon={<SearchX className="h-8 w-8" />}
        title={title}
        className="py-4"
        action={
          <ul className="flex list-disc flex-col gap-1 pl-5 text-left text-[15px] text-muted">
            <li>Проверьте написание или поищите по автору</li>
            <li>Попробуйте ISBN — 10 или 13 цифр</li>
            {activeFilters > 0 && <li>Снимите фильтры</li>}
          </ul>
        }
      />
      {activeFilters > 0 && (
        <Button variant="secondary" size="sm" onClick={onReset}>
          Сбросить фильтры
        </Button>
      )}
      <PopularChips />
    </div>
  )
}
