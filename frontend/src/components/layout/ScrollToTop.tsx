import { useEffect, useRef } from 'react'
import { useLocation, useNavigationType } from 'react-router-dom'

export function ScrollToTop() {
  const { pathname } = useLocation()
  const navigationType = useNavigationType()
  const previous = useRef(pathname)
  useEffect(() => {
    const changed = previous.current !== pathname
    previous.current = pathname
    if (changed && navigationType !== 'POP') window.scrollTo(0, 0)
  }, [pathname, navigationType])
  return null
}
