import type { BeaconResponse } from '../api/types'

const HOUR = 3_600_000
const DAY = 24 * HOUR

function initialBeacons(): BeaconResponse[] {
  const now = Date.now()
  const iso = (ms: number) => new Date(ms).toISOString()
  return [
    {
      id: 1,
      editionId: 16,
      title: 'Мастер и Маргарита',
      author: 'Михаил Булгаков',
      conditionType: 'BELOW_PRICE',
      targetPrice: 3200,
      status: 'TRIGGERED',
      currentBestPrice: 3150,
      currentBestStore: 'FLIP',
      currentBestUrl: 'https://example.com/flip/16',
      baselinePrice: 3610,
      lastTriggeredAt: iso(now - HOUR),
      createdAt: iso(now - 20 * DAY),
      statusChangedAt: iso(now - HOUR),
    },
    {
      id: 2,
      editionId: 7,
      title: 'Атомные привычки',
      author: 'Джеймс Клир',
      conditionType: 'ANY_DROP',
      status: 'ACTIVE',
      currentBestPrice: 5290,
      currentBestStore: 'FLIP',
      baselinePrice: 5990,
      createdAt: iso(now - 14 * DAY),
      statusChangedAt: iso(now - 14 * DAY),
    },
    {
      id: 3,
      editionId: 9,
      title: 'Чистый код',
      author: 'Роберт Мартин',
      conditionType: 'BELOW_PRICE',
      targetPrice: 8000,
      status: 'ACTIVE',
      currentBestPrice: 8900,
      currentBestStore: 'MELOMAN',
      baselinePrice: 8900,
      createdAt: iso(now - 10 * DAY),
      statusChangedAt: iso(now - 10 * DAY),
    },
    {
      id: 4,
      editionId: 8,
      title: 'Абай жолы',
      author: 'Мұхтар Әуезов',
      conditionType: 'DISCOUNT_PERCENT',
      targetPercent: 15,
      status: 'ACTIVE',
      currentBestPrice: 6400,
      currentBestStore: 'FLIP',
      baselinePrice: 7110,
      createdAt: iso(now - 8 * DAY),
      statusChangedAt: iso(now - 8 * DAY),
    },
    {
      id: 5,
      editionId: 61,
      title: 'Сто лет одиночества',
      author: 'Габриэль Гарсиа Маркес',
      conditionType: 'BELOW_PRICE',
      targetPrice: 3000,
      status: 'PAUSED',
      currentBestPrice: 3450,
      currentBestStore: 'OZON',
      baselinePrice: 3450,
      createdAt: iso(now - 60 * DAY),
      statusChangedAt: iso(now - 30 * DAY),
    },
  ]
}

function startsEmpty(): boolean {
  try {
    return sessionStorage.getItem('kr.mock.beacons') === 'empty'
  } catch {
    return false
  }
}

function freshList(): BeaconResponse[] {
  return startsEmpty() ? [] : initialBeacons()
}

export let mockBeacons: BeaconResponse[] = freshList()
let nextId = 6

export function resetMockBeacons(): void {
  mockBeacons = freshList()
  nextId = 6
}

export function takeMockBeaconId(): number {
  return nextId++
}

export function setMockBeacons(list: BeaconResponse[]): void {
  mockBeacons = list
}
