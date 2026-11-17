import { useId } from 'react'
import { Link } from 'react-router-dom'
import { authLinkClasses } from '../auth/authLink'

export function HowItWorksPanel() {
  const headingId = useId()
  return (
    <section
      aria-labelledby={headingId}
      className="flex flex-col gap-2.5 rounded-card border border-line bg-surface p-4 md:gap-3.5 md:p-5"
    >
      <h2 id={headingId} className="font-sans text-[17px] font-bold md:text-lg">
        Как работает маячок
      </h2>
      <p className="text-sm leading-normal text-ink-soft">
        Проверяем цены каждые 6 часов. Письмо приходит о каждом новом минимуме; та же или более высокая цена письма не
        даёт.
      </p>
      <Link to="/profile" className={`${authLinkClasses} text-sm font-semibold`}>
        Все настройки аккаунта
      </Link>
    </section>
  )
}
