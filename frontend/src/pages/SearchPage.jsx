import { useState } from 'react'
import Navbar from '../components/Navbar'
import BookCard from '../components/BookCard'
import { searchBooks } from '../api/books'

export default function SearchPage() {
  const [query, setQuery] = useState('')
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [searched, setSearched] = useState(false)

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!query.trim()) return
    setLoading(true)
    setError('')
    setSearched(true)
    try {
      const data = await searchBooks(query.trim())
      setBooks(data)
    } catch {
      setError('Не удалось выполнить поиск. Попробуйте позже.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-5xl mx-auto px-4 py-12">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Сравните цены на книги
          </h1>
          <p className="text-gray-500">
            Ищем лучшие предложения на Kaspi, Ozon и других площадках
          </p>
        </div>

        <form onSubmit={handleSearch} className="flex gap-3 mb-10">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Название книги или автор..."
            className="flex-1 px-4 py-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white text-sm"
          />
          <button
            type="submit"
            disabled={loading || !query.trim()}
            className="px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-medium rounded-xl transition-colors text-sm"
          >
            {loading ? 'Ищем...' : 'Найти'}
          </button>
        </form>

        {error && (
          <p className="text-center text-red-500 text-sm mb-6">{error}</p>
        )}

        {loading && (
          <div className="text-center py-16 text-gray-400">
            <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <p className="text-sm">Собираем предложения...</p>
          </div>
        )}

        {!loading && searched && books.length === 0 && (
          <div className="text-center py-16 text-gray-400">
            <p className="text-4xl mb-3">📚</p>
            <p className="text-sm">Ничего не найдено. Попробуйте другой запрос.</p>
          </div>
        )}

        {!loading && books.length > 0 && (
          <>
            <p className="text-sm text-gray-500 mb-4">
              Найдено: <span className="font-medium text-gray-700">{books.length}</span> книг
            </p>
            <div className="grid gap-3">
              {books.map((book) => (
                <BookCard key={book.id} book={book} />
              ))}
            </div>
          </>
        )}

        {!searched && (
          <div className="text-center py-16 text-gray-300">
            <p className="text-5xl mb-4">🔍</p>
            <p className="text-sm">Введите название книги или имя автора</p>
          </div>
        )}
      </div>
    </div>
  )
}
