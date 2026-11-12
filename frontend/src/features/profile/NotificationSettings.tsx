import { Checkbox } from '../../components/ui/Checkbox'
import { Radio } from '../../components/ui/Radio'
import { useMe } from './useMe'
import { useUpdateNotifications } from './useUpdateNotifications'

export function NotificationSettings() {
  const { data: user } = useMe()
  const { mutate } = useUpdateNotifications()
  if (!user) return null
  return (
    <fieldset className="flex flex-col gap-1">
      <legend className="mb-1 text-[15px] font-semibold text-ink">Письма о маячках</legend>
      <Radio
        name="notifyFrequency"
        label="Сразу, как маячок сработает"
        checked={user.notifyFrequency === 'INSTANT'}
        onChange={() => mutate({ notifyFrequency: 'INSTANT' })}
      />
      <Radio
        name="notifyFrequency"
        label="Одна сводка в день, в 9:00"
        checked={user.notifyFrequency === 'DAILY'}
        onChange={() => mutate({ notifyFrequency: 'DAILY' })}
      />
      <Checkbox
        label="Еженедельная подборка самых больших скидок (по понедельникам)"
        checked={Boolean(user.weeklyDigest)}
        onChange={(event) => mutate({ weeklyDigest: event.target.checked })}
      />
      <p className="text-[13px] text-muted">Сохраняется сразу при изменении.</p>
    </fieldset>
  )
}
