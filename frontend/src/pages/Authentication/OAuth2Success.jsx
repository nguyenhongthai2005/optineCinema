import React, { useContext, useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';

export default function OAuth2Success() {
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');
  const { completeGoogleLogin } = useContext(AuthContext);
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setError('Không nhận được mã đăng nhập từ Google.');
      return;
    }

    // Remove the JWT from browser history immediately, then hydrate the normal auth state.
    window.history.replaceState({}, document.title, '/oauth2/success');
    completeGoogleLogin(token)
      .then((user) => {
        const target = user.roles?.includes('ROLE_STAFF')
          ? '/staff'
          : user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_MANAGER')
            ? '/admin'
            : '/';
        navigate(target, { replace: true });
      })
      .catch(() => setError('Đăng nhập Google thất bại. Vui lòng thử lại.'));
  }, [completeGoogleLogin, navigate, searchParams]);

  return (
    <div className="auth-wrapper">
      <div className="auth-bg"></div>
      <div className="auth-card text-center">
        <Link to="/" className="auth-brand">OPTICINE</Link>
        {error ? (
          <>
            <div className="alert alert-danger" role="alert">{error}</div>
            <Link to="/login" className="auth-link">Quay lại đăng nhập</Link>
          </>
        ) : (
          <p className="auth-subtitle mb-0">Đang xử lý đăng nhập Google...</p>
        )}
      </div>
    </div>
  );
}
