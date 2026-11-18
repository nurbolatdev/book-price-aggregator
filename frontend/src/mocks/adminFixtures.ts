import type {
  AdminActivityDay,
  AdminEvent,
  AdminHealth,
  AdminParser,
  AdminPeriod,
  AdminStats,
  QueueStats,
  SearchQueryStat,
} from '../api/types'

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

const iso = (ms: number) => new Date(ms).toISOString()

function initialParsers(): AdminParser[] {
  const now = Date.now()
  return [
    {
      code: 'FLIP',
      name: 'Flip.kz',
      enabled: true,
      hasParser: true,
      lastRun: {
        status: 'SUCCESS',
        startedAt: iso(now - 10 * MINUTE),
        durationSeconds: 41,
      },
      successPercent24h: 100,
      matchedOffers: 1284,
      avgRunSeconds: 41,
      circuitBreaker: 'CLOSED',
    },
    {
      code: 'MELOMAN',
      name: 'Meloman',
      enabled: true,
      hasParser: true,
      lastRun: {
        status: 'PARTIAL',
        startedAt: iso(now - 2 * HOUR),
        durationSeconds: 68,
      },
      successPercent24h: 81,
      matchedOffers: 612,
      avgRunSeconds: 68,
      circuitBreaker: 'CLOSED',
    },
    {
      code: 'OZON',
      name: 'Ozon',
      enabled: true,
      hasParser: true,
      lastRun: {
        status: 'FAILED',
        startedAt: iso(now - 6 * HOUR),
        durationSeconds: 12,
      },
      successPercent24h: 0,
      circuitBreaker: 'OPEN',
    },
    {
      code: 'KASPI',
      name: 'Kaspi.kz',
      enabled: false,
      hasParser: true,
      lastRun: {
        status: 'FAILED',
        startedAt: iso(now - 30 * DAY),
        durationSeconds: 5,
      },
      circuitBreaker: 'CLOSED',
    },
    {
      code: 'WILDBERRIES',
      name: 'Wildberries',
      enabled: false,
      hasParser: true,
      lastRun: {
        status: 'FAILED',
        startedAt: iso(now - 30 * DAY),
        durationSeconds: 5,
      },
      circuitBreaker: 'CLOSED',
    },
  ]
}

function initialQueues(): QueueStats[] {
  return [
    {
      name: 'kr.parse.flip',
      ready: 12,
      unacknowledged: 3,
      consumers: 2,
      ackRate: 0.63,
    },
    {
      name: 'kr.parse.meloman',
      ready: 0,
      unacknowledged: 0,
      consumers: 1,
      ackRate: 0.2,
    },
    {
      name: 'kr.parse.ozon',
      ready: 0,
      unacknowledged: 0,
      consumers: 1,
      ackRate: 0,
    },
    {
      name: 'kr.price-changed.beacons',
      ready: 0,
      unacknowledged: 0,
      consumers: 2,
      ackRate: 2,
    },
    {
      name: 'kr.price-changed.cache',
      ready: 0,
      unacknowledged: 0,
      consumers: 1,
      ackRate: 1.5,
    },
    {
      name: 'kr.notify.email',
      ready: 4,
      unacknowledged: 1,
      consumers: 1,
      ackRate: 0.1,
    },
    {
      name: 'kr.dead-letter',
      ready: 7,
      unacknowledged: 0,
      consumers: 0,
      ackRate: 0,
    },
  ]
}

const LEAD_EVENTS: Array<Omit<AdminEvent, 'id' | 'createdAt'> & { minutesAgo: number }> = [
  {
    minutesAgo: 8,
    level: 'INFO',
    source: 'parser',
    message: 'Flip.kz: обновлено 1 284 предложения',
    details: { store: 'FLIP', offers: 1284 },
  },
  {
    minutesAgo: 70,
    level: 'WARN',
    source: 'parser',
    message: 'Meloman: изменилась вёрстка карточки, 19% страниц без цены',
    details: { store: 'MELOMAN', missingPricePercent: 19 },
  },
  {
    minutesAgo: 95,
    level: 'INFO',
    source: 'beacon',
    message: 'Маячок сработал: «Мастер и Маргарита», письмо поставлено в очередь',
    details: { beaconId: 1, editionId: 16 },
  },
  {
    minutesAgo: 130,
    level: 'ERROR',
    source: 'mail',
    message: 'Письмо не отправлено: SMTP connection timed out',
    details: { notificationId: 103, attempt: 3 },
  },
  {
    minutesAgo: 190,
    level: 'ERROR',
    source: 'parser',
    message: 'Ozon: таймаут 3 раза подряд, circuit breaker открыт',
    details: { store: 'OZON', failures: 3 },
  },
]

