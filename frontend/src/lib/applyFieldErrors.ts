import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '../api/errors'

/** Puts API field errors on the listed form fields; returns false when nothing was applied. */
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): boolean {
  if (!(error instanceof ApiError) || error.code !== 'VALIDATION_FAILED') return false
  let applied = false
  for (const field of fields) {
    const match = error.fieldErrors.find((item) => item.field === field)
    if (!match) continue
    setError(field, { type: 'server', message: match.message }, { shouldFocus: !applied })
    applied = true
  }
  return applied
}
