import type { AdminPeriod, AdminStats } from '../../../api/types'
import { formatNumber } from '../../../lib/format'
import { periodLabel } from '../adminFormat'
import { KpiCard, KpiGrid } from '../KpiCard'

export function StatsKpis({ stats, period }: { stats: AdminStats; period: AdminPeriod }) {
  const label = periodLabel(period)
  return (
    <KpiGrid>
      <KpiCard
        label="Пользователи"
        value={formatNumber(stats.users?.total ?? 0)}
        caption={`+${formatNumber(stats.users?.newInPeriod ?? 0)} за ${label}`}
      />
      <KpiCard
        label={`Активны за ${label}`}
        value={formatNumber(stats.activeUsers ?? 0)}
        caption="активны за период"
      />
      <KpiCard
        label="Активные маячки"
        value={formatNumber(stats.beacons?.active ?? 0)}
        caption={`${formatNumber(stats.beacons?.triggeredInPeriod ?? 0)} сработали за ${label}`}
      />
      <KpiCard
        label="Писем отправлено"
        value={formatNumber(stats.emails?.sent ?? 0)}
        caption={`ошибок доставки: ${formatNumber(stats.emails?.failed ?? 0)}`}
      />
      <KpiCard
        label={`Поисков за ${label}`}
        value={formatNumber(stats.searches ?? 0)}
        caption={`попадание в кэш ${Math.round(stats.cacheHitPercent ?? 0)}%`}
      />
      <KpiCard
        label="Книг / предложений"
        value={`${formatNumber(stats.catalog?.books ?? 0)} / ${formatNumber(stats.catalog?.matchedOffers ?? 0)}`}
        caption="в каталоге и у магазинов"
      />
    </KpiGrid>
  )
}
