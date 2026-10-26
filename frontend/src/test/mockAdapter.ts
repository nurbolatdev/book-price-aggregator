import { AxiosError, AxiosHeaders } from 'axios'
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios'
import { api } from '../api/client'
import type { AuthResponse } from '../api/types'

export type MockReply = [status: number, body?: unknown]
export const NETWORK_FAILURE = Symbol('network-failure')

export type MockHandler = (
  config: InternalAxiosRequestConfig,
) => MockReply | typeof NETWORK_FAILURE | Promise<MockReply | typeof NETWORK_FAILURE>

export function mockApi(handler: MockHandler): void {
  const adapter: AxiosAdapter = async (config) => {
    const reply = await handler(config)
    if (reply === NETWORK_FAILURE) {
      throw new AxiosError('Network Error', 'ERR_NETWORK', config)
    }
    const [status, body] = reply
    const response = {
      data: body,
      status,
      statusText: String(status),
      headers: new AxiosHeaders(),
      config,
    }
    if (status >= 200 && status < 300) return response
    throw new AxiosError(
      `Request failed with status code ${status}`,
      'ERR_BAD_REQUEST',
      config,
      null,
      response,
    )
  }
  api.defaults.adapter = adapter
}

export function authBody(accessToken: string, refreshToken: string): AuthResponse {
  return {
    accessToken,
    refreshToken,
    expiresIn: 900,
    user: { id: 1, email: 'reader@example.com', name: 'Reader', role: 'USER' },
  }
}
