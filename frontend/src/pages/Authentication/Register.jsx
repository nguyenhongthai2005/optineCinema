import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import authService from '../../services/auth.service';

export default function Register() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: ''
  });
  
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    if (formData.password !== formData.confirmPassword) {
      setError("Mật khẩu xác nhận không khớp!");
      return;
    }
    
    setLoading(true);
    try {
      await authService.register(
        formData.fullName,
        formData.email,
        formData.phone,
        formData.password
      );
      
      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 2000);
      
    } catch (err) {
      const resMessage =
        (err.response &&
          err.response.data &&
          err.response.data.message) ||
        err.message ||
        err.toString();

      setError(resMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-bg"></div>
      
      <div className="auth-card" style={{ maxWidth: 500 }}>
        <Link to="/" className="auth-brand">OPTICINE</Link>
        <p className="auth-subtitle">Trở thành thành viên của gia đình Opticine</p>
        
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        
        {success && (
          <div className="alert alert-success" role="alert">
            Đăng ký thành công! Đang chuyển hướng đến trang đăng nhập...
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className="auth-form-group">
            <label className="auth-form-label">Họ và Tên</label>
            <input 
              type="text" 
              name="fullName"
              className="auth-input" 
              placeholder="VD: Nguyễn Văn A"
              value={formData.fullName}
              onChange={handleChange}
              required
            />
          </div>
          
          <div className="row">
            <div className="col-md-6">
              <div className="auth-form-group">
                <label className="auth-form-label">Email</label>
                <input 
                  type="email" 
                  name="email"
                  className="auth-input" 
                  placeholder="name@example.com"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
            <div className="col-md-6">
              <div className="auth-form-group">
                <label className="auth-form-label">Số điện thoại</label>
                <input 
                  type="tel" 
                  name="phone"
                  className="auth-input" 
                  placeholder="0912 345 678"
                  value={formData.phone}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </div>
          
          <div className="auth-form-group">
            <label className="auth-form-label">Mật khẩu</label>
            <input 
              type="password" 
              name="password"
              className="auth-input" 
              placeholder="Tạo mật khẩu"
              value={formData.password}
              onChange={handleChange}
              required
              minLength={6}
            />
          </div>
          
          <div className="auth-form-group">
            <label className="auth-form-label">Xác nhận mật khẩu</label>
            <input 
              type="password" 
              name="confirmPassword"
              className="auth-input" 
              placeholder="Nhập lại mật khẩu"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              minLength={6}
            />
          </div>
          
          <button type="submit" className="auth-btn-primary mt-2" disabled={loading}>
            {loading ? (
              <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            ) : (
              'ĐĂNG KÝ TÀI KHOẢN'
            )}
          </button>
        </form>

        <div className="auth-divider">HOẶC</div>

        <button type="button" className="auth-social-btn mb-3"
          onClick={() => { window.location.href = authService.getGoogleLoginUrl(); }}>
          <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" style={{width: 20, height: 20}} />
          Đăng ký bằng Google
        </button>
        
        <p className="text-center mt-4 mb-0" style={{ fontSize: 14, color: 'var(--on-surface-variant)' }}>
          Đã có tài khoản? <Link to="/login" className="auth-link ms-1">Đăng nhập ngay</Link>
        </p>
      </div>
    </div>
  );
}
