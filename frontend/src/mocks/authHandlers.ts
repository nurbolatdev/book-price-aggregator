import { HttpResponse, http } from 'msw'
import {
  MOCK_ACCESS_TOKEN,
  MOCK_PASSWORD,
  isMockSignedIn,
  mockUser,
  patchMockUser,
  setMockSignedIn,
} from './authFixtures'

function error(status: number, code: string, message: string, extra: Record<string, unknown> = {}) {
  return HttpResponse.json({ code, message, ...extra }, { status })
}

function session() {
  return HttpResponse.json({ accessToken: MOCK_ACCESS_TOKEN, expiresIn: 900, user: mockUser })
}

async function jsonBody(request: Request): Promise<Record<string, unknown>> {
  try {
    return ((await request.json()) as Record<string, unknown>) ?? {}
  } catch {
    return {}
  }
}

function tokenReply(token: unknown, onValid: () => void) {
  if (token === 'valid') {
    onValid()
    return new HttpResponse(null, { status: 204 })
  }
  if (token === 'expired') return error(401, 'TOKEN_EXPIRED', 'Ссылка устарела')
  return error(400, 'TOKEN_INVALID', 'Ссылка недействительна')
}

function wrongPassword(field: string) {
  return error(422, 'VALIDATION_FAILED', 'Проверьте введённые данные', {
    fieldErrors: [{ field, message: 'Неверный пароль' }],
  })
}

export const authHandlers = [
  http.post('*/api/v1/auth/login', async ({ request }) => {
    const { email, password } = await jsonBody(request)
    if (email === 'limit@example.com') {
      return HttpResponse.json(
        { code: 'RATE_LIMITED', message: 'Слишком много попыток' },
        { status: 429, headers: { 'Retry-After': '45' } },
      )
    }
    if (password === MOCK_PASSWORD) {
      setMockSignedIn(true)
      return session()
    }
    return error(401, 'INVALID_CREDENTIALS', 'Неверный email или пароль')
  }),

  http.post('*/api/v1/auth/refresh', () =>
    isMockSignedIn() ? session() : error(401, 'UNAUTHORIZED', 'Войдите, чтобы продолжить'),
  ),

  http.post('*/api/v1/auth/logout', () => {
    setMockSignedIn(false)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('*/api/v1/auth/logout-all', ({ request }) => {
    if (request.headers.get('Authorization') !== `Bearer ${MOCK_ACCESS_TOKEN}`) {
      return error(401, 'UNAUTHORIZED', 'Войдите, чтобы продолжить')
    }
    setMockSignedIn(false)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('*/api/v1/auth/register', async ({ request }) => {
    const { email } = await jsonBody(request)
    if (email === 'taken@example.com') {
      return error(409, 'EMAIL_TAKEN', 'Этот email уже зарегистрирован')
    }
    return HttpResponse.json({ userId: 7, emailVerificationRequired: true }, { status: 201 })
  }),

  http.post('*/api/v1/auth/resend-verification', () => new HttpResponse(null, { status: 204 })),

  http.post('*/api/v1/auth/forgot-password', () => new HttpResponse(null, { status: 204 })),

  http.post('*/api/v1/auth/verify-email', async ({ request }) => {
    const { token } = await jsonBody(request)
    return tokenReply(token, () => patchMockUser({ emailVerified: true }))
  }),

  http.post('*/api/v1/auth/reset-password', async ({ request }) => {
    const { token } = await jsonBody(request)
    return tokenReply(token, () => setMockSignedIn(false))
  }),

  http.get('*/api/v1/me', ({ request }) =>
    request.headers.get('Authorization') === `Bearer ${MOCK_ACCESS_TOKEN}`
      ? HttpResponse.json(mockUser)
      : error(401, 'UNAUTHORIZED', 'Войдите, чтобы продолжить'),
  ),

  http.patch('*/api/v1/me', async ({ request }) => {
    const patch = await jsonBody(request)
    return HttpResponse.json(patchMockUser(patch))
  }),

  http.post('*/api/v1/me/password', async ({ request }) => {
    const { currentPassword } = await jsonBody(request)
    if (currentPassword === MOCK_PASSWORD) return new HttpResponse(null, { status: 204 })
    return wrongPassword('currentPassword')
  }),

  http.delete('*/api/v1/me', async ({ request }) => {
    const { password } = await jsonBody(request)
    if (password === MOCK_PASSWORD) {
      setMockSignedIn(false)
      return new HttpResponse(null, { status: 204 })
    }
    return wrongPassword('password')
  }),
]
