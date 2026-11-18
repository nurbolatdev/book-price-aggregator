import { beforeEach, describe, expect, it } from 'vitest'
import { MOCK_ACCESS_TOKEN } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import {
  fetchAdminActivity,
  fetchAdminEvents,
  fetchAdminHealth,
  fetchAdminParsers,
  fetchAdminQueues,
  fetchAdminStats,
  fetchTopQueries,
  runParser,
} from './admin'
import { ApiError, toApiError } from './errors'
import { USER_SCOPED_KEYS } from './queryKeys'

beforeEach(() => {
  useAuthStore.setState({
    status: 'anonymous',
    user: null,
    accessToken: MOCK_ACCESS_TOKEN,
  })
})

function captureUrls(path: string): URL[] {
  const urls: URL[] = []
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.endsWith(path)) urls.push(url)
  })
  return urls
}

describe('admin monitoring api', () => {
  it('sends the period and returns the period stats', async () => {
    const urls = captureUrls('/admin/stats')
    const stats = await fetchAdminStats('7d')
    expect(urls[0]?.searchParams.get('period')).toBe('7d')
    expect(stats.users?.newInPeriod).toBe(14)
    server.events.removeAllListeners()
  })

  it('returns the health components', async () => {
    const health = await fetchAdminHealth()
    expect(health.components?.db?.status).toBe('UP')
  })

  it('lists five parsers in mockup order', async () => {
    const parsers = await fetchAdminParsers()
    expect(parsers.map((p) => p.code)).toEqual(['FLIP', 'MELOMAN', 'OZON', 'KASPI', 'WILDBERRIES'])
  })

  it('queues a parser run and rejects a disabled store with 409', async () => {
    expect(await runParser('FLIP')).toEqual({ queued: true })
    const failure = await runParser('KASPI').catch(toApiError)
    expect(failure).toBeInstanceOf(ApiError)
    expect(failure).toMatchObject({ status: 409, code: 'CONFLICT' })
  })

  it('lists the queues including the dead letter', async () => {
    const queues = await fetchAdminQueues()
    expect(queues.find((q) => q.name === 'kr.dead-letter')?.ready).toBe(7)
  })

  it('returns the requested number of activity days', async () => {
    expect(await fetchAdminActivity(14)).toHaveLength(14)
  })

  it('pages events and omits the level for all', async () => {
    const urls = captureUrls('/admin/events')
    const all = await fetchAdminEvents({ page: 0, size: 50 })
    expect(urls[0]?.searchParams.has('level')).toBe(false)
    expect(all.content).toHaveLength(50)
    expect(all.totalElements).toBe(120)
    const errors = await fetchAdminEvents({
      level: 'ERROR',
      page: 0,
      size: 50,
    })
    expect(urls[1]?.searchParams.get('level')).toBe('ERROR')
    expect(errors.content?.length).toBeGreaterThan(0)
    expect(errors.content?.every((e) => e.level === 'ERROR')).toBe(true)
    server.events.removeAllListeners()
  })

  it('returns the top queries for the period', async () => {
    const top = await fetchTopQueries('24h')
    expect(top[0]?.query).toBe('Мастер и Маргарита')
  })

  it('drops admin data on sign-out', () => {
    expect(USER_SCOPED_KEYS).toContainEqual(['admin'])
  })
})
