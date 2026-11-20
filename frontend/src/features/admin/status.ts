import type { BadgeTone } from '../../components/ui/Badge'

export interface ParserStatusInput {
  enabled?: boolean
  hasParser?: boolean
  lastRun?: { status?: string } | null
}

export function parserStatus(p: ParserStatusInput): { label: string; tone: BadgeTone } {
  if (!p.enabled) return { label: 'Выключен', tone: 'off' }
  if (!p.hasParser) return { label: 'Нет парсера', tone: 'off' }
  switch (p.lastRun?.status) {
    case 'SUCCESS':
      return { label: 'OK', tone: 'ok' }
    case 'PARTIAL':
      return { label: 'Частичные ошибки', tone: 'warn' }
    case 'FAILED':
      return { label: 'Не отвечает', tone: 'danger' }
    case 'RUNNING':
      return { label: 'Идёт обход', tone: 'neutral' }
    default:
      return { label: 'Нет запусков', tone: 'neutral' }
  }
}

export function breakerLabel(state: string | undefined): { label: string; className: string } {
  switch (state) {
    case 'CLOSED':
      return { label: 'закрыт', className: 'text-deal' }
    case 'OPEN':
      return { label: 'открыт', className: 'font-semibold text-danger' }
    case 'HALF_OPEN':
      return { label: 'полуоткрыт', className: 'text-warn' }
    default:
      return { label: '—', className: 'text-muted' }
  }
}

export function healthStatus(code: string | undefined): { label: string; dotClass: string } {
  switch (code) {
    case 'UP':
      return { label: 'Работает', dotClass: 'bg-dot-ok' }
    case 'DOWN':
      return { label: 'Не отвечает', dotClass: 'bg-danger' }
    case 'OUT_OF_SERVICE':
      return { label: 'Выключен', dotClass: 'bg-dot-off' }
    default:
      return { label: 'Нет данных', dotClass: 'bg-dot-off' }
  }
}

export function notificationStatus(code: string | undefined): { label: string; tone: BadgeTone } {
  switch (code) {
    case 'QUEUED':
      return { label: 'В очереди', tone: 'off' }
    case 'SENT':
      return { label: 'Отправлено', tone: 'ok' }
    case 'FAILED':
      return { label: 'Ошибка', tone: 'danger' }
    default:
      return { label: code ?? '—', tone: 'neutral' }
  }
}

export function deliveryLabel(code: string | undefined): string {
  switch (code) {
    case 'INSTANT':
      return 'сразу'
    case 'DIGEST':
      return 'сводка'
    case 'WEEKLY':
      return 'подборка недели'
    default:
      return code ?? '—'
  }
}

export function levelTone(level: string | undefined): BadgeTone {
  if (level === 'INFO') return 'ok'
  if (level === 'WARN') return 'warn'
  if (level === 'ERROR') return 'danger'
  return 'neutral'
}
