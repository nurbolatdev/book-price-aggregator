import type { QueueStats } from '../../../api/types'
import { formatNumber } from '../../../lib/format'
import { perMinute } from '../adminFormat'
import { row, table, td, th, tableWrap } from '../table'
import { DEAD_LETTER, queueCaption } from './queueCaption'

export function QueuesTable({ queues }: { queues: QueueStats[] }) {
  return (
    <div className={tableWrap}>
      <table className={`${table} min-w-[640px]`}>
        <thead>
          <tr>
            <th scope="col" className={th}>
              Очередь
            </th>
            <th scope="col" className={th}>
              Готовы к обработке
            </th>
            <th scope="col" className={th}>
              В работе (unacked)
            </th>
            <th scope="col" className={th}>
              Консьюмеры
            </th>
            <th scope="col" className={th}>
              Скорость подтверждения
            </th>
          </tr>
        </thead>
        <tbody>
          {queues.map((q) => {
            const dead = q.name === DEAD_LETTER
            const ready = q.ready ?? 0
            const caption = queueCaption(q.name)
            return (
              <tr key={q.name} className={row}>
                <td className={td}>
                  <div className="flex flex-col">
                    <code className="text-[13px] font-semibold">{q.name}</code>
                    {caption && <span className="text-xs text-muted">{caption}</span>}
                  </div>
                </td>
                <td className={td}>
                  {dead && ready > 0 ? (
                    <strong className="text-danger">{formatNumber(ready)}</strong>
                  ) : (
                    formatNumber(ready)
                  )}
                </td>
                <td className={td}>{formatNumber(q.unacknowledged ?? 0)}</td>
                <td className={td}>{formatNumber(q.consumers ?? 0)}</td>
                <td className={td}>{dead ? '—' : `${perMinute(q.ackRate ?? 0)}/мин`}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
