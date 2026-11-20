import { useQuery } from "@tanstack/react-query";
import { fetchZeroResults } from "../../../api/admin";
import { queryKeys } from "../../../api/queryKeys";
import type { AdminPeriod } from "../../../api/types";
import { ErrorBlock } from "../../../components/ui/ErrorBlock";
import { Skeleton } from "../../../components/ui/Skeleton";
import { formatNumber } from "../../../lib/format";
import { AdminSection } from "../AdminSection";
import { SegmentedControl } from "../SegmentedControl";

export type ZeroPeriod = Extract<AdminPeriod, "24h" | "7d">;

interface ZeroResultsProps {
  period: ZeroPeriod;
  onPeriodChange: (period: ZeroPeriod) => void;
}

const OPTIONS: { value: ZeroPeriod; label: string }[] = [
  { value: "24h", label: "24 ч" },
  { value: "7d", label: "7 дней" },
];

export function ZeroResults({ period, onPeriodChange }: ZeroResultsProps) {
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: queryKeys.admin.zeroResults(period),
    queryFn: ({ signal }) => fetchZeroResults(period, signal),
  });
  const items = data ?? [];
  return (
    <AdminSection
      id="zero"
      title="Запросы без результатов"
      className="max-w-[360px] flex-[1_1_280px]"
      actions={
        <SegmentedControl
          label="Период"
          options={OPTIONS}
          value={period}
          onChange={onPeriodChange}
        />
      }
    >
      {isPending && <Skeleton className="h-32 w-full" />}
      {isError && !data && <ErrorBlock onRetry={() => void refetch()} />}
      {data && items.length === 0 && (
        <p className="text-sm text-muted">Таких запросов нет</p>
      )}
      {items.length > 0 && (
        <ol className="flex list-none flex-col text-sm">
          {items.map((q) => (
            <li
              key={q.query}
              className="flex justify-between gap-3 border-t border-divider py-1.5"
            >
              <span>{q.query}</span>
              <span className="font-semibold">
                {formatNumber(q.searches ?? 0)}
              </span>
            </li>
          ))}
        </ol>
      )}
      <p className="text-[13px] text-muted">
        Подсказка, какие книги добавить в обход.
      </p>
    </AdminSection>
  );
}
