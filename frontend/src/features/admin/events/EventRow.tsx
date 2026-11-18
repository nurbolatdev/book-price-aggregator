import { useId } from "react";
import type { AdminEvent } from "../../../api/types";
import { formatAdminTime } from "../adminFormat";
import { levelTone } from "../status";
import { StatusBadge } from "../StatusBadge";

interface EventRowProps {
  event: AdminEvent;
  expanded: boolean;
  onToggle: () => void;
}

export function EventRow({ event, expanded, onToggle }: EventRowProps) {
  const detailsId = useId();
  const details = event.details ?? {};
  const hasDetails = Object.keys(details).length > 0;
  return (
    <div className="border-t border-divider px-1 py-2.5">
      <button
        type="button"
        aria-expanded={expanded}
        aria-controls={detailsId}
        onClick={onToggle}
        className="flex w-full flex-wrap items-center gap-3 text-left"
      >
        <span className="w-[92px] text-sm text-muted">
          {event.createdAt ? formatAdminTime(event.createdAt, new Date()) : "—"}
        </span>
        <StatusBadge tone={levelTone(event.level)}>{event.level}</StatusBadge>
        <code className="text-[13px] text-ink-soft">{event.source}</code>
        <span className="flex-[1_1_300px] text-sm">{event.message}</span>
      </button>
      {expanded && (
        <pre
          id={detailsId}
          className="mt-2 overflow-x-auto rounded-lg bg-canvas p-3 font-mono text-[13px]"
        >
          {hasDetails ? JSON.stringify(details, null, 2) : "Нет деталей"}
        </pre>
      )}
    </div>
  );
}
