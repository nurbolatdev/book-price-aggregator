import { useVirtualizer } from "@tanstack/react-virtual";
import { useEffect, useRef, useState } from "react";
import type { AdminEvent } from "../../../api/types";
import { EventRow } from "./EventRow";

interface EventsListProps {
  events: AdminEvent[];
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  fetchNextPage: () => void;
}

const PREFETCH_MARGIN = 5;

export function EventsList({
  events,
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
}: EventsListProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [expanded, setExpanded] = useState<Set<number>>(() => new Set());
  const count = events.length + (isFetchingNextPage ? 1 : 0);

  const virtualizer = useVirtualizer({
    count,
    getScrollElement: () => scrollRef.current,
    estimateSize: () => 48,
    overscan: 8,
    initialRect: { width: 800, height: 640 },
  });
  const items = virtualizer.getVirtualItems();
  const lastIndex =
    items.length > 0 ? (items[items.length - 1]?.index ?? 0) : -1;

  useEffect(() => {
    if (
      lastIndex >= events.length - PREFETCH_MARGIN &&
      hasNextPage &&
      !isFetchingNextPage
    ) {
      fetchNextPage();
    }
  }, [
    lastIndex,
    events.length,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  ]);

  function toggle(id: number) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  return (
    <div ref={scrollRef} className="h-[640px] overflow-y-auto">
      <div
        className="relative w-full"
        style={{ height: virtualizer.getTotalSize() }}
      >
        {items.map((item) => {
          const event = events[item.index];
          return (
            <div
              key={event ? event.id : "loading"}
              data-index={item.index}
              ref={virtualizer.measureElement}
              className="absolute left-0 top-0 w-full"
              style={{ transform: `translateY(${item.start}px)` }}
            >
              {event ? (
                <EventRow
                  event={event}
                  expanded={event.id !== undefined && expanded.has(event.id)}
                  onToggle={() => event.id !== undefined && toggle(event.id)}
                />
              ) : (
                <p className="border-t border-divider px-1 py-2.5 text-sm text-muted">
                  Загружаем ещё…
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
