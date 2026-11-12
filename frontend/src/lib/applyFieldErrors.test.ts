import { act, renderHook } from '@testing-library/react'
import { useForm } from 'react-hook-form'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/errors'
import { applyFieldErrors } from './applyFieldErrors'

type Values = { email: string; password: string }

function setup() {
  return renderHook(() => useForm<Values>({ defaultValues: { email: '', password: '' } })).result
}

describe('applyFieldErrors', () => {
  it('sets server errors only on the listed fields present in the response', () => {
    const form = setup()
    const error = new ApiError(422, 'VALIDATION_FAILED', 'x', [
      { field: 'email', message: 'Занят' },
      { field: 'other', message: 'ignored' },
    ])
    let applied = false
    act(() => {
      applied = applyFieldErrors(error, form.current.setError, ['email', 'password'])
    })
    expect(applied).toBe(true)
    expect(form.current.getFieldState('email').error).toMatchObject({ type: 'server', message: 'Занят' })
    expect(form.current.getFieldState('password').error).toBeUndefined()
  })

  it('returns false when no listed field matches', () => {
    const form = setup()
    const error = new ApiError(422, 'VALIDATION_FAILED', 'x', [{ field: 'other', message: 'm' }])
    expect(applyFieldErrors(error, form.current.setError, ['email'])).toBe(false)
  })

  it('returns false for other errors', () => {
    const form = setup()
    expect(applyFieldErrors(new ApiError(401, 'UNAUTHORIZED', 'x'), form.current.setError, ['email'])).toBe(false)
    expect(applyFieldErrors(new Error('boom'), form.current.setError, ['email'])).toBe(false)
  })
})
