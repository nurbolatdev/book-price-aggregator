import type { EditionDetailResponse } from '../../api/types'
import { formatIsbn13, languageName } from '../../lib/format'

export function EditionFacts({ edition }: { edition: EditionDetailResponse }) {
  const rows: [string, string][] = []
  if (edition.isbn13) rows.push(['ISBN', formatIsbn13(edition.isbn13)])
  if (edition.pages) rows.push(['Страниц', String(edition.pages)])
  if (edition.language) rows.push(['Язык', languageName(edition.language)])
  if (rows.length === 0) return null
  return (
    <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm">
      {rows.map(([term, value]) => (
        <div key={term} className="contents">
          <dt className="text-muted">{term}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
