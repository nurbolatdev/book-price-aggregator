import type { AdminHealth, QueueStats } from '../../../api/types'
import { cn } from '../../../lib/cn'
import { formatMb } from '../adminFormat'
import { healthStatus } from '../status'

interface HealthGridProps {
  health: AdminHealth
  queues?: QueueStats[]
}

interface Card {
  name: string
  status: string | undefined
  detail?: string
}

export function HealthGrid({ health, queues }: HealthGridProps) {
  const components = health.components ?? {}
  const metrics = health.metrics
  const consumers = queues?.reduce((sum, q) => sum + (q.consumers ?? 0), 0)

  const cards: Card[] = [
    {
      name: 'Backend API',
      status: health.status,
      detail:
        metrics?.httpP95Millis !== undefined
          ? `p95 ответа: ${Math.round(metrics.httpP95Millis)} мс`
          : undefined,
    },
    {
      name: 'PostgreSQL',
      status: components.db?.status,
      detail:
        components.db?.details ??
        (metrics?.dbPoolActive !== undefined && metrics.dbPoolMax !== undefined
          ? `подключений: ${metrics.dbPoolActive}/${metrics.dbPoolMax}`
          : undefined),
    },
    {
      name: 'Redis',
      status: components.redis?.status,
      detail:
        components.redis?.details ??
        (metrics?.redisUsedMemoryBytes !== undefined
          ? `память: ${formatMb(metrics.redisUsedMemoryBytes)} МБ`
          : undefined),
    },
    {
      name: 'RabbitMQ',
      status: components.rabbit?.status,
      detail:
        components.rabbit?.details ?? (consumers !== undefined ? `консьюмеров: ${consumers}` : undefined),
    },
    {
      name: 'SMTP (почта)',
      status: components.mail?.status,
      detail: components.mail?.details,
    },
    {
      name: 'Планировщик',
      status: components.scheduler?.status,
      detail: components.scheduler?.details,
    },
  ]

  return (
    <ul className="grid list-none grid-cols-[repeat(auto-fill,minmax(170px,1fr))] gap-2.5">
      {cards.map((card) => {
        const status = healthStatus(card.status)
        return (
          <li key={card.name} className="flex flex-col gap-1 rounded-xl border border-line p-3">
            <span className="flex items-center gap-2 font-semibold">
              <span aria-hidden="true" className={cn('h-2.5 w-2.5 shrink-0 rounded-full', status.dotClass)} />
              {card.name}
            </span>
            <span className="text-[13px] text-ink-soft">{status.label}</span>
            {card.detail && <span className="text-xs text-muted">{card.detail}</span>}
          </li>
        )
      })}
    </ul>
  )
}
