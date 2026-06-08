import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <nav className="bg-white border-b border-gray-100 sticky top-0 z-50">
      <div className="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
        <Link to="/" className="text-lg font-bold text-blue-600 tracking-tight">
          BookPrice
        </Link>

        <div className="flex items-center gap-4">
          {user ? (
            <>
              <Link
                to="/favorites"
                className="text-sm text-gray-600 hover:text-blue-600 transition-colors"
              >
                Избранное
              </Link>
              <span className="text-sm text-gray-400">|</span>
              <span className="text-sm text-gray-600">{user.name || user.email}</span>
              <button
                onClick={handleLogout}
                className="text-sm text-gray-500 hover:text-red-500 transition-colors"
              >
                Выйти
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm text-gray-600 hover:text-blue-600 transition-colors"
              >
                Войти
              </Link>
              <Link
                to="/register"
                className="text-sm bg-blue-600 hover:bg-blue-700 text-white px-4 py-1.5 rounded-lg transition-colors"
              >
                Регистрация
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
