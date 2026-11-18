import type { SearchItem } from '../../../api/types'
import { focusRing } from '../../../components/ui/focusRing'
import { cn } from '../../../lib/cn'
import { bindingName, pluralRu } from '../../../lib/format'

interface EditionVariantsProps {
  items: SearchItem[]
  value: number | null
  onChange: (editionId: number) => void
}

function details(item: SearchItem): string {
  const parts: string[] = []
  const head = [item.publisher, item.year].filter(Boolean).join(' ')
  if (head) parts.push(head)
  if (item.binding && item.binding !== 'UNKNOWN') parts.push(bindingName(item.binding))
  if (item.storesCount !== undefined) {
    parts.push(
      `${item.storesCount} ${pluralRu(item.storesCount, ['предложение', 'предложения', 'предложений'])}`,
    )
  }
  return parts.join(' · ')
}

export function EditionVariants({ items, value, onChange }: EditionVariantsProps) {
  return (
    <fieldset className="flex flex-col gap-2.5">
      <legend className="mb-2.5 font-bold">Привязать к изданию</legend>
      {items.length === 0 ? (
        <p className="text-sm text-muted">Изданий не нашли — уточните запрос</p>
      ) : (
        items.map((item) => {
          const selected = item.editionId === value
          return (
            <label
              key={item.editionId}
              className={cn(
                'flex cursor-pointer items-center gap-3 rounded-xl p-3',
                selected ? 'border-2 border-accent' : 'border border-line',
              )}
            >
              <input
                type="radio"
                name="edition"
                checked={selected}
                onChange={() => item.editionId !== undefined && onChange(item.editionId)}
                className={cn('h-5 w-5 shrink-0 accent-ink', focusRing)}
              />
              <span className="flex flex-col">
                <strong>{item.title}</strong>
                <span className="text-[13px] text-muted">{details(item)}</span>
              </span>
            </label>
          )
        })
      )}
    </fieldset>
  )
}
