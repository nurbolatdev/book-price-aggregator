import { Link } from 'react-router-dom'

export default function BookCard({ book }) {
  const hasCover = Boolean(book.coverUrl)

  return (
    <Link
      to={`/books/${book.id}`}
      className="group flex gap-4 bg-white border border-gray-100 rounded-xl p-4 hover:shadow-md hover:border-blue-100 transition-all"
    >
      <div className="flex-shrink-0 w-16 h-24 rounded-lg overflow-hidden bg-gray-100">
        {hasCover ? (
          <img
            src={book.coverUrl}
            alt={book.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-300 text-2xl">
            📖
          </div>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <h3 className="font-semibold text-gray-900 text-sm leading-snug line-clamp-2 group-hover:text-blue-600 transition-colors">
          {book.title}
        </h3>
        {book.author && (
          <p className="text-xs text-gray-500 mt-1 truncate">{book.author}</p>
        )}

        <div className="mt-3 flex items-end justify-between">
          {book.bestPrice ? (
            <div>
              <p className="text-xs text-gray-400">от</p>
              <p className="text-lg font-bold text-blue-600">
                {Number(book.bestPrice).toLocaleString('ru-RU')} ₸
              </p>
            </div>
          ) : (
            <p className="text-sm text-gray-400">Нет предложений</p>
          )}

          {book.offers?.length > 0 && (
            <span className="text-xs text-gray-400 bg-gray-50 px-2 py-1 rounded-md">
              {book.offers.length}{' '}
              {book.offers.length === 1 ? 'магазин' : 'магазина'}
            </span>
          )}
        </div>
      </div>
    </Link>
  )
}
