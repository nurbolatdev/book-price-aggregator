import type { EditionDetailResponse } from '../../api/types'
import { Cover } from '../../components/ui/Cover'
import { pluralRu } from '../../lib/format'
import { editionLabel } from './editionLabel'
import { EditionDescription } from './EditionDescription'
import { EditionFacts } from './EditionFacts'
import { EditionMobileHeader } from './EditionMobileHeader'
import { EditionSwitcher } from './EditionSwitcher'
import { MobileBuyBar } from './MobileBuyBar'
import { OffersList } from './OffersList'
import { PriceHistory } from './PriceHistory'
import { PriceVerdict } from './PriceVerdict'
import { SimilarBooks } from './SimilarBooks'

interface EditionMobileProps {
  edition: EditionDetailResponse
  editionId: number
}

export function EditionMobile({ edition, editionId }: EditionMobileProps) {
  const { book } = edition
  const n = edition.offers?.length ?? 0
  const label = editionLabel(edition)
  return (
    <div className="flex flex-col">
      <EditionMobileHeader title={book?.title} shareable />
      <div className="flex flex-col gap-3.5 px-4 py-4">
        <section className="flex gap-3.5">
          <Cover
            editionId={editionId}
            title={book?.title ?? ''}
            coverUrl={edition.coverUrl}
            size="detail"
            className="h-[140px] w-24 rounded-lg"
          />
          <div className="flex min-w-0 flex-col gap-1.5">
            <h1 className="font-display text-[19px] font-medium leading-[1.2] text-ink">{book?.title}</h1>
            {book?.author && <p className="text-sm text-muted">{book.author}</p>}
            {label && <p className="text-[13px] text-muted">{label}</p>}
            <PriceVerdict verdict={edition.priceVerdict} size="sm" />
          </div>
        </section>
        <section aria-labelledby="offers-title" className="flex flex-col gap-3.5">
          <h2 id="offers-title" className="mt-1 font-sans text-base font-bold">
            Цены в {n} {pluralRu(n, ['магазине', 'магазинах', 'магазинах'])}
          </h2>
          <OffersList edition={edition} />
        </section>
        <PriceHistory editionId={editionId} />
        <section aria-labelledby="details-title" className="flex flex-col gap-3.5">
          <h2 id="details-title" className="font-sans text-base font-bold">
            Подробности
          </h2>
          <EditionSwitcher edition={edition} editionId={editionId} />
          <EditionFacts edition={edition} />
          <EditionDescription text={book?.description} />
        </section>
        <SimilarBooks editionId={editionId} />
      </div>
      <MobileBuyBar edition={edition} editionId={editionId} />
    </div>
  )
}
