import { beforeEach, describe, expect, it } from 'vitest'
import { MOCK_ACCESS_TOKEN } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import {
  fetchAdminActivity,
  fetchAdminEvents,
  fetchAdminHealth,
  fetchAdminNotifications,
  fetchAdminParsers,
  fetchAdminQueues,
  fetchAdminStats,
  fetchAdminUsers,
  fetchPendingOffers,
  fetchTopQueries,
  fetchZeroResults,
  matchOffer,
  rejectOffer,
  requeueDeadLetter,
  retryNotification,
  runParser,
  setStoreEnabled,
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

  it('switches a store and returns its state', async () => {
    expect(await setStoreEnabled('FLIP', false)).toEqual({
      code: 'FLIP',
      enabled: false,
    })
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

  it('requeues the dead-letter queue', async () => {
    expect(await requeueDeadLetter()).toEqual({ requeued: 6, skipped: 1 })
  })

  it('serialises user filters, omitting empty ones', async () => {
    const urls = captureUrls('/admin/users')
    const all = await fetchAdminUsers({ q: '', page: 0, size: 20 })
    expect(all.totalElements).toBe(45)
    expect(urls[0]?.searchParams.has('q')).toBe(false)
    expect(urls[0]?.searchParams.has('verified')).toBe(false)
    const filtered = await fetchAdminUsers({
      q: 'aigerim',
      verified: false,
      page: 1,
      size: 20,
    })
    expect(urls[1]?.searchParams.get('q')).toBe('aigerim')
    expect(urls[1]?.searchParams.get('verified')).toBe('false')
    expect(urls[1]?.searchParams.get('page')).toBe('1')
    expect(filtered.content).toEqual([])
    server.events.removeAllListeners()
  })

  it('lists notifications with filters and retries a failed one', async () => {
    const urls = captureUrls('/admin/notifications')
    const queued = await fetchAdminNotifications({
      status: 'QUEUED',
      page: 0,
      size: 1,
    })
    expect(queued.totalElements).toBe(3)
    expect(urls[0]?.searchParams.get('status')).toBe('QUEUED')
    expect(urls[0]?.searchParams.has('delivery')).toBe(false)
    const retried = await retryNotification(103)
    expect(retried.status).toBe('QUEUED')
    await expect(retryNotification(101)).rejects.toMatchObject({ status: 409 })
    server.events.removeAllListeners()
  })

  it('pages pending offers and removes a matched one', async () => {
    const first = await fetchPendingOffers(0, 20)
    expect(first.content).toHaveLength(20)
    expect(first.totalElements).toBe(23)
    const second = await fetchPendingOffers(1, 20)
    expect(second.content).toHaveLength(3)
    const matched = await matchOffer(501, 16)
    expect(matched.matchStatus).toBe('MATCHED')
    const rejected = await rejectOffer(502)
    expect(rejected.matchStatus).toBe('REJECTED')
    expect((await fetchPendingOffers(0, 20)).totalElements).toBe(21)
    await expect(matchOffer(501, 16)).rejects.toMatchObject({ status: 404 })
  })

  it('returns zero-result queries for the period', async () => {
    const urls = captureUrls('/admin/search/zero-results')
    expect(await fetchZeroResults('24h')).toHaveLength(2)
    expect(urls[0]?.searchParams.get('period')).toBe('24h')
    expect((await fetchZeroResults('7d'))[0]?.query).toBe('гарри поттер и дары смерти')
    server.events.removeAllListeners()
  })

  it('drops admin data on sign-out', () => {
    expect(USER_SCOPED_KEYS).toContainEqual(['admin'])
  })
})
