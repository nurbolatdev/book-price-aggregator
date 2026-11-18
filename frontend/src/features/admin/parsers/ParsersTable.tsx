import type { AdminParser } from '../../../api/types'
import { Button } from '../../../components/ui/Button'
import { formatNumber } from '../../../lib/format'
import { formatAdminTime } from '../adminFormat'
import { StatusBadge } from '../StatusBadge'
import { parserStatus } from '../status'
import { row, table, td, th, tableWrap } from '../table'
import type { ParserActions } from './useParserActions'

interface ParsersTableProps {
  parsers: AdminParser[]
  variant: 'compact'
  actions: ParserActions
}

const DASH = '—'

function disabledReason(p: AdminParser): string | undefined {
  if (!p.enabled) return 'Сначала включите магазин'
  if (!p.hasParser) return 'У магазина нет парсера'
  return undefined
}

export function ParsersTable({ parsers, actions }: ParsersTableProps) {
  const now = new Date()
  return (
    <div className={tableWrap}>
      <table className={`${table} min-w-[760px]`}>
        <thead>
          <tr>
            <th scope="col" className={th}>
              Магазин
            </th>
            <th scope="col" className={th}>
              Статус
            </th>
            <th scope="col" className={th}>
              Последний запуск
            </th>
            <th scope="col" className={th}>
              Успешно
            </th>
            <th scope="col" className={th}>
              Предложений
            </th>
            <th scope="col" className={th}>
              Ср. время
            </th>
            <th scope="col" className={th}>
              <span className="sr-only">Действия</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {parsers.map((p) => {
            const code = p.code
            const status = parserStatus(p)
            const reason = disabledReason(p)
            const live = Boolean(p.enabled)
            const message = code ? actions.rowError[code] : undefined
            return (
              <tr key={code ?? p.name} className={row}>
                <td className={`${td} font-semibold`}>{p.name}</td>
                <td className={td}>
                  <StatusBadge tone={status.tone}>{status.label}</StatusBadge>
                </td>
                <td className={td}>
                  {p.lastRun?.startedAt ? formatAdminTime(p.lastRun.startedAt, now) : DASH}
                </td>
                <td className={td}>
                  {live && p.successPercent24h !== undefined ? `${Math.round(p.successPercent24h)}%` : DASH}
                </td>
                <td className={td}>
                  {live && p.matchedOffers !== undefined ? formatNumber(p.matchedOffers) : DASH}
                </td>
                <td className={td}>
                  {live && p.avgRunSeconds !== undefined ? `${Math.round(p.avgRunSeconds)} с` : DASH}
                </td>
                <td className={td}>
                  {code && (
                    <Button
                      variant="secondary"
                      size="sm"
                      disabled={reason !== undefined}
                      title={reason}
                      loading={actions.running.has(code)}
                      onClick={() => void actions.run(code)}
                    >
                      Перезапустить
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
