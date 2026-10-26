import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './app/App'
import { restoreSession } from './api/auth'

const root = document.getElementById('root')
if (!root) {
  throw new Error('#root not found')
}

void restoreSession()

createRoot(root).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
