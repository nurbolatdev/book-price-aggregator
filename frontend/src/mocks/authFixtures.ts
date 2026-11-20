import type { UserResponse } from '../api/types'

export const MOCK_PASSWORD = 'password123'
export const MOCK_ACCESS_TOKEN = 'mock-access'

const SESSION_KEY = 'kr.mock.session'

function verifiedFlag(): boolean {
  try {
    return sessionStorage.getItem('kr.mock.emailVerified') === '1'
  } catch {
    return false
  }
}

function adminFlag(): boolean {
  try {
    return sessionStorage.getItem('kr.mock.role') === 'ADMIN'
  } catch {
    return false
  }
}

const initialUser: UserResponse = {
  id: 7,
  email: 'nurbo@example.com',
  name: 'Нурболат',
  role: adminFlag() ? 'ADMIN' : 'USER',
  emailVerified: verifiedFlag(),
  notifyFrequency: 'INSTANT',
  weeklyDigest: true,
}

export let mockUser: UserResponse = { ...initialUser }

export function patchMockUser(patch: Partial<UserResponse>): UserResponse {
  mockUser = { ...mockUser, ...patch }
  return mockUser
}

export function isMockSignedIn(): boolean {
  try {
    return sessionStorage.getItem(SESSION_KEY) === '1'
  } catch {
    return false
  }
}

export function setMockSignedIn(signedIn: boolean): void {
  try {
    if (signedIn) sessionStorage.setItem(SESSION_KEY, '1')
    else sessionStorage.removeItem(SESSION_KEY)
  } catch {
    // storage unavailable: the mock session just does not persist
  }
}

export function resetMockAuth(): void {
  mockUser = { ...initialUser }
  setMockSignedIn(false)
}
