import { HttpResponse, http } from 'msw'
import type { AdminEvent, EventLevel } from '../api/types'
import { activity, health, mockAdmin, statsFor, topQueries } from './adminFixtures'

function error(status: number, code: string, message: string) {
  return HttpResponse.json({ code, message }, { status })
}

function paging(url: URL, defaultSize: number) {
  const page = Math.max(0, Number(url.searchParams.get('page') ?? 0))
  const size = Math.max(1, Number(url.searchParams.get('size') ?? defaultSize))
  return { page, size }
}

function pageOf<T>(items: T[], page: number, size: number) {
  return {
    content: items.slice(page * size, page * size + size),
    page,
    size,
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
  }
}

export const adminHandlers = [
  http.get('*/api/v1/admin/stats', ({ request }) =>
    HttpResponse.json(statsFor(new URL(request.url).searchParams.get('period') ?? '24h')),
  ),

  http.get('*/api/v1/admin/health', () => HttpResponse.json(health)),

  http.get('*/api/v1/admin/parsers', () => HttpResponse.json(mockAdmin.parsers)),

  http.post('*/api/v1/admin/parsers/:code/run', ({ params }) => {
    const parser = mockAdmin.parsers.find((p) => p.code === params.code)
    if (!parser) return error(404, 'NOT_FOUND', 'Магазин не найден')
    if (!parser.enabled) return error(409, 'CONFLICT', 'Магазин выключен')
    return HttpResponse.json({ queued: true }, { status: 202 })
  }),

  http.patch('*/api/v1/admin/stores/:code', async ({ params, request }) => {
    const parser = mockAdmin.parsers.find((p) => p.code === params.code)
    if (!parser) return error(404, 'NOT_FOUND', 'Магазин не найден')
    const body = (await request.json()) as { enabled: boolean }
    parser.enabled = body.enabled
    return HttpResponse.json({ code: parser.code, enabled: parser.enabled })
  }),

  http.get('*/api/v1/admin/queues', () => HttpResponse.json(mockAdmin.queues)),

  http.post('*/api/v1/admin/queues/dead-letter/requeue', () => {
    const dead = mockAdmin.queues.find((q) => q.name === 'kr.dead-letter')
    if (dead) dead.ready = 0
    return HttpResponse.json({ requeued: 6, skipped: 1 })
  }),

  http.get('*/api/v1/admin/activity', ({ request }) => {
    const days = Number(new URL(request.url).searchParams.get('days') ?? 14)
    return HttpResponse.json(activity(days))
  }),

  http.get('*/api/v1/admin/events', ({ request }) => {
    const url = new URL(request.url)
    const level = url.searchParams.get('level') as EventLevel | null
    const items: AdminEvent[] = level
      ? mockAdmin.events.filter((e) => e.level === level)
      : mockAdmin.events
    const { page, size } = paging(url, 50)
    return HttpResponse.json(pageOf(items, page, size))
  }),

  http.get('*/api/v1/admin/users', ({ request }) => {
    const url = new URL(request.url)
    const q = (url.searchParams.get('q') ?? '').trim().toLowerCase()
    const verified = url.searchParams.get('verified')
    const items = mockAdmin.users.filter(
      (u) =>
        (!q || u.email?.toLowerCase().includes(q) || u.name?.toLowerCase().includes(q)) &&
        (verified === null || String(u.verified) === verified),
    )
    const { page, size } = paging(url, 20)
    return HttpResponse.json(pageOf(items, page, size))
  }),

  http.get('*/api/v1/admin/notifications', ({ request }) => {
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const delivery = url.searchParams.get('delivery')
    const items = mockAdmin.notifications.filter(
      (n) => (!status || n.status === status) && (!delivery || n.delivery === delivery),
    )
    const { page, size } = paging(url, 20)
    return HttpResponse.json(pageOf(items, page, size))
  }),

  http.post('*/api/v1/admin/notifications/:id/retry', ({ params }) => {
    const item = mockAdmin.notifications.find((n) => String(n.id) === params.id)
    if (!item) return error(404, 'NOT_FOUND', 'Письмо не найдено')
    if (item.status !== 'FAILED') return error(409, 'CONFLICT', 'Письмо нельзя повторить')
    item.status = 'QUEUED'
    item.error = undefined
    return HttpResponse.json(item)
  }),

  http.get('*/api/v1/admin/search/top', () => HttpResponse.json(topQueries())),
]
