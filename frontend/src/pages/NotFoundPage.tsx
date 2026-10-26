import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="font-display text-2xl font-semibold">Страница не найдена</h1>
      <Link
        to="/"
        className="mt-4 inline-flex min-h-touch items-center text-link underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-link"
      >
        На главную
      </Link>
    </div>
  )
}
