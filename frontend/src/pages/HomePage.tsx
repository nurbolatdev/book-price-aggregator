import { Link } from 'react-router-dom'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { buttonClasses } from '../components/ui/Button'
import { DropGrid, DropGridSkeleton } from '../features/home/DropGrid'
import { HowItWorks } from '../features/home/HowItWorks'
import { StoresLine } from '../features/home/StoresLine'
import { useDrops } from '../features/home/useDrops'
import { PopularChips } from '../features/search/PopularChips'
import { SearchBox } from '../features/search/SearchBox'
import { focusRing } from '../components/ui/focusRing'
import { useAuthStore } from '../stores/auth'

function WeeklyDrops() {
  const { data, isLoading, isError, refetch } = useDrops(8)
  return (
    <section className="mx-auto w-full max-w-page px-4 pb-10 md:px-6 md:pb-14">
      <div className="mb-4 flex flex-nowrap items-start justify-between gap-3 md:mb-6">
        <h2 className="min-w-0 font-display text-xl font-bold md:text-[26px] md:font-medium">Сильнее всего подешевели за неделю</h2>
        <Link to="/deals" className={'font-semibold text-link hover:text-link-hover ' + focusRing}>
          Все скидки
        </Link>
      </div>
      {isLoading ? (
        <DropGridSkeleton />
      ) : isError ? (
        <ErrorBlock message="Не удалось загрузить подборку." onRetry={() => void refetch()} />
      ) : (
        <DropGrid drops={data ?? []} phoneLimit={4} />
      )}
    </section>
  )
}

export default function HomePage() {
  const anonymous = useAuthStore((s) => s.status === 'anonymous')
  return (
    <>
      <section className="mx-auto flex w-full max-w-page flex-col gap-3.5 px-4 pb-5 pt-7 md:gap-7 md:px-6 md:pb-12 md:pt-[72px]">
        <h1 className="max-w-[860px] font-display text-[30px] font-bold leading-[1.12] tracking-tight md:text-[52px] md:leading-[1.1]">
          Одна книга — все цены Казахстана
        </h1>
        <p className="max-w-[720px] text-[15px] leading-normal text-muted md:text-xl">
          Сравниваем цены книжных магазинов Казахстана за секунду. Поставьте маячок — напишем на почту, когда книга
          подешевеет.
        </p>
        <SearchBox variant="hero" />
        <PopularChips scrollOnPhone />
        <StoresLine />
      </section>
      <HowItWorks />
      <WeeklyDrops />
      {anonymous && (
        <section className="mx-auto mb-10 w-full max-w-page px-4 md:mb-16 md:px-6">
          <div className="flex flex-wrap items-center justify-between gap-6 rounded-[20px] bg-ink p-6 text-white md:p-10">
            <div className="flex max-w-[640px] flex-col gap-2">
              <h2 className="font-display text-xl font-bold md:text-[26px] md:font-medium">Есть список «хочу прочитать»?</h2>
              <p className="text-[15px] leading-normal text-mist md:text-[17px]">
                Создайте аккаунт и ставьте маячки на книги из своего списка. Письмо придёт только когда цена
                действительно снизится.
              </p>
            </div>
            <Link
              to="/register"
              className={buttonClasses({ variant: 'light', size: 'lg', className: 'max-md:w-full' })}
            >
              Создать аккаунт
            </Link>
          </div>
        </section>
      )}
    </>
  )
}
