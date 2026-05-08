import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export default function AdminRoute() {
  const user = useAuthStore((s) => s.user)
  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
