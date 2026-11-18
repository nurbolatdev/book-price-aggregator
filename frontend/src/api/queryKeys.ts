import type {
  AdminPeriod,
  EventLevel,
  NotificationDelivery,
  NotificationStatus,
  PricePeriod,
} from './types'
import type { FacetParams, SearchParams } from './search'

export const queryKeys = {
  me: ['me'],
  stores: ['stores'],
  popular: (limit: number) => ['search', 'popular', limit],
  suggest: (q: string) => ['search', 'suggest', q],
  search: (params: SearchParams) => ['search', 'results', params],
  facets: (params: FacetParams) => ['search', 'facets', params],
  drops: (limit: number) => ['home', 'drops', limit],
  edition: (id: number) => ['editions', id],
  priceHistory: (id: number, period: PricePeriod) => ['editions', id, 'price-history', period],
  similar: (id: number, limit: number) => ['editions', id, 'similar', limit],
  book: (id: number) => ['books', id],
  beacons: ['beacons', 'list'],
  beaconForEdition: (id: number) => ['beacons', 'edition', id],
  admin: {
    stats: (period: AdminPeriod) => ['admin', 'stats', period],
    health: ['admin', 'health'],
    parsers: ['admin', 'parsers'],
    queues: ['admin', 'queues'],
    activity: (days: number) => ['admin', 'activity', days],
    recentEvents: (size: number) => ['admin', 'events', 'recent', size],
    events: (level: EventLevel | undefined) => ['admin', 'events', 'list', level ?? 'ALL'],
    topQueries: (period: AdminPeriod) => ['admin', 'top-queries', period],
    zeroResults: (period: AdminPeriod) => ['admin', 'zero-results', period],
    users: (params: { q?: string; verified?: boolean; page: number }) => ['admin', 'users', params],
    notifications: (params: {
      status?: NotificationStatus
      delivery?: NotificationDelivery
      page: number
      size?: number
    }) => ['admin', 'notifications', params],
    pendingOffers: (page: number) => ['admin', 'pending-offers', page],
    editionSearch: (q: string) => ['admin', 'edition-search', q],
  },
} as const

export const USER_SCOPED_KEYS: readonly (readonly string[])[] = [['me'], ['beacons'], ['admin']]
