import { useParams } from 'react-router-dom'
import { isNotFound } from '../api/errors'
import type { EditionDetailResponse } from '../api/types'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { NotFound } from '../components/ui/NotFound'
import { EditionDesktop } from '../features/edition/EditionDesktop'
import { EditionMobile } from '../features/edition/EditionMobile'
import { EditionMobileHeader } from '../features/edition/EditionMobileHeader'
import { EditionSkeleton } from '../features/edition/EditionSkeleton'
import { storeNameOf } from '../features/edition/offers'
import { useBeaconParam } from '../features/edition/useBeaconParam'
import { useEdition } from '../features/edition/useEdition'
import { formatPrice, pluralRu } from '../lib/format'
import { useMediaQuery } from '../lib/useMediaQuery'
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

function EditionContent({
  edition,
  id,
  desktop,
}: {
  edition: EditionDetailResponse
  id: number
  desktop: boolean
}) {
  usePageMeta(metaFor(edition))
  return desktop ? (
    <EditionDesktop edition={edition} editionId={id} />
  ) : (
    <EditionMobile edition={edition} editionId={id} />
  )
}

function PhoneShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-col">
      <EditionMobileHeader />
      <div className="px-4 py-4">{children}</div>
    </div>
  )
}

export default function EditionPage() {
  const id = parseId(useParams().id)
  const desktop = useMediaQuery('(min-width: 768px)')
  const { data, error, isPending, refetch } = useEdition(id)
  useBeaconParam(id)

  if (data) {
    const content = <EditionContent key={id} edition={data} id={id} desktop={desktop} />
    return desktop ? <Container>{content}</Container> : content
  }

  let state: React.ReactNode
  if (id === 0 || isNotFound(error)) state = <NotFound />
  else if (isPending) state = <EditionSkeleton variant={desktop ? 'desktop' : 'mobile'} />
  else state = <ErrorBlock message="Не удалось загрузить издание." onRetry={() => void refetch()} />
  return desktop ? <Container>{state}</Container> : <PhoneShell>{state}</PhoneShell>
}
