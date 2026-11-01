import type { EditionDetailResponse } from '../../api/types'
import { Cover } from '../../components/ui/Cover'
import { BestPriceCard } from './BestPriceCard'
import { EditionBreadcrumbs } from './EditionBreadcrumbs'
import { EditionDescription } from './EditionDescription'
import { EditionFacts } from './EditionFacts'
import { OffersTable } from './OffersTable'

interface EditionDesktopProps {
  edition: EditionDetailResponse
  editionId: number
}

export function EditionDesktop({ edition, editionId }: EditionDesktopProps) {
  const { book } = edition
  return (
    <>
      <EditionBreadcrumbs title={book?.title ?? ''} />
      <section className="flex flex-wrap items-start gap-8">
        <Cover
          editionId={editionId}
          title={book?.title ?? ''}
          coverUrl={edition.coverUrl}
          size="detail"
          className="h-[380px] w-[260px] max-w-full rounded-[14px]"
        />
        <div className="flex min-w-0 flex-[1_1_340px] flex-col gap-3.5">
          <h1 className="font-display text-4xl font-bold leading-[1.15] text-ink">{book?.title}</h1>
          {book?.author && <p className="text-lg text-muted">{book.author}</p>}
          <EditionFacts edition={edition} />
          <EditionDescription text={book?.description} />
        </div>
        <BestPriceCard
          edition={edition}
          editionId={editionId}
          className="max-w-[360px] flex-[1_1_300px]"
        />
      </section>
      <OffersTable edition={edition} />
    </>
  )
}
