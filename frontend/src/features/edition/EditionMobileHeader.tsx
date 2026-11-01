import { ChevronLeft, Share2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { IconButton } from '../../components/ui/IconButton'
import { toast } from '../../stores/toast'

interface EditionMobileHeaderProps {
  title?: string
  shareable?: boolean
}

export function EditionMobileHeader({ title, shareable = false }: EditionMobileHeaderProps) {
  const navigate = useNavigate()

  const goBack = () => {
    const idx = (window.history.state as { idx?: number } | null)?.idx ?? 0
    if (idx > 0) navigate(-1)
    else navigate('/')
  }

  const share = async () => {
    const url = window.location.href
    if (typeof navigator.share === 'function') {
      try {
        await navigator.share({ title, url })
      } catch (error) {
        if (!(error instanceof DOMException && error.name === 'AbortError')) {
          toast.error('Не удалось поделиться ссылкой')
        }
      }
      return
    }
    try {
      await navigator.clipboard.writeText(url)
      toast.success('Ссылка скопирована')
    } catch {
      toast.error('Не удалось скопировать ссылку')
    }
  }

  return (
    <div className="flex items-center gap-3 border-b border-line bg-surface px-4 py-3">
      <IconButton variant="plain" label="Назад" onClick={goBack}>
        <ChevronLeft aria-hidden="true" size={22} />
      </IconButton>
      <p className="min-w-0 flex-1 truncate font-semibold">{title}</p>
      {shareable && (
        <IconButton variant="plain" label="Поделиться" onClick={() => void share()}>
          <Share2 aria-hidden="true" size={20} />
        </IconButton>
      )}
    </div>
  )
}
