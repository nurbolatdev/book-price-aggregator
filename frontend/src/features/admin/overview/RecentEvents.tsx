import { ErrorBlock } from "../../../components/ui/ErrorBlock";
import { Skeleton } from "../../../components/ui/Skeleton";
import { AdminSection } from "../AdminSection";
import { formatAdminTime } from "../adminFormat";
import { StatusBadge } from "../StatusBadge";
import { levelTone } from "../status";
import { useRecentEvents } from "./useOverviewQueries";

export function RecentEvents() {
  const { data, isPending, isError, refetch } = useRecentEvents(5);
  const items = data?.content ?? [];
  const now = new Date();
  return (
    <AdminSection title="Последние события" className="flex-[1_1_380px]">
      {isPending && <Skeleton className="h-40 w-full" />}
      {isError && <ErrorBlock onRetry={() => void refetch()} />}
      {data && items.length === 0 && (
        <p className="text-sm text-muted">Событий пока нет</p>
      )}
      {items.length > 0 && (
        <ul className="flex list-none flex-col">
          {items.map((e) => (
            <li
              key={e.id}
              className="flex flex-wrap items-center gap-3 border-t border-divider py-2.5 first:border-t-0"
            >
              <span className="w-[104px] shrink-0 whitespace-nowrap text-sm text-muted">
                {e.createdAt ? formatAdminTime(e.createdAt, now) : "—"}
              </span>
              <StatusBadge tone={levelTone(e.level)}>{e.level}</StatusBadge>
              <span className="min-w-0 flex-[1_1_200px] text-sm">
                {e.message}
              </span>
            </li>
          ))}
        </ul>
      )}
    </AdminSection>
  );
}
