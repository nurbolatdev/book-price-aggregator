import { lazy } from 'react'
import { Route, Routes } from 'react-router-dom'
import { RequireAdmin } from '../components/auth/RequireAdmin'
import { RequireAuth } from '../components/auth/RequireAuth'
import { AppLayout } from '../components/layout/AppLayout'

const HomePage = lazy(() => import('../pages/HomePage'))
const SearchPage = lazy(() => import('../pages/SearchPage'))
const EditionPage = lazy(() => import('../pages/EditionPage'))
const LoginPage = lazy(() => import('../pages/LoginPage'))
const RegisterPage = lazy(() => import('../pages/RegisterPage'))
const BeaconsPage = lazy(() => import('../pages/BeaconsPage'))
const ProfilePage = lazy(() => import('../pages/ProfilePage'))
const AdminPage = lazy(() => import('../pages/AdminPage'))
const NotFoundPage = lazy(() => import('../pages/NotFoundPage'))

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="editions/:id" element={<EditionPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route element={<RequireAuth />}>
          <Route path="beacons" element={<BeaconsPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
        <Route element={<RequireAdmin />}>
          <Route path="admin" element={<AdminPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
