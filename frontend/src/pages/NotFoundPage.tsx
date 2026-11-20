import { NotFound } from '../components/ui/NotFound'
import { usePageMeta } from '../lib/usePageMeta'

export default function NotFoundPage() {
  usePageMeta({
    title: 'Страница не найдена | KitapRadar',
    description: 'Такой страницы нет. Вернитесь на главную или найдите книгу в каталоге.',
  })
  return (
    <div className="mx-auto w-full max-w-page px-4 py-10 md:px-6">
      <NotFound />
    </div>
  )
}
