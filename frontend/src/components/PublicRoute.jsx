import { useContext } from 'react'
import { Navigate } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'

export default function PublicRoute({ children }) {
  const { user } = useContext(AuthContext)

  if (user) {
    const roles = user.roles || []
    if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MANAGER')) {
      return <Navigate to="/admin" replace />
    }
    if (roles.includes('ROLE_STAFF')) {
      return <Navigate to="/staff" replace />
    }
  }

  return children
}
