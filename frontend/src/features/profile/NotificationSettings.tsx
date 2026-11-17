import { Checkbox } from '../../components/ui/Checkbox'
import { Radio } from '../../components/ui/Radio'
import { useMe } from './useMe'
import { useUpdateNotifications } from './useUpdateNotifications'

interface NotificationSettingsProps {
  variant?: 'profile' | 'panel'
}

export function NotificationSettings({ variant = 'profile' }: NotificationSettingsProps) {
  const panel = variant === 'panel'
  const { data: user } = useMe()
  const { mutate } = useUpdateNotifications(panel ? { successMessage: 'Сохранено' } : {})
  if (!user) return null
  return (
    <fieldset className="flex flex-col gap-1">
      <legend className="mb-1 text-[15px] font-semibold text-ink">{panel ? 'Как часто' : 'Письма о маячках'}</legend>
      <Radio
        name="notifyFrequency"
        label={panel ? 'Сразу, как сработает' : 'Сразу, как маячок сработает'}
        checked={user.notifyFrequency === 'INSTANT'}
        onChange={() => mutate({ notifyFrequency: 'INSTANT' })}
      />
      <Radio
        name="notifyFrequency"
        label={panel ? 'Сводка раз в день' : 'Одна сводка в день, в 9:00'}
        checked={user.notifyFrequency === 'DAILY'}
        onChange={() => mutate({ notifyFrequency: 'DAILY' })}
      />
      <Checkbox
        label={
          panel
            ? 'Еженедельная подборка скидок'
            : 'Еженедельная подборка самых больших скидок (по понедельникам)'
        }
        checked={Boolean(user.weeklyDigest)}
        onChange={(event) => mutate({ weeklyDigest: event.target.checked })}
      />
      {!panel && <p className="text-[13px] text-muted">Сохраняется сразу при изменении.</p>}
    </fieldset>
  )
}
