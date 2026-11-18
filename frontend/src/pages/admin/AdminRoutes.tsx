import { Route, Routes } from 'react-router-dom'
import { NotFound } from '../../components/ui/NotFound'

export default function AdminRoutes() {
  return (
    <Routes>
      <Route index element={<h1 className="font-display text-[26px] font-medium">Обзор системы</h1>} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
