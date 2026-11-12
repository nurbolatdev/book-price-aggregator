import { isAxiosError } from 'axios'
import type { ErrorCode, ErrorResponse, FieldError } from './types'

export type ApiErrorCode = ErrorCode | 'NETWORK_ERROR' | 'UNKNOWN'

export class ApiError extends Error {
  readonly status: number
  readonly code: ApiErrorCode
  readonly fieldErrors: FieldError[]
  readonly retryAfter: number | null

  constructor(
    status: number,
    code: ApiErrorCode,
    message: string,
    fieldErrors: FieldError[] = [],
    retryAfter: number | null = null,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.retryAfter = retryAfter
  }
}

export const ERROR_MESSAGES: Record<ApiErrorCode, string> = {
  VALIDATION_FAILED: 'Проверьте введённые данные',
  NOT_FOUND: 'Ничего не найдено',
  UNAUTHORIZED: 'Войдите, чтобы продолжить',
  FORBIDDEN: 'Недостаточно прав',
  EMAIL_NOT_VERIFIED: 'Подтвердите email — ссылка в письме',
  EMAIL_TAKEN: 'Этот email уже зарегистрирован',
  INVALID_CREDENTIALS: 'Неверный email или пароль',
  TOKEN_EXPIRED: 'Сессия истекла, войдите снова',
  TOKEN_INVALID: 'Ссылка недействительна или уже использована',
  RATE_LIMITED: 'Слишком много запросов, попробуйте через минуту',
  CONFLICT: 'Данные изменились, обновите страницу',
  SERVICE_UNAVAILABLE: 'Сервис временно недоступен, попробуйте позже',
  INTERNAL_ERROR: 'Что-то пошло не так',
  NETWORK_ERROR: 'Нет соединения с сервером',
  UNKNOWN: 'Что-то пошло не так',
}

function isErrorBody(data: unknown): data is ErrorResponse & { code: ErrorCode } {
  return (
    typeof data === 'object' &&
    data !== null &&
    typeof (data as { code?: unknown }).code === 'string' &&
    (data as { code: string }).code in ERROR_MESSAGES
  )
}

function parseRetryAfter(value: unknown): number | null {
  if (typeof value !== 'string' || !/^\d+$/.test(value.trim())) return null
  const seconds = Number(value.trim())
  return seconds >= 1 ? seconds : null
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (isAxiosError(error)) {
    const response = error.response
    if (!response) return new ApiError(0, 'NETWORK_ERROR', ERROR_MESSAGES.NETWORK_ERROR)
    const data: unknown = response.data
    const retryAfter = parseRetryAfter(response.headers?.['retry-after'])
    if (isErrorBody(data)) {
      return new ApiError(
        response.status,
        data.code,
        data.message ?? ERROR_MESSAGES[data.code],
        data.fieldErrors ?? [],
        retryAfter,
      )
    }
    return new ApiError(response.status, 'UNKNOWN', ERROR_MESSAGES.UNKNOWN, [], retryAfter)
  }
  return new ApiError(0, 'UNKNOWN', ERROR_MESSAGES.UNKNOWN)
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) return ERROR_MESSAGES[error.code]
  return ERROR_MESSAGES.UNKNOWN
}

export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404
}
