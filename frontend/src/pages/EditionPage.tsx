import { useParams } from 'react-router-dom'
import { isNotFound } from '../api/errors'
import type { EditionDetailResponse } from '../api/types'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { NotFound } from '../components/ui/NotFound'
import { EditionDesktop } from '../features/edition/EditionDesktop'
import { EditionSkeleton } from '../features/edition/EditionSkeleton'
import { storeNameOf } from '../features/edition/offers'
import { useBeaconParam } from '../features/edition/useBeaconParam'
import { useEdition } from '../features/edition/useEdition'
import { formatPrice, pluralRu } from '../lib/format'
import { usePageMeta } from '../lib/usePageMeta'

function parseId(raw: string | undefined): number {
  return raw && /^[1-9]\d*$/.test(raw) ? Number(raw) : 0
}

function Container({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-page flex-col gap-6 px-4 pb-16 pt-5 md:px-6">
      {children}
    </div>
  )
}

function metaFor(edition: EditionDetailResponse) {
  const title = edition.book?.title ?? ''
  const author = edition.book?.author ?? ''
  const n = edition.offers?.length ?? 0
  const best = edition.bestPrice
  const description =
    best?.price != null
      ? `${title}, ${author}: от ${formatPrice(best.price)} в ${storeNameOf(edition.offers, best)}. ` +
        `Сравните цены в ${n} ${pluralRu(n, ['магазине', 'магазинах', 'магазинах'])} Казахстана.`
      : `${title}, ${author}: цены в магазинах Казахстана.`
  return {
    title: `${title} — цены в магазинах Казахстана | KitapRadar`,
    description,
  }
}

function EditionContent({ edition, id }: { edition: EditionDetailResponse; id: number }) {
  usePageMeta(metaFor(edition))
  return <EditionDesktop edition={edition} editionId={id} />
}

export default function EditionPage() {
  const id = parseId(useParams().id)
  const { data, error, isPending, refetch } = useEdition(id)
  useBeaconParam(id)

  if (id === 0 || isNotFound(error))
    return (
      <Container>
        <NotFound />
      </Container>
    )
  if (data) {
    return (
      <Container>
        <EditionContent key={id} edition={data} id={id} />
      </Container>
    )
  }
  if (isPending)
    return (
      <Container>
        <EditionSkeleton />
      </Container>
    )
  return (
    <Container>
      <ErrorBlock message="Не удалось загрузить издание." onRetry={() => void refetch()} />
    </Container>
  )
}
