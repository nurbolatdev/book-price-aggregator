import type { ReactNode, Ref } from 'react'
import type { UseFormRegisterReturn } from 'react-hook-form'
import { focusRing } from '../../components/ui/focusRing'
import { cn } from '../../lib/cn'

interface ConditionOptionProps {
  value: string
  label: string
  selected: boolean
  invalid: boolean
  register: UseFormRegisterReturn
  /** Extra ref to the radio, used to focus the option that is checked when the dialog opens. */
  inputRef?: Ref<HTMLInputElement>
  /** Field shown after the label; rendered only for the selected option. */
  children?: ReactNode
}

function assign<T>(ref: Ref<T> | undefined, value: T | null) {
  if (typeof ref === 'function') ref(value)
  else if (ref) (ref as { current: T | null }).current = value
}

export function ConditionOption({ value, label, selected, invalid, register, inputRef, children }: ConditionOptionProps) {
  return (
    <div
      className={cn(
        'flex flex-wrap items-center gap-2.5 rounded-xl border bg-surface',
        !selected && 'border-line p-3',
        selected && !invalid && 'border-2 border-accent p-[11px]',
        selected && invalid && 'border-2 border-danger p-[11px]',
      )}
    >
      <label className="flex min-h-touch flex-1 items-center gap-2.5">
        <input
          type="radio"
          value={value}
          className={cn('h-5 w-5 shrink-0 accent-ink', focusRing)}
          {...register}
          ref={(node) => {
            register.ref(node)
            assign(inputRef, node)
          }}
        />
        <span className="text-[15px] text-ink">{label}</span>
      </label>
      {children}
    </div>
  )
}
