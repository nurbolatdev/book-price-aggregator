import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import BookCard from '../components/BookCard'
import { getFavorites, removeFavorite } from '../api/favorites'

export default function FavoritesPage() {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getFavorites()
      .then(setBooks)
      .finally(() => setLoading(false))
  }, [])

  const handleRemove = async (bookId) => {
    await removeFavorite(bookId)
    setBooks((prev) => prev.filter((b) => b.id !== bookId))
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Избранное</h1>
          {books.length > 0 && (
            <span className="text-sm text-gray-400">{books.length} книг</span>
          )}
        </div>

        {loading && (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && books.length === 0 && (
          <div className="text-center py-20 text-gray-400">
            <p className="text-5xl mb-4">🤍</p>
            <p className="text-sm mb-4">Вы ещё не добавили книги в избранное</p>
            <Link
              to="/"
              className="text-sm text-blue-600 hover:underline"
            >
              Найти книги →
            </Link>
          </div>
        )}

        {!loading && books.length > 0 && (
          <div className="grid gap-3">
            {books.map((book) => (
              <div key={book.id} className="relative group">
                <BookCard book={book} />
                <button
                  onClick={() => handleRemove(book.id)}
                  className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 text-xs text-gray-400 hover:text-red-500 bg-white border border-gray-100 px-2 py-1 rounded-lg transition-all"
                >
                  Удалить
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
