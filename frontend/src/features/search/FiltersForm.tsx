import type { ReactNode } from 'react'
import type { SearchFacets, StoreResponse } from '../../api/types'
import { Checkbox } from '../../components/ui/Checkbox'
import { cn } from '../../lib/cn'
import { bindingName, languageName } from '../../lib/format'
import { focusRing } from '../../components/ui/focusRing'
import { PriceRange } from './PriceRange'
import { BINDING_ORDER, LANGUAGE_ORDER, type SearchFilters } from './useSearchFilters'

interface FiltersFormProps {
  layout: 'list' | 'chips'
  value: SearchFilters
  onChange: (patch: Partial<SearchFilters>) => void
  facets?: SearchFacets
  stores: StoreResponse[]
  idPrefix?: string
}

interface Option {
  key: string
  label: string
  count: number
  checked: boolean
  disabled: boolean
  onToggle: () => void
}

function countOf(list: { code?: string; value?: string; count?: number }[] | undefined, key: string): number {
  return list?.find((f) => (f.code ?? f.value) === key)?.count ?? 0
}

function toggle(list: string[], key: string): string[] {
  return list.includes(key) ? list.filter((v) => v !== key) : [...list, key]
}

function Group({ legend, children }: { legend: string; children: ReactNode }) {
  return (
    <fieldset className="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
      <legend className="mb-1 p-0 text-[15px] font-bold text-ink">{legend}</legend>
      {children}
    </fieldset>
  )
}

function Options({ options, layout }: { options: Option[]; layout: 'list' | 'chips' }) {
  if (layout === 'list') {
    return (
      <>
        {options.map((o) => (
          <Checkbox
            key={o.key}
            label={o.label}
            count={o.count}
            checked={o.checked}
            disabled={o.disabled}
            onChange={o.onToggle}
          />
        ))}
      </>
    )
  }
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((o) => (
        <label
          key={o.key}
          className={cn(
            'flex min-h-touch cursor-pointer items-center gap-2 rounded-xl border px-3 text-[15px]',
            o.checked ? 'border-ink bg-canvas' : 'border-line bg-surface',
            o.disabled && 'cursor-not-allowed opacity-50',
          )}
        >
          <input
            type="checkbox"
            className={cn('h-4 w-4 accent-ink', focusRing)}
            checked={o.checked}
            disabled={o.disabled}
            onChange={o.onToggle}
          />
          <span>{`${o.label} · ${o.count}`}</span>
        </label>
      ))}
    </div>
  )
}

export function FiltersForm({ layout, value, onChange, facets, stores, idPrefix = 'filters' }: FiltersFormProps) {
  const allCodes = stores.map((s) => s.code ?? '')
  const selected = value.stores.length === 0 ? allCodes : value.stores

  const storeOptions: Option[] = stores.map((s) => {
    const code = s.code ?? ''
    const checked = selected.includes(code)
    const count = countOf(facets?.stores, code)
    const isLast = checked && selected.length === 1
    return {
      key: code,
      label: s.name ?? code,
      count,
      checked,
      disabled: isLast || (count === 0 && !checked),
      onToggle: () => {
        const next = checked ? selected.filter((c) => c !== code) : [...selected, code]
        onChange({ stores: next.length === allCodes.length ? [] : next })
      },
    }
  })

  const valueOptions = (
    order: readonly string[],
    list: { value?: string; count?: number }[] | undefined,
    current: string[],
    field: 'binding' | 'language',
    name: (v: string) => string,
  ): Option[] =>
    order.map((v) => {
      const count = countOf(list, v)
      const checked = current.includes(v)
      return {
        key: v,
        label: name(v),
        count,
        checked,
        disabled: count === 0 && !checked,
        onToggle: () => onChange({ [field]: toggle(current, v) }),
      }
    })

  return (
    <div className="flex flex-col gap-[22px]">
      <Group legend="Магазины">
        <Options options={storeOptions} layout={layout} />
      </Group>
      <Group legend="Цена, ₸">
        <PriceRange
          idPrefix={`${idPrefix}-price`}
          minPrice={value.minPrice}
          maxPrice={value.maxPrice}
          onCommit={onChange}
        />
      </Group>
      <Checkbox
        label="Только в наличии"
        className="font-bold"
        checked={value.inStock}
        onChange={(e) => onChange({ inStock: e.target.checked })}
      />
      <Group legend="Переплёт">
        <Options
          layout={layout}
          options={valueOptions(BINDING_ORDER, facets?.bindings, value.binding, 'binding', bindingName)}
        />
      </Group>
      <Group legend="Язык">
        <Options
          layout={layout}
          options={valueOptions(LANGUAGE_ORDER, facets?.languages, value.language, 'language', languageName)}
        />
      </Group>
    </div>
  )
}
