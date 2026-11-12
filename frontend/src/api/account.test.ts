import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { MOCK_ACCESS_TOKEN, MOCK_PASSWORD, mockUser } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { forgotPassword, login, logoutAll, register, resendVerification, resetPassword, verifyEmail } from './auth'
import { ApiError } from './errors'
import { changePassword, deleteAccount } from './me'

interface Logged {
  method: string
  path: string
  body: unknown
}

let log: Logged[] = []

beforeEach(() => {
  log = []
  server.events.on('request:start', async ({ request }) => {
    const text = await request.clone().text()
    log.push({
      method: request.method,
      path: new URL(request.url).pathname,
      body: text ? JSON.parse(text) : undefined,
    })
  })
})

afterEach(() => {
  server.events.removeAllListeners()
})

function signIn() {
  useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user: mockUser })
}

async function rejection(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise
  } catch (e) {
    return e as ApiError
  }
  throw new Error('expected a rejection')
}

describe('account api', () => {
  it('register posts name, email and password', async () => {
    const result = await register({ name: 'Айгерим', email: 'new@example.com', password: 'secret123' })
    expect(result).toEqual({ userId: 7, emailVerificationRequired: true })
    expect(log[0]).toEqual({
      method: 'POST',
      path: '/api/v1/auth/register',
      body: { name: 'Айгерим', email: 'new@example.com', password: 'secret123' },
    })
  })

  it('register rejects a taken email', async () => {
    const error = await rejection(register({ email: 'taken@example.com', password: 'secret123' }))
    expect(error.code).toBe('EMAIL_TAKEN')
  })

  it('resend and forgot post the email', async () => {
    await resendVerification('a@example.com')
    await forgotPassword('b@example.com')
    expect(log.map((l) => [l.path, l.body])).toEqual([
      ['/api/v1/auth/resend-verification', { email: 'a@example.com' }],
      ['/api/v1/auth/forgot-password', { email: 'b@example.com' }],
    ])
  })

  it('verifyEmail maps the token to the outcome', async () => {
    await expect(verifyEmail('valid')).resolves.toBeUndefined()
    expect((await rejection(verifyEmail('expired'))).code).toBe('TOKEN_EXPIRED')
    expect((await rejection(verifyEmail('x'))).code).toBe('TOKEN_INVALID')
  })

  it('resetPassword posts the token and the new password', async () => {
    await resetPassword('valid', 'newpass123')
    expect(log[0]?.body).toEqual({ token: 'valid', newPassword: 'newpass123' })
  })

  it('changePassword sends exactly the two passwords', async () => {
    signIn()
    await changePassword(MOCK_PASSWORD, 'newpass123')
    expect(log[0]?.body).toEqual({ currentPassword: MOCK_PASSWORD, newPassword: 'newpass123' })
  })

  it('changePassword with a wrong current password gives a field error', async () => {
    signIn()
    const error = await rejection(changePassword('nope', 'newpass123'))
    expect(error.status).toBe(422)
    expect(error.fieldErrors[0]?.field).toBe('currentPassword')
  })

  it('deleteAccount sends DELETE /me with the password and clears the session', async () => {
    signIn()
    await deleteAccount(MOCK_PASSWORD)
    expect(log[0]).toEqual({ method: 'DELETE', path: '/api/v1/me', body: { password: MOCK_PASSWORD } })
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('deleteAccount keeps the session on a wrong password', async () => {
    signIn()
    const error = await rejection(deleteAccount('nope'))
    expect(error.fieldErrors[0]?.field).toBe('password')
    expect(useAuthStore.getState().status).toBe('authenticated')
  })

  it('logoutAll clears the session', async () => {
    signIn()
    await logoutAll()
    expect(log[0]?.path).toBe('/api/v1/auth/logout-all')
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('login for the limited address is rate limited with Retry-After', async () => {
    const error = await rejection(login('limit@example.com', 'whatever'))
    expect(error.code).toBe('RATE_LIMITED')
    expect(error.retryAfter).toBe(45)
  })
})
