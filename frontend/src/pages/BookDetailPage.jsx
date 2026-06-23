import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import OffersTable from '../components/OffersTable'
import { getBook } from '../api/books'
import { addFavorite, removeFavorite, getFavoriteStatus } from '../api/favorites'
import { useAuth } from '../context/AuthContext'

export default function BookDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [book, setBook] = useState(null)
  const [isFavorite, setIsFavorite] = useState(false)
  const [loading, setLoading] = useState(true)
  const [favLoading, setFavLoading] = useState(false)

  useEffect(() => {
    getBook(id)
      .then(setBook)
      .catch(() => navigate('/'))
      .finally(() => setLoading(false))
  }, [id, navigate])

  useEffect(() => {
    if (user && book) {
      getFavoriteStatus(book.id).then((data) => setIsFavorite(data.favorite))
    }
  }, [user, book])

  const toggleFavorite = async () => {
    if (!user) { navigate('/login'); return }
    setFavLoading(true)
    try {
      if (isFavorite) {
        await removeFavorite(book.id)
        setIsFavorite(false)
      } else {
        await addFavorite(book.id)
        setIsFavorite(true)
      }
    } finally {
      setFavLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="flex items-center justify-center py-32">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      </div>
    )
  }

  if (!book) return null

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="max-w-5xl mx-auto px-4 py-8">
        <button
          onClick={() => navigate(-1)}
          className="text-sm text-gray-500 hover:text-blue-600 mb-6 flex items-center gap-1 transition-colors"
        >
          ← Назад
        </button>

        <div className="bg-white rounded-2xl border border-gray-100 p-6 mb-6">
          <div className="flex gap-6">
            <div className="flex-shrink-0 w-32 h-44 rounded-xl overflow-hidden bg-gray-100">
              {book.coverUrl ? (
                <img src={book.coverUrl} alt={book.title} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-4xl text-gray-300">📖</div>
              )}
            </div>

            <div className="flex-1">
              <h1 className="text-2xl font-bold text-gray-900 mb-1">{book.title}</h1>
              {book.author && <p className="text-gray-500 mb-1">{book.author}</p>}
              {book.publisher && <p className="text-xs text-gray-400 mb-4">{book.publisher}</p>}

              <div className="flex items-center gap-4 mb-4">
                {book.bestPrice && (
                  <div>
                    <p className="text-xs text-gray-400">Лучшая цена</p>
                    <p className="text-2xl font-bold text-blue-600">
                      {Number(book.bestPrice).toLocaleString('ru-RU')} ₸
                    </p>
                  </div>
                )}
              </div>

              <button
                onClick={toggleFavorite}
                disabled={favLoading}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors border ${
                  isFavorite
                    ? 'bg-red-50 text-red-500 border-red-200 hover:bg-red-100'
                    : 'bg-gray-50 text-gray-600 border-gray-200 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-200'
                }`}
              >
                {isFavorite ? '♥ В избранном' : '♡ В избранное'}
              </button>
            </div>
          </div>

          {book.description && (
            <p className="mt-4 text-sm text-gray-600 leading-relaxed border-t border-gray-50 pt-4 line-clamp-4">
              {book.description}
            </p>
          )}
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Предложения магазинов</h2>
          <OffersTable offers={book.offers} />
        </div>
      </div>
    </div>
  )
}
