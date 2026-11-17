import { http } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { MOCK_ACCESS_TOKEN } from '../mocks/authFixtures'
import { resetMockBeacons } from '../mocks/beaconFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import {
  createBeacon,
  deleteBeacon,
  fetchBeaconForEdition,
  fetchBeacons,
  unsubscribeBeacon,
  updateBeacon,
} from './beacons'

beforeEach(() => {
  useAuthStore.setState({ status: 'anonymous', user: null, accessToken: MOCK_ACCESS_TOKEN })
})

describe('beacon api', () => {
  it('lists five beacons with the triggered one first', async () => {
    const list = await fetchBeacons()
    expect(list).toHaveLength(5)
    expect(list[0]?.status).toBe('TRIGGERED')
  })

  it('finds the beacon of an edition or null', async () => {
    expect(await fetchBeaconForEdition(41)).toBeNull()
    expect((await fetchBeaconForEdition(16))?.id).toBe(1)
  })

  it('creates a beacon and sends exactly the given body', async () => {
    let body: unknown
    server.use(
      http.post('*/api/v1/beacons', async ({ request }) => {
        body = await request.clone().json()
        return undefined
      }),
    )
    const request = { editionId: 41, conditionType: 'BELOW_PRICE', targetPrice: 2840 } as const
    const created = await createBeacon(request)
    expect(body).toEqual(request)
    expect(created.title).toBe('Мастер и Маргарита')
    expect(created.status).toBe('ACTIVE')
  })

  it('rejects a duplicate with CONFLICT', async () => {
    await expect(
      createBeacon({ editionId: 16, conditionType: 'ANY_DROP' }),
    ).rejects.toMatchObject({ code: 'CONFLICT' })
  })

  it('rejects BELOW_PRICE without a price with a field error', async () => {
    await expect(
      createBeacon({ editionId: 41, conditionType: 'BELOW_PRICE' }),
    ).rejects.toMatchObject({
      code: 'VALIDATION_FAILED',
      fieldErrors: [expect.objectContaining({ field: 'targetPrice' })],
    })
  })

  it('pauses a beacon and moves statusChangedAt forward', async () => {
    const before = (await fetchBeacons()).find((b) => b.id === 2)
    const updated = await updateBeacon(2, { status: 'PAUSED' })
    expect(updated.status).toBe('PAUSED')
    expect(new Date(updated.statusChangedAt!).getTime()).toBeGreaterThan(
      new Date(before!.statusChangedAt!).getTime(),
    )
  })

  it('deletes a beacon', async () => {
    await deleteBeacon(3)
    expect(await fetchBeacons()).toHaveLength(4)
  })

  it('unsubscribes by token', async () => {
    expect(await unsubscribeBeacon('valid')).toMatchObject({
      title: 'Мастер и Маргарита',
      status: 'PAUSED',
    })
    await expect(unsubscribeBeacon('expired')).rejects.toMatchObject({ code: 'TOKEN_EXPIRED' })
    await expect(unsubscribeBeacon('x')).rejects.toMatchObject({ code: 'TOKEN_INVALID' })
  })

  it('starts empty when the session flag says so', async () => {
    sessionStorage.setItem('kr.mock.beacons', 'empty')
    resetMockBeacons()
    expect(await fetchBeacons()).toEqual([])
  })
})
