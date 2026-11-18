import { describe, expect, it } from 'vitest'
import { breakerLabel, deliveryLabel, healthStatus, levelTone, notificationStatus, parserStatus } from './status'

const on = { enabled: true, hasParser: true }

describe('status', () => {
  it.each([
    [{ ...on, lastRun: { status: 'SUCCESS' } }, 'OK', 'ok'],
    [{ ...on, lastRun: { status: 'PARTIAL' } }, 'Частичные ошибки', 'warn'],
    [{ ...on, lastRun: { status: 'FAILED' } }, 'Не отвечает', 'danger'],
    [{ ...on, lastRun: { status: 'RUNNING' } }, 'Идёт обход', 'neutral'],
    [{ ...on }, 'Нет запусков', 'neutral'],
    [{ enabled: true, hasParser: false, lastRun: { status: 'SUCCESS' } }, 'Нет парсера', 'off'],
    [{ enabled: false, hasParser: true, lastRun: { status: 'FAILED' } }, 'Выключен', 'off'],
  ])('parser status %#', (input, label, tone) => {
    expect(parserStatus(input)).toEqual({ label, tone })
  })

  it('maps circuit breaker states', () => {
    expect(breakerLabel('CLOSED')).toEqual({ label: 'закрыт', className: 'text-deal' })
    expect(breakerLabel('OPEN').label).toBe('открыт')
    expect(breakerLabel('OPEN').className).toContain('text-danger')
    expect(breakerLabel('HALF_OPEN')).toEqual({ label: 'полуоткрыт', className: 'text-warn' })
    expect(breakerLabel(undefined).label).toBe('—')
    expect(breakerLabel('DISABLED').label).toBe('—')
  })

  it('maps health codes', () => {
    expect(healthStatus('UP')).toEqual({ label: 'Работает', dotClass: 'bg-dot-ok' })
    expect(healthStatus('DOWN')).toEqual({ label: 'Не отвечает', dotClass: 'bg-danger' })
    expect(healthStatus('OUT_OF_SERVICE')).toEqual({ label: 'Выключен', dotClass: 'bg-dot-off' })
    expect(healthStatus('UNKNOWN')).toEqual({ label: 'Нет данных', dotClass: 'bg-dot-off' })
    expect(healthStatus(undefined)).toEqual({ label: 'Нет данных', dotClass: 'bg-dot-off' })
  })

  it('maps notification statuses and delivery', () => {
    expect(notificationStatus('QUEUED')).toEqual({ label: 'В очереди', tone: 'off' })
    expect(notificationStatus('SENT')).toEqual({ label: 'Отправлено', tone: 'ok' })
    expect(notificationStatus('FAILED')).toEqual({ label: 'Ошибка', tone: 'danger' })
    expect(deliveryLabel('INSTANT')).toBe('сразу')
    expect(deliveryLabel('DIGEST')).toBe('сводка')
    expect(deliveryLabel('WEEKLY')).toBe('подборка недели')
  })

  it('maps event levels', () => {
    expect(levelTone('INFO')).toBe('ok')
    expect(levelTone('WARN')).toBe('warn')
    expect(levelTone('ERROR')).toBe('danger')
  })
})
