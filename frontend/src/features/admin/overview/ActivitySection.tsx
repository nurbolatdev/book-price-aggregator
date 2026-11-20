import { lazy, Suspense } from "react";
import { ErrorBlock } from "../../../components/ui/ErrorBlock";
import { Skeleton } from "../../../components/ui/Skeleton";
import { AdminSection } from "../AdminSection";
import { activityColors } from "./chartColors";
import { useAdminActivity } from "./useOverviewQueries";

const ActivityChart = lazy(() => import("./ActivityChart"));

function Legend({ color, label }: { color: string; label: string }) {
  return (
    <span className="flex items-center gap-1.5 text-xs text-ink-soft">
      <span
        aria-hidden="true"
        className="h-3 w-3 rounded-[3px]"
        style={{ backgroundColor: color }}
      />
      {label}
    </span>
  );
}

export function ActivitySection() {
  const { data, isPending, isError, refetch } = useAdminActivity(14);
  return (
    <AdminSection title="Активность за 14 дней" className="flex-[1_1_380px]">
      <div className="flex gap-4">
        <Legend color={activityColors.searches} label="Поиски" />
        <Legend color={activityColors.beacons} label="Новые маячки" />
      </div>
      {isPending && <Skeleton className="h-[220px] w-full" />}
      {isError && <ErrorBlock onRetry={() => void refetch()} />}
      {data && (
        <Suspense fallback={<Skeleton className="h-[220px] w-full" />}>
          <ActivityChart days={data} />
        </Suspense>
      )}
    </AdminSection>
  );
}
