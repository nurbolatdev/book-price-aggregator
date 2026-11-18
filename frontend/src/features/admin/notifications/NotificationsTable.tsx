import type { AdminNotification } from '../../../api/types'
import { Badge } from '../../../components/ui/Badge'
import { Button } from '../../../components/ui/Button'
import { formatAdminTime } from '../adminFormat'
import { deliveryLabel, notificationStatus } from '../status'
import { row, table, tableWrap, td, th } from '../table'
import type { useRetryNotification } from './useRetryNotification'

interface NotificationsTableProps {
  items: AdminNotification[]
  actions: ReturnType<typeof useRetryNotification>
}

export function NotificationsTable({ items, actions }: NotificationsTableProps) {
  const now = new Date()
  return (
    <div className={tableWrap}>
      <table className={`${table} min-w-[920px]`}>
        <thead>
          <tr>
            <th scope="col" className={th}>
              Создано
            </th>
            <th scope="col" className={th}>
              Получатель
            </th>
            <th scope="col" className={th}>
              Книга / тема
            </th>
            <th scope="col" className={th}>
              Доставка
            </th>
            <th scope="col" className={th}>
              Статус
            </th>
            <th scope="col" className={th}>
              Ошибка
            </th>
            <th scope="col" className={th}>
              <span className="sr-only">Действия</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((n) => {
            const status = notificationStatus(n.status)
            const id = n.id
            const message = id === undefined ? undefined : actions.rowError[id]
            return (
              <tr key={id} className={row}>
                <td className={td}>{n.createdAt ? formatAdminTime(n.createdAt, now) : '—'}</td>
                <td className={td}>{n.userEmail}</td>
                <td className={td}>{n.title}</td>
                <td className={td}>{deliveryLabel(n.delivery)}</td>
                <td className={td}>
                  <Badge tone={status.tone} className="font-semibold whitespace-nowrap">
                    {status.label}
                  </Badge>
                </td>
                <td className={`${td} text-[13px] ${n.error ? 'text-danger' : ''}`}>{n.error || '—'}</td>
                <td className={td}>
                  {n.status === 'FAILED' && id !== undefined && (
                    <Button
                      variant="secondary"
                      size="sm"
                      loading={actions.retrying.has(id)}
                      onClick={() => void actions.retry(id)}
                    >
                      Повторить
                    </Button>
                  )}
                  {message && (
                    <p role="alert" className="mt-1 text-[13px] text-danger">
                      {message}
                    </p>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
