import { WifiOff } from 'lucide-react'
import { useOnline } from '../../lib/useOnline'
import { Alert } from '../ui/Alert'

export function OfflineBanner() {
  const online = useOnline()
  if (online) return null
  return (
    <Alert tone="offline">
      <WifiOff size={20} aria-hidden="true" className="shrink-0" />
      <span>Нет подключения к интернету. Показываем сохранённые данные.</span>
    </Alert>
  )
}
