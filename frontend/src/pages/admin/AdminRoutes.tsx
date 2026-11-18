import { Route, Routes } from 'react-router-dom'
import { NotFound } from '../../components/ui/NotFound'
import { AdminLayout } from '../../features/admin/AdminLayout'
import OverviewPage from './OverviewPage'

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<OverviewPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
