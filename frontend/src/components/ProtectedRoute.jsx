import { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

export default function ProtectedRoute({ children, roles, positions }) {
  const { user } = useContext(AuthContext);
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && !roles.some((role) => user.roles?.includes(role))) {
    return <Navigate to="/" replace />;
  }

  const isAdmin = user.roles?.includes('ROLE_ADMIN');
  if (positions && !isAdmin && !positions.includes(user.position)) {
    return <div style={{ padding: 32, fontWeight: 800, color: '#fecaca' }}>403 - Bạn không có quyền truy cập chức năng này.</div>;
  }

  return children;
}
