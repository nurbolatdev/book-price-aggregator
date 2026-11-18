import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { queryKeys } from '../../../api/queryKeys'
import type { ReviewOffer } from '../../../api/types'
import { fetchSearchPage } from '../../../api/search'
import { Button } from '../../../components/ui/Button'
import { ErrorBlock } from '../../../components/ui/ErrorBlock'
import { ExternalLink } from '../../../components/ui/ExternalLink'
import { Input } from '../../../components/ui/Input'
import { Skeleton } from '../../../components/ui/Skeleton'
import { formatPrice } from '../../../lib/format'
import { useStores } from '../../search/useStores'
import { formatFound } from '../adminFormat'
import { EditionVariants } from './EditionVariants'

interface OfferPanelProps {
  offer: ReviewOffer
  pending: boolean
  onMatch: (editionId: number) => void
  onReject: () => void
}

/** Rendered with `key={offer.id}`, so the search text and the choice reset per offer. */
export function OfferPanel({ offer, pending, onMatch, onReject }: OfferPanelProps) {
  const { storeName } = useStores()
  const initial = (offer.title ?? '').slice(0, 200)
  const [text, setText] = useState(initial)
  const [q, setQ] = useState(initial)
  const [editionId, setEditionId] = useState<number | null>(null)

  const search = useQuery({
    queryKey: queryKeys.admin.editionSearch(q),
    queryFn: ({ signal }) => fetchSearchPage({ q }, 0, signal),
    enabled: q.length >= 2,
    select: (page) => (page.content ?? []).slice(0, 5),
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    setQ(text.trim())
    setEditionId(null)
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-[13px] text-muted">
        {storeName(offer.store ?? '')}
        {offer.firstSeenAt ? ` · ${formatFound(offer.firstSeenAt, new Date())}` : ''}
        {offer.url && (
          <>
            {' · '}
            <ExternalLink href={offer.url} className="text-accent underline">
              открыть на сайте магазина
            </ExternalLink>
          </>
        )}
      </p>
      <h2 className="font-sans text-xl font-bold">{offer.title}</h2>
      <dl className="grid grid-cols-2 gap-3 text-sm">
        <div>
          <dt className="text-muted">Цена</dt>
          <dd className="font-bold">{offer.price !== undefined ? formatPrice(offer.price) : '—'}</dd>
        </div>
        <div>
          <dt className="text-muted">Наличие</dt>
          <dd>{offer.inStock ? 'в наличии' : 'нет в наличии'}</dd>
        </div>
        <div>
          <dt className="text-muted">ID у магазина</dt>
          <dd>
            <code>{offer.externalId}</code>
          </dd>
        </div>
      </dl>

      <form role="search" onSubmit={submit} className="flex gap-2">
        <label htmlFor="edition-search" className="sr-only">
          Найти издание
        </label>
        <Input
          id="edition-search"
          type="search"
          value={text}
          maxLength={200}
          onChange={(e) => setText(e.target.value)}
        />
        <Button type="submit" variant="secondary" size="sm" className="shrink-0">
          Найти издание
        </Button>
      </form>

      {search.isFetching && !search.data && <Skeleton className="h-40 w-full" />}
      {search.isError && <ErrorBlock onRetry={() => void search.refetch()} />}
      {search.data && (
        <EditionVariants items={search.data} value={editionId} onChange={setEditionId} />
      )}

      <div className="flex flex-wrap gap-2.5">
        <Button
          variant="dark"
          size="lg"
          disabled={editionId === null || pending}
          loading={pending}
          onClick={() => editionId !== null && onMatch(editionId)}
        >
          Привязать к изданию
        </Button>
        <Button variant="outline-danger" size="lg" disabled={pending} onClick={onReject}>
          Отклонить: не книга / мусор
        </Button>
      </div>
    </div>
  )
}
