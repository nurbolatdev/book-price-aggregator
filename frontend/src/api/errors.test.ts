import { AxiosError, AxiosHeaders } from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { ApiError, getErrorMessage, toApiError } from './errors'

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig

function withResponse(status: number, data: unknown): AxiosError {
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    data,
    status,
    statusText: '',
    headers: {},
    config,
  })
}

describe('toApiError', () => {
  it('keeps status, code, message and field errors from the body', () => {
    const error = toApiError(
      withResponse(422, {
        status: 422,
        code: 'VALIDATION_FAILED',
        message: 'Invalid',
        fieldErrors: [{ field: 'email', message: 'bad' }],
      }),
    )
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(422)
    expect(error.code).toBe('VALIDATION_FAILED')
    expect(error.message).toBe('Invalid')
    expect(error.fieldErrors).toEqual([{ field: 'email', message: 'bad' }])
  })

  it('maps a missing response to a network error', () => {
    const error = toApiError(new AxiosError('Network Error', 'ERR_NETWORK', config))
    expect(error.status).toBe(0)
    expect(error.code).toBe('NETWORK_ERROR')
  })

  it('maps a non-JSON body to UNKNOWN', () => {
    const error = toApiError(withResponse(502, '<html>Bad gateway</html>'))
    expect(error.status).toBe(502)
    expect(error.code).toBe('UNKNOWN')
  })
})

describe('getErrorMessage', () => {
  it('returns the dictionary text for a known code', () => {
    expect(getErrorMessage(new ApiError(401, 'INVALID_CREDENTIALS', 'x'))).toBe(
      'Неверный email или пароль',
    )
  })

  it('returns the generic text for a plain error', () => {
    expect(getErrorMessage(new Error('boom'))).toBe('Что-то пошло не так')
  })
})
