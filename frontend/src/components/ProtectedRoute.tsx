
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'

interface ProtectedRouteProps {
  children: ReactNode
  allowedRoles?: string[]
}

function ProtectedRoute({
  children,
  allowedRoles,
}: ProtectedRouteProps) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <p>Đang kiểm tra phiên đăng nhập...</p>
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname }}
      />
    )
  }

  if (
    allowedRoles &&
    !allowedRoles.includes(user.role)
  ) {
    return <Navigate to="/profile" replace />
  }

  return <>{children}</>
}

export default ProtectedRoute
