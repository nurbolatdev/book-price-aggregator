import { Route, Routes } from 'react-router-dom'
import { NotFound } from '../../components/ui/NotFound'
import { AdminLayout } from '../../features/admin/AdminLayout'
import { AdminPageHeader } from '../../features/admin/AdminPageHeader'

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<AdminPageHeader title="Обзор системы" />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
