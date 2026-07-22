import React, { useState, useContext, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import authService from '../../services/auth.service';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname
    ? `${location.state.from.pathname}${location.state.from.search || ''}`
    : '/';

  useEffect(() => {
    const oauthError = new URLSearchParams(location.search).get('oauthError');
    if (oauthError === 'account_inactive') {
      setError('Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động.');
    } else if (oauthError === 'google_email_missing') {
      setError('Không nhận được email từ Google.');
    } else if (oauthError) {
      setError('Đăng nhập Google thất bại. Vui lòng thử lại.');
    }
  }, [location.search]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      const user = await login(email, password);
      const defaultPath = user.roles?.includes('ROLE_STAFF')
        ? '/staff'
        : user.roles?.includes('ROLE_ADMIN') || user.roles?.includes('ROLE_MANAGER')
          ? '/admin'
          : '/';
      navigate(from === '/' ? defaultPath : from, { replace: true });
    } catch (err) {
      const resMessage =
        (err.response &&
          err.response.data &&
          err.response.data.message) ||
        err.message ||
        err.toString();

      setError("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-bg"></div>
      
      <div className="auth-card">
        <Link to="/" className="auth-brand">OPTICINE</Link>
        <p className="auth-subtitle">Đăng nhập vào tài khoản của bạn</p>
        
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className="auth-form-group">
            <label className="auth-form-label">Email, số điện thoại hoặc tài khoản</label>
            <input 
              type="text" 
              className="auth-input" 
              placeholder="Nhập email, số điện thoại hoặc tài khoản"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          
          <div className="auth-form-group">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <label className="auth-form-label mb-0">Mật khẩu</label>
              <Link to="/forgot-password" className="auth-link">Quên mật khẩu?</Link>
            </div>
            <input 
              type="password" 
              className="auth-input" 
              placeholder="Nhập mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          
          <button type="submit" className="auth-btn-primary" disabled={loading}>
            {loading ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            ) : (
              'ĐĂNG NHẬP'
            )}
          </button>
        </form>
        
        <div className="auth-divider">HOẶC</div>
        
        <button type="button" className="auth-social-btn mb-3"
          onClick={() => { window.location.href = authService.getGoogleLoginUrl(); }}>
          <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" style={{width: 20, height: 20}} />
          Tiếp tục với Google
        </button>
        
        <p className="text-center mt-4 mb-0" style={{ fontSize: 14, color: 'var(--on-surface-variant)' }}>
          Chưa có tài khoản? <Link to="/register" className="auth-link ms-1">Đăng ký ngay</Link>
        </p>
      </div>
    </div>
  );
}
