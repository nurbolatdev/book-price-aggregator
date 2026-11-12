import { useNavigate } from 'react-router-dom'
import { logout, logoutAll } from '../../api/auth'
import { getErrorMessage } from '../../api/errors'
import { Button } from '../../components/ui/Button'
import { toast } from '../../stores/toast'

export function SessionCard() {
  const navigate = useNavigate()

  async function onLogout() {
    await logout()
    navigate('/')
  }

  async function onLogoutAll() {
    try {
      await logoutAll()
    } catch (error) {
      toast.error(getErrorMessage(error))
      return
    }
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex flex-col gap-2.5 md:flex-row">
      <Button variant="secondary" onClick={() => void onLogout()} className="w-full md:w-auto">
        Выйти
      </Button>
      <Button variant="secondary" onClick={() => void onLogoutAll()} className="w-full md:w-auto">
        Выйти на всех устройствах
      </Button>
    </div>
  )
}
