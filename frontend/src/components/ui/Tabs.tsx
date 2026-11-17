import { useRef, type KeyboardEvent } from 'react'
import { chipClasses } from './Chip'

export interface TabItem {
  id: string
  label: string
}

interface TabsProps {
  label: string
  items: TabItem[]
  value: string
  onChange: (id: string) => void
  panelId: string
  idPrefix: string
}

export function Tabs({ label, items, value, onChange, panelId, idPrefix }: TabsProps) {
  const refs = useRef<Record<string, HTMLButtonElement | null>>({})

  const select = (index: number) => {
    const item = items[(index + items.length) % items.length]
    if (!item) return
    refs.current[item.id]?.focus()
    onChange(item.id)
  }

  const onKeyDown = (event: KeyboardEvent, index: number) => {
    if (event.key === 'ArrowRight') select(index + 1)
    else if (event.key === 'ArrowLeft') select(index - 1)
    else if (event.key === 'Home') select(0)
    else if (event.key === 'End') select(items.length - 1)
    else return
    event.preventDefault()
  }

  return (
    <div role="tablist" aria-label={label} className="flex gap-2 overflow-x-auto">
      {items.map((item, index) => {
        const selected = item.id === value
        return (
          <button
            key={item.id}
            ref={(el) => {
              refs.current[item.id] = el
            }}
            type="button"
            role="tab"
            id={`${idPrefix}-${item.id}`}
            aria-selected={selected}
            aria-controls={panelId}
            tabIndex={selected ? 0 : -1}
            onClick={() => onChange(item.id)}
            onKeyDown={(event) => onKeyDown(event, index)}
            className={chipClasses({
              selected,
              className: selected ? 'shrink-0 whitespace-nowrap font-semibold' : 'shrink-0 whitespace-nowrap',
            })}
          >
            {item.label}
          </button>
        )
      })}
    </div>
  )
}
