import { Button } from '../../components/ui/Button'

interface PaginationProps {
  page: number
  size: number
  totalElements: number
  totalPages: number
  onChange: (page: number) => void
  caption?: (from: number, to: number, total: number) => string
}

export function Pagination({ page, size, totalElements, totalPages, onChange, caption }: PaginationProps) {
  if (totalElements === 0) return null
  const from = page * size + 1
  const to = Math.min((page + 1) * size, totalElements)
  const text = caption ? caption(from, to, totalElements) : `Показаны ${from}–${to} из ${totalElements}`
  return (
    <div className="flex flex-wrap items-center justify-between gap-2.5 text-sm text-muted">
      <span>{text}</span>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
          Назад
        </Button>
        <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
          Вперёд
        </Button>
      </div>
    </div>
  )
}
