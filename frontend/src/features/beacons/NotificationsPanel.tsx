import { useId } from 'react'
import { EmailStatus } from '../profile/EmailStatus'
import { NotificationSettings } from '../profile/NotificationSettings'
import { useMe } from '../profile/useMe'

export function NotificationsPanel() {
  const headingId = useId()
  const { data: user } = useMe()
  return (
    <section
      aria-labelledby={headingId}
      className="flex flex-col gap-2.5 rounded-card border border-line bg-surface p-4 md:gap-3.5 md:p-5"
    >
      <h2 id={headingId} className="font-sans text-[17px] font-bold md:text-lg">
        Уведомления
      </h2>
      {user && (
        <>
          <div className="flex flex-col gap-1">
            <span className="text-[13px] text-muted md:text-sm">Письма приходят на</span>
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-semibold">{user.email}</span>
              <EmailStatus verified={Boolean(user.emailVerified)} />
            </div>
          </div>
          <NotificationSettings variant="panel" />
        </>
      )}
    </section>
  )
}
