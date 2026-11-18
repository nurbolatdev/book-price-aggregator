import { Link } from "react-router-dom";
import type { AdminPeriod } from "../../../api/types";
import { ErrorBlock } from "../../../components/ui/ErrorBlock";
import { Skeleton } from "../../../components/ui/Skeleton";
import { formatNumber } from "../../../lib/format";
import { AdminSection } from "../AdminSection";
import { periodLabel } from "../adminFormat";
import { useTopQueries } from "./useOverviewQueries";

export function TopQueries({ period }: { period: AdminPeriod }) {
  const { data, isPending, isError, refetch } = useTopQueries(period);
  const items = (data ?? []).slice(0, 5);
  return (
    <AdminSection
      title={`Топ запросов за ${periodLabel(period)}`}
      className="flex-[1_1_300px]"
    >
      {isPending && <Skeleton className="h-32 w-full" />}
      {isError && <ErrorBlock onRetry={() => void refetch()} />}
      {data && items.length === 0 && (
        <p className="text-sm text-muted">Запросов пока нет</p>
      )}
      {items.length > 0 && (
        <ol className="flex list-decimal flex-col gap-1.5 pl-5 text-sm">
          {items.map((q) => (
            <li key={q.query}>
              {q.query} — {formatNumber(q.searches ?? 0)}
            </li>
          ))}
        </ol>
      )}
      <p className="text-[13px] text-muted">
        Запросы без результатов:{" "}
        <Link to="/admin/events#zero" className="text-link underline">
          смотреть список
        </Link>{" "}
        — подсказка, какие книги добавить.
      </p>
    </AdminSection>
  );
}
