import { HttpResponse, http } from 'msw'
import type { BeaconResponse, CreateBeaconRequest, UpdateBeaconRequest } from '../api/types'
import { editionDetail } from './editionFixtures'
import { mockBeacons, setMockBeacons, takeMockBeaconId } from './beaconFixtures'

function error(status: number, code: string, message: string, extra: Record<string, unknown> = {}) {
  return HttpResponse.json({ code, message, ...extra }, { status })
}

function invalid(field: string, message: string) {
  return error(422, 'VALIDATION_FAILED', 'Проверьте введённые данные', {
    fieldErrors: [{ field, message }],
  })
}

async function jsonBody<T>(request: Request): Promise<T> {
  try {
    return ((await request.json()) as T) ?? ({} as T)
  } catch {
    return {} as T
  }
}

function conditionError(body: Partial<CreateBeaconRequest>) {
  if (body.conditionType === 'BELOW_PRICE' && !(Number(body.targetPrice) > 0)) {
    return invalid('targetPrice', 'должно быть больше 0')
  }
  if (body.conditionType === 'DISCOUNT_PERCENT') {
    const percent = Number(body.targetPercent)
    if (!(percent >= 1 && percent <= 90)) return invalid('targetPercent', 'должно быть от 1 до 90')
  }
  return null
}

function applyCondition(beacon: BeaconResponse, body: UpdateBeaconRequest): BeaconResponse {
  if (!body.conditionType) return beacon
  const rest: BeaconResponse = { ...beacon }
  delete rest.targetPrice
  delete rest.targetPercent
  return {
    ...rest,
    conditionType: body.conditionType,
    ...(body.conditionType === 'BELOW_PRICE' ? { targetPrice: body.targetPrice } : {}),
    ...(body.conditionType === 'DISCOUNT_PERCENT' ? { targetPercent: body.targetPercent } : {}),
  }
}

export const beaconHandlers = [
  http.get('*/api/v1/beacons', ({ request }) => {
    const params = new URL(request.url).searchParams
    const editionId = params.get('editionId')
    const status = params.get('status')
    return HttpResponse.json(
      mockBeacons.filter(
        (b) =>
          (editionId === null || b.editionId === Number(editionId)) &&
          (status === null || b.status === status),
      ),
    )
  }),

  http.post('*/api/v1/beacons', async ({ request }) => {
    const body = await jsonBody<CreateBeaconRequest>(request)
    if (mockBeacons.some((b) => b.editionId === body.editionId)) {
      return error(409, 'CONFLICT', 'На это издание уже есть маячок')
    }
    const failure = conditionError(body)
    if (failure) return failure
    const detail = editionDetail(body.editionId)
    const now = new Date().toISOString()
    const price = detail?.bestPrice?.price
    const created: BeaconResponse = {
      id: takeMockBeaconId(),
      editionId: body.editionId,
      title: detail?.book?.title ?? `Издание ${body.editionId}`,
      author: detail?.book?.author,
      coverUrl: detail?.coverUrl,
      conditionType: body.conditionType,
      ...(body.conditionType === 'BELOW_PRICE' ? { targetPrice: body.targetPrice } : {}),
      ...(body.conditionType === 'DISCOUNT_PERCENT' ? { targetPercent: body.targetPercent } : {}),
      status: 'ACTIVE',
      currentBestPrice: price,
      currentBestStore: detail?.bestPrice?.store,
      baselinePrice: price,
      createdAt: now,
      statusChangedAt: now,
    }
    setMockBeacons([created, ...mockBeacons])
    return HttpResponse.json(created, { status: 201 })
  }),

  http.patch('*/api/v1/beacons/:id', async ({ params, request }) => {
    const id = Number(params.id)
    const current = mockBeacons.find((b) => b.id === id)
    if (!current) return error(404, 'NOT_FOUND', 'Маячок не найден')
    const body = await jsonBody<UpdateBeaconRequest>(request)
    const failure = body.conditionType ? conditionError(body as CreateBeaconRequest) : null
    if (failure) return failure
    let updated = applyCondition(current, body)
    if (body.status) {
      updated = { ...updated, status: body.status, statusChangedAt: new Date().toISOString() }
    }
    setMockBeacons(mockBeacons.map((b) => (b.id === id ? updated : b)))
    return HttpResponse.json(updated)
  }),

  http.delete('*/api/v1/beacons/:id', ({ params }) => {
    const id = Number(params.id)
    if (!mockBeacons.some((b) => b.id === id)) return error(404, 'NOT_FOUND', 'Маячок не найден')
    setMockBeacons(mockBeacons.filter((b) => b.id !== id))
    return new HttpResponse(null, { status: 204 })
  }),

  http.post('*/api/v1/beacons/unsubscribe', async ({ request }) => {
    const { token } = await jsonBody<{ token?: string }>(request)
    if (token === 'valid') {
      setMockBeacons(
        mockBeacons.map((b) =>
          b.id === 1 ? { ...b, status: 'PAUSED', statusChangedAt: new Date().toISOString() } : b,
        ),
      )
      return HttpResponse.json({
        beaconId: 1,
        editionId: 16,
        title: 'Мастер и Маргарита',
        status: 'PAUSED',
      })
    }
    if (token === 'expired') return error(401, 'TOKEN_EXPIRED', 'Срок действия ссылки истёк')
    return error(400, 'TOKEN_INVALID', 'Ссылка недействительна')
  }),
]
