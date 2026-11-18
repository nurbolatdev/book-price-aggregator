import { Route, Routes } from 'react-router-dom'
import { NotFound } from '../../components/ui/NotFound'
import { AdminLayout } from '../../features/admin/AdminLayout'
import NotificationsPage from './NotificationsPage'
import OverviewPage from './OverviewPage'
import ParsersPage from './ParsersPage'
import QueuesPage from './QueuesPage'
import UsersPage from './UsersPage'

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<OverviewPage />} />
        <Route path="parsers" element={<ParsersPage />} />
        <Route path="queues" element={<QueuesPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
