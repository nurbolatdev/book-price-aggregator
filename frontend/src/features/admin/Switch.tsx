import { cn } from '../../lib/cn'
import { focusRing } from '../../components/ui/focusRing'

interface SwitchProps {
  checked: boolean
  label: string
  disabled?: boolean
  onChange: (checked: boolean) => void
}

export const Switch = ({ checked, label, disabled, onChange }: SwitchProps) => (
  <button
    type="button"
    role="switch"
    aria-checked={checked}
    aria-label={label}
    disabled={disabled}
    onClick={() => onChange(!checked)}
    className={cn(
      'flex h-[26px] w-11 shrink-0 rounded-full p-[3px] disabled:cursor-not-allowed disabled:opacity-60',
      checked ? 'justify-end bg-deal' : 'justify-start bg-dot-off',
      focusRing,
    )}
  >
    <span aria-hidden="true" className="h-5 w-5 rounded-full bg-surface" />
  </button>
)
