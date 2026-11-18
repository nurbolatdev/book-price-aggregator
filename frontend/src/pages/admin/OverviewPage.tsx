import { useSearchParams } from "react-router-dom";
import type { AdminPeriod } from "../../api/types";
import { ErrorBlock } from "../../components/ui/ErrorBlock";
import { Skeleton } from "../../components/ui/Skeleton";
import { Button } from "../../components/ui/Button";
import { ADMIN_PERIODS } from "../../features/admin/adminFormat";
import { AdminPageHeader } from "../../features/admin/AdminPageHeader";
import { AdminSection } from "../../features/admin/AdminSection";
import { SegmentedControl } from "../../features/admin/SegmentedControl";
import { ActivitySection } from "../../features/admin/overview/ActivitySection";
import { HealthGrid } from "../../features/admin/overview/HealthGrid";
import { QueuesSummary } from "../../features/admin/overview/QueuesSummary";
import { RecentEvents } from "../../features/admin/overview/RecentEvents";
import { StatsKpis } from "../../features/admin/overview/StatsKpis";
import { TopQueries } from "../../features/admin/overview/TopQueries";
import {
  useAdminHealth,
  useAdminQueues,
  useAdminStats,
} from "../../features/admin/overview/useOverviewQueries";
import { ParsersTable } from "../../features/admin/parsers/ParsersTable";
import { useAdminParsers } from "../../features/admin/parsers/useAdminParsers";
import { useParserActions } from "../../features/admin/parsers/useParserActions";

function parsePeriod(value: string | null): AdminPeriod {
  return ADMIN_PERIODS.find((p) => p.value === value)?.value ?? "24h";
}

export default function OverviewPage() {
  const [params, setParams] = useSearchParams();
  const period = parsePeriod(params.get("period"));
  const stats = useAdminStats(period);
  const health = useAdminHealth();
  const parsers = useAdminParsers();
  const queues = useAdminQueues();
  const actions = useParserActions();

  const updatedAt = Math.max(
    stats.dataUpdatedAt,
    health.dataUpdatedAt,
    parsers.dataUpdatedAt,
    queues.dataUpdatedAt,
  );

  function changePeriod(next: AdminPeriod) {
    setParams(
      (prev) => {
        const out = new URLSearchParams(prev);
        if (next === "24h") out.delete("period");
        else out.set("period", next);
        return out;
      },
      { replace: true },
    );
  }

  return (
    <>
      <AdminPageHeader
        title="Обзор системы"
        updatedAt={updatedAt}
        actions={
          <SegmentedControl
            label="Период"
            options={ADMIN_PERIODS}
            value={period}
            onChange={changePeriod}
          />
        }
      />

      {stats.isPending && <Skeleton className="h-28 w-full" />}
      {stats.isError && !stats.data && (
        <ErrorBlock onRetry={() => void stats.refetch()} />
      )}
      {stats.data && <StatsKpis stats={stats.data} period={period} />}

      <AdminSection title="Состояние сервисов">
        {health.isPending && <Skeleton className="h-24 w-full" />}
        {health.isError && !health.data && (
          <ErrorBlock onRetry={() => void health.refetch()} />
        )}
        {health.data && (
          <HealthGrid health={health.data} queues={queues.data} />
        )}
      </AdminSection>

      <AdminSection
        title="Парсеры магазинов"
        actions={
          <Button
            variant="secondary"
            size="sm"
            loading={actions.runningAll}
            disabled={!parsers.data}
            onClick={() => parsers.data && void actions.runAll(parsers.data)}
          >
            Запустить все
          </Button>
        }
      >
        {parsers.isPending && <Skeleton className="h-40 w-full" />}
        {parsers.isError && !parsers.data && (
          <ErrorBlock onRetry={() => void parsers.refetch()} />
        )}
        {parsers.data && (
          <ParsersTable
            parsers={parsers.data}
            variant="compact"
            actions={actions}
          />
        )}
      </AdminSection>

      <div className="flex flex-wrap gap-[22px]">
        <ActivitySection />
        <AdminSection title="Очереди RabbitMQ" className="flex-[1_1_300px]">
          {queues.isPending && <Skeleton className="h-40 w-full" />}
          {queues.isError && !queues.data && (
            <ErrorBlock onRetry={() => void queues.refetch()} />
          )}
          {queues.data && <QueuesSummary queues={queues.data} />}
        </AdminSection>
      </div>

      <div className="flex flex-wrap gap-[22px]">
        <RecentEvents />
        <TopQueries period={period} />
      </div>
    </>
  );
}
