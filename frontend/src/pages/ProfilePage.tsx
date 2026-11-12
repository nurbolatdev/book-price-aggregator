import { EmailBanner } from '../features/profile/EmailBanner'
import { NotificationSettings } from '../features/profile/NotificationSettings'
import { PersonalDataForm } from '../features/profile/PersonalDataForm'
import { ProfileCard } from '../features/profile/ProfileCard'
import { useMe } from '../features/profile/useMe'
import { usePageMeta } from '../lib/usePageMeta'

export default function ProfilePage() {
  usePageMeta({ title: 'Профиль и настройки | KitapRadar', description: 'Данные аккаунта и настройки писем.' })
  const { data: user } = useMe()
  return (
    <div className="mx-auto flex w-full max-w-[880px] flex-col gap-5 px-4 pb-16 pt-8 md:px-6">
      <h1 className="font-display text-2xl font-medium md:text-[30px]">Профиль и настройки</h1>
      {user && (
        <>
          {user.emailVerified === false && user.email && <EmailBanner email={user.email} />}
          <ProfileCard title="Личные данные">
            <PersonalDataForm user={user} />
          </ProfileCard>
          <ProfileCard title="Уведомления">
            <NotificationSettings />
          </ProfileCard>
        </>
      )}
    </div>
  )
}
