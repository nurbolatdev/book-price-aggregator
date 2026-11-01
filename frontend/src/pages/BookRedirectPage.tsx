import { Navigate, useParams } from 'react-router-dom'
import { isNotFound } from '../api/errors'
import { PageSkeleton } from '../components/layout/PageSkeleton'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { NotFound } from '../components/ui/NotFound'
import { pickBestEdition } from '../features/edition/pickBestEdition'
import { useBook } from '../features/edition/useBook'

function parseId(raw: string | undefined): number {
  return raw && /^[1-9]\d*$/.test(raw) ? Number(raw) : 0
}

export default function BookRedirectPage() {
  const id = parseId(useParams().id)
  const { data, error, isPending, refetch } = useBook(id)

  if (id === 0)
    return (
      <Wrapped>
        <NotFound />
      </Wrapped>
    )
  if (isPending) return <PageSkeleton />
  if (error) {
    if (isNotFound(error))
      return (
        <Wrapped>
          <NotFound />
        </Wrapped>
      )
    return (
      <Wrapped>
        <ErrorBlock message="Не удалось загрузить книгу." onRetry={() => void refetch()} />
      </Wrapped>
    )
  }
  const editionId = pickBestEdition(data)
  if (editionId === null)
    return (
      <Wrapped>
        <NotFound />
      </Wrapped>
    )
  return <Navigate replace to={'/editions/' + editionId} />
}

function Wrapped({ children }: { children: React.ReactNode }) {
  return <div className="mx-auto w-full max-w-page px-4 py-10 md:px-6">{children}</div>
}
