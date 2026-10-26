import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './app/App'
import { restoreSession } from './api/auth'
import { AppProviders } from './app/AppProviders'
import { ErrorBoundary } from './components/ErrorBoundary'

const root = document.getElementById('root')
if (!root) {
  throw new Error('#root not found')
}

void restoreSession()

createRoot(root).render(
  <StrictMode>
    <ErrorBoundary
      fallback={() => (
        <div className="mx-auto max-w-6xl px-4 py-10">
          <p className="mb-4">Что-то пошло не так</p>
          <button
            type="button"
            onClick={() => location.reload()}
            className="min-h-touch rounded-control border border-line bg-surface px-4"
          >
            Обновить страницу
          </button>
        </div>
      )}
    >
      <AppProviders>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AppProviders>
    </ErrorBoundary>
  </StrictMode>,
)
