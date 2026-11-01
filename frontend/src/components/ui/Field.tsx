import { useId, type ReactNode } from 'react'

export interface FieldControlProps {
  id: string
  describedBy: string | undefined
  invalid: boolean
}

interface FieldProps {
  label: string
  hint?: string
  error?: string
  children: (props: FieldControlProps) => ReactNode
}

export function Field({ label, hint, error, children }: FieldProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const ids = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ')
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold text-ink">
        {label}
      </label>
      {children({ id, describedBy: ids || undefined, invalid: Boolean(error) })}
      {hint && (
        <p id={hintId} className="text-[13px] text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-[13px] text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
