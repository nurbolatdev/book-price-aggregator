import { Skeleton } from "../../components/ui/Skeleton";
import { CHART_BOX } from "./PriceHistory";

export function EditionBarSkeleton() {
  return (
    <div className="sticky bottom-0 z-10 flex gap-2.5 border-t border-line bg-surface px-4 pt-3 pb-[calc(20px+env(safe-area-inset-bottom))]">
      <Skeleton className="h-[52px] flex-1" />
      <Skeleton className="h-[52px] flex-[1.4]" />
    </div>
  );
}

export function EditionSkeleton({
  variant = "desktop",
}: {
  variant?: "desktop" | "mobile";
}) {
  if (variant === "mobile") {
    return (
      <div
        role="status"
        aria-label="Загружаем издание"
        className="flex flex-col"
      >
        <div className="flex flex-col gap-3.5">
          <div className="flex gap-3.5">
            <Skeleton className="h-[140px] w-24 shrink-0 rounded-lg" />
            <div className="flex min-w-0 flex-1 flex-col gap-1.5">
              <Skeleton className="h-6 w-4/5" />
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-6 w-1/3" />
            </div>
          </div>
          <Skeleton className="mt-1 h-5 w-40" />
          <div className="flex flex-col gap-2">
            <Skeleton className="h-[66px] w-full rounded-xl" />
            <Skeleton className="h-[66px] w-full rounded-xl" />
            <Skeleton className="h-[66px] w-full rounded-xl" />
          </div>
          <div className="flex flex-col gap-3.5 rounded-card border border-line bg-surface p-[22px]">
            <Skeleton className="h-6 w-1/2" />
            <Skeleton className={CHART_BOX} />
          </div>
        </div>
      </div>
    );
  }
  return (
    <div
      role="status"
      aria-label="Загружаем издание"
      className="flex flex-col gap-6"
    >
      <Skeleton className="h-5 w-64 max-w-full" />
      <div className="flex flex-wrap items-start gap-8">
        <Skeleton className="h-[380px] w-[260px] max-w-full rounded-[14px]" />
        <div className="flex min-w-0 flex-[1_1_340px] flex-col gap-3.5">
          <Skeleton className="h-10 w-4/5" />
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
        <Skeleton className="h-[300px] max-w-[406px] flex-[1_1_346px] rounded-card" />
      </div>
      <Skeleton className="h-[300px] w-full rounded-card" />
      <div className="flex flex-col gap-4 rounded-card border border-line bg-surface p-[22px]">
        <Skeleton className="h-7 w-64 max-w-full" />
        <Skeleton className={CHART_BOX} />
      </div>
    </div>
  );
}
