import type { QueueStats } from "../../../api/types";
import { formatNumber } from "../../../lib/format";
import { perMinute } from "../adminFormat";
import { queueCaption } from "../queues/queueCaption";

export function QueuesSummary({ queues }: { queues: QueueStats[] }) {
  return (
    <ul className="flex list-none flex-col">
      {queues.map((q) => {
        const dead = q.name === "kr.dead-letter";
        const caption = queueCaption(q.name);
        return (
          <li
            key={q.name}
            className="flex flex-wrap items-center justify-between gap-2 border-t border-divider py-2.5 first:border-t-0"
          >
            <div className="flex flex-col">
              <code className="text-[13px] font-semibold">{q.name}</code>
              {caption && <span className="text-xs text-muted">{caption}</span>}
            </div>
            <div className="flex flex-col items-end text-sm">
              <span
                className={
                  dead && (q.ready ?? 0) > 0
                    ? "font-semibold text-danger"
                    : "font-semibold"
                }
              >
                {formatNumber(q.ready ?? 0)} в очереди
              </span>
              <span className="text-xs text-muted">
                {dead
                  ? "требуют внимания"
                  : `${perMinute(q.ackRate ?? 0)}/мин обработано`}
              </span>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
