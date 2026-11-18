import { useEffect } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import type { EventLevel } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { EmptyState } from "../../components/ui/EmptyState";
import { ErrorBlock } from "../../components/ui/ErrorBlock";
import { Skeleton } from "../../components/ui/Skeleton";
import { AdminPageHeader } from "../../features/admin/AdminPageHeader";
import { AdminSection } from "../../features/admin/AdminSection";
import { EventsList } from "../../features/admin/events/EventsList";
import { useEventsJournal } from "../../features/admin/events/useEventsJournal";
import {
  ZeroResults,
  type ZeroPeriod,
} from "../../features/admin/events/ZeroResults";
import { FilterChips } from "../../features/admin/FilterChips";

type LevelFilter = "all" | EventLevel;

const LEVEL_OPTIONS: { value: LevelFilter; label: string }[] = [
  { value: "all", label: "Все" },
  { value: "INFO", label: "INFO" },
  { value: "WARN", label: "WARN" },
  { value: "ERROR", label: "ERROR" },
];

function parseLevel(raw: string | null): LevelFilter {
  return raw === "INFO" || raw === "WARN" || raw === "ERROR" ? raw : "all";
}

function parseZeroPeriod(raw: string | null): ZeroPeriod {
  return raw === "24h" ? "24h" : "7d";
}

export default function EventsPage() {
  const [params, setParams] = useSearchParams();
  const { hash } = useLocation();
  const levelFilter = parseLevel(params.get("level"));
  const zeroPeriod = parseZeroPeriod(params.get("zeroPeriod"));
  const level = levelFilter === "all" ? undefined : levelFilter;

  const journal = useEventsJournal(level);
  const events = journal.data?.pages.flatMap((p) => p.content ?? []) ?? [];
  const loaded = journal.data !== undefined;

  useEffect(() => {
    if (hash === "#zero" && loaded) {
      document.getElementById("zero")?.scrollIntoView?.();
    }
  }, [hash, loaded]);

  function setParam(key: string, value: string | null) {
    setParams((prev) => {
      const out = new URLSearchParams(prev);
      if (value === null) out.delete(key);
      else out.set(key, value);
      return out;
    });
  }

  return (
    <>
      <AdminPageHeader
        title="Журнал событий"
        subtitle="system_events · новые сверху"
        actions={
          <Button
            variant="secondary"
            size="sm"
            loading={
              journal.isFetching &&
              !journal.isPending &&
              !journal.isFetchingNextPage
            }
            onClick={() => void journal.refetch()}
          >
            Обновить
          </Button>
        }
      />
      <div className="flex flex-wrap gap-[22px]">
        <AdminSection
          title="События системы"
          className="min-w-0 flex-[999_1_420px]"
          actions={
            <FilterChips
              label="Уровень"
              options={LEVEL_OPTIONS}
              value={levelFilter}
              onChange={(v) => setParam("level", v === "all" ? null : v)}
            />
          }
        >
          {journal.isPending && <Skeleton className="h-72 w-full" />}
          {journal.isError && !journal.data && (
            <ErrorBlock onRetry={() => void journal.refetch()} />
          )}
          {loaded && events.length === 0 && <EmptyState title="Событий нет" />}
          {events.length > 0 && (
            <EventsList
              key={levelFilter}
              events={events}
              hasNextPage={journal.hasNextPage}
              isFetchingNextPage={journal.isFetchingNextPage}
              fetchNextPage={() => void journal.fetchNextPage()}
            />
          )}
        </AdminSection>
        <ZeroResults
          period={zeroPeriod}
          onPeriodChange={(p) => setParam("zeroPeriod", p === "7d" ? null : p)}
        />
      </div>
    </>
  );
}
