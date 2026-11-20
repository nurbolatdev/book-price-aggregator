import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { RequireAdmin } from '../components/auth/RequireAdmin'
import { RequireGuest } from '../components/auth/RequireGuest'
import { RequireAuth } from '../components/auth/RequireAuth'
import { AppLayout } from '../components/layout/AppLayout'
import { PageSkeleton } from '../components/layout/PageSkeleton'
import { EditionRouteFallback } from '../features/edition/EditionSkeleton'
import { loadEditionPage } from '../features/edition/loadEditionPage'
import { loadAdminRoutes } from './loadAdminRoutes'

const HomePage = lazy(() => import('../pages/HomePage'))
const SearchPage = lazy(() => import('../pages/SearchPage'))
const DealsPage = lazy(() => import('../pages/DealsPage'))
const EditionPage = lazy(loadEditionPage)
const BookRedirectPage = lazy(() => import('../pages/BookRedirectPage'))
const LoginPage = lazy(() => import('../pages/LoginPage'))
const RegisterPage = lazy(() => import('../pages/RegisterPage'))
const CheckEmailPage = lazy(() => import('../pages/CheckEmailPage'))
const VerifyEmailPage = lazy(() => import('../pages/VerifyEmailPage'))
const ForgotPasswordPage = lazy(() => import('../pages/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('../pages/ResetPasswordPage'))
const UnsubscribePage = lazy(() => import('../pages/UnsubscribePage'))
const BeaconsPage = lazy(() => import('../pages/BeaconsPage'))
const ProfilePage = lazy(() => import('../pages/ProfilePage'))
const AdminRoutes = lazy(loadAdminRoutes)
const NotFoundPage = lazy(() => import('../pages/NotFoundPage'))

export default function App() {
  return (
    <Routes>
      <Route
        path="admin/*"
        element={
          <RequireAdmin
            denied={
              <AppLayout>
                <NotFoundPage />
              </AppLayout>
            }
          >
            <Suspense fallback={<PageSkeleton />}>
              <AdminRoutes />
            </Suspense>
          </RequireAdmin>
        }
      />
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="catalog" element={<SearchPage />} />
        <Route path="deals" element={<DealsPage />} />
        <Route
          path="editions/:id"
          element={
            <Suspense fallback={<EditionRouteFallback />}>
              <EditionPage />
            </Suspense>
          }
        />
        <Route path="books/:id" element={<BookRedirectPage />} />
        <Route element={<RequireGuest />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
        </Route>
        <Route path="check-email" element={<CheckEmailPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route path="unsubscribe" element={<UnsubscribePage />} />
        <Route element={<RequireAuth />}>
          <Route path="beacons" element={<BeaconsPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