const EVENT_COUNT = 120
const LEVELS: NonNullable<AdminEvent['level']>[] = ['INFO', 'INFO', 'WARN', 'ERROR']
const SOURCES = ['parser', 'beacon', 'mail', 'scheduler']

function initialEvents(): AdminEvent[] {
  const now = Date.now()
  return Array.from({ length: EVENT_COUNT }, (_, i) => {
    const lead = LEAD_EVENTS[i]
    if (lead) {
      const { minutesAgo, ...rest } = lead
      return {
        id: EVENT_COUNT - i,
        createdAt: iso(now - minutesAgo * MINUTE),
        ...rest,
      }
    }
    return {
      id: EVENT_COUNT - i,
      createdAt: iso(now - (200 + i * 15) * MINUTE),
      level: LEVELS[i % LEVELS.length],
      source: SOURCES[i % SOURCES.length],
      message: `Событие системы №${EVENT_COUNT - i}`,
      details: { sequence: EVENT_COUNT - i },
    }
  })
}

interface MockAdminState {
  parsers: AdminParser[]
  queues: QueueStats[]
  events: AdminEvent[]
}

function initialState(): MockAdminState {
  return {
    parsers: initialParsers(),
    queues: initialQueues(),
    events: initialEvents(),
  }
}

let state = initialState()

export const mockAdmin = {
  get parsers() {
    return state.parsers
  },
  get queues() {
    return state.queues
  },
  get events() {
    return state.events
  },
}

export function resetMockAdmin(): void {
  state = initialState()
}

const STATS: Record<AdminPeriod, AdminStats> = {
  '24h': {
    period: '24h',
    users: { total: 128, newInPeriod: 3, verified: 117 },
    activeUsers: 41,
    beacons: { active: 312, triggeredInPeriod: 9 },
    emails: { sent: 27, failed: 1 },
    searches: 1840,
    cacheHitPercent: 72.4,
    catalog: {
      books: 1842,
      editions: 2917,
      matchedOffers: 4105,
      pendingReview: 23,
    },
  },
  '7d': {
    period: '7d',
    users: { total: 128, newInPeriod: 14, verified: 117 },
    activeUsers: 76,
    beacons: { active: 312, triggeredInPeriod: 41 },
    emails: { sent: 168, failed: 4 },
    searches: 10_420,
    cacheHitPercent: 70.1,
    catalog: {
      books: 1842,
      editions: 2917,
      matchedOffers: 4105,
      pendingReview: 23,
    },
  },
  '30d': {
    period: '30d',
    users: { total: 128, newInPeriod: 52, verified: 117 },
    activeUsers: 104,
    beacons: { active: 312, triggeredInPeriod: 163 },
    emails: { sent: 702, failed: 15 },
    searches: 41_960,
    cacheHitPercent: 69.3,
    catalog: {
      books: 1842,
      editions: 2917,
      matchedOffers: 4105,
      pendingReview: 23,
    },
  },
}

export function statsFor(period: string): AdminStats {
  return STATS[period as AdminPeriod] ?? STATS['24h']
}

export const health: AdminHealth = {
  status: 'UP',
  components: {
    db: { status: 'UP' },
    redis: { status: 'UP' },
    rabbit: { status: 'UP' },
    mail: { status: 'UP' },
    diskSpace: { status: 'UP' },
    ping: { status: 'UP' },
    scheduler: { status: 'UP' },
  },
  metrics: {
    httpP95Millis: 184.3,
    dbPoolActive: 3,
    dbPoolMax: 20,
    redisUsedMemoryBytes: 12_582_912,
  },
}

export function activity(days: number): AdminActivityDay[] {
  const today = new Date()
  today.setUTCHours(0, 0, 0, 0)
  return Array.from({ length: days }, (_, i) => {
    const step = days > 1 ? i / (days - 1) : 0
    const date = new Date(today.getTime() - (days - 1 - i) * DAY)
    return {
      date: date.toISOString().slice(0, 10),
      searches: Math.round(60 + step * 70),
      beacons: Math.round(18 + step * 36),
      registrations: 2 + (i % 4),
      emails: 10 + ((i * 3) % 9),
    }
  })
}

const TOP: SearchQueryStat[] = [
  { query: 'Мастер и Маргарита', searches: 212 },
  { query: 'Атомные привычки', searches: 148 },
  { query: 'Абай жолы', searches: 97 },
  { query: 'Чистый код', searches: 80 },
  { query: 'Гарри Поттер', searches: 64 },
]

export function topQueries(): SearchQueryStat[] {
  return TOP
}
