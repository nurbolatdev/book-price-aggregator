import { TrendingDown } from "lucide-react";
import { Link } from "react-router-dom";
import { buttonClasses } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { ErrorBlock } from "../components/ui/ErrorBlock";
import { DropGrid, DropGridSkeleton } from "../features/home/DropGrid";
import { useDrops } from "../features/home/useDrops";

export default function DealsPage() {
  const { data, isPending, isError, refetch } = useDrops(48);

  return (
    <div className="mx-auto flex w-full max-w-page flex-col gap-6 px-4 pb-14 pt-7 md:px-6">
      <div className="flex flex-col gap-1">
        <h1 className="font-display text-[26px] font-medium">Скидки недели</h1>
        <p className="text-[15px] text-muted">
          Сильнее всего подешевели за 7 дней
        </p>
      </div>
      {isPending ? (
        <DropGridSkeleton count={8} />
      ) : isError ? (
        <ErrorBlock
          message="Не удалось загрузить подборку."
          onRetry={() => void refetch()}
        />
      ) : data.length === 0 ? (
        <EmptyState
          icon={<TrendingDown size={28} aria-hidden="true" />}
          title="За неделю цены не снижались"
          text="Загляните позже или найдите нужную книгу в каталоге."
          action={
            <Link
              to="/catalog"
              className={buttonClasses({ variant: "dark", size: "lg" })}
            >
              Найти книгу
            </Link>
          }
        />
      ) : (
        <DropGrid drops={data} />
      )}
    </div>
  );
}
