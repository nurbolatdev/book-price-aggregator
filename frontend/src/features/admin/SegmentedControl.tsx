import { cn } from '../../lib/cn'
import { focusRing } from '../../components/ui/focusRing'

interface SegmentedControlProps<T extends string> {
  label: string
  options: { value: T; label: string }[]
  value: T
  onChange: (value: T) => void
}

export function SegmentedControl<T extends string>({ label, options, value, onChange }: SegmentedControlProps<T>) {
  return (
    <div role="group" aria-label={label} className="flex gap-1 rounded-control border border-line bg-surface p-1">
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          aria-pressed={o.value === value}
          onClick={() => onChange(o.value)}
          className={cn(
            'rounded-lg px-3 py-1.5 text-sm',
            o.value === value ? 'bg-ink text-white' : 'text-ink hover:bg-canvas',
            focusRing,
          )}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}
