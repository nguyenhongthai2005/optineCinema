import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import authService from '../../services/auth.service';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authService.forgotPassword(email.trim());
      setSubmitted(true);
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err.message ||
        'Đã xảy ra lỗi. Vui lòng thử lại.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-bg"></div>

      <div className="auth-card" style={{ maxWidth: 460 }}>
        <Link to="/" className="auth-brand">OPTICINE</Link>
        <p className="auth-subtitle">Khôi phục mật khẩu của bạn</p>

        {/* Icon minh hoạ */}
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          marginBottom: 24,
        }}>
          <div style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: 'linear-gradient(135deg, rgba(225,29,72,0.15), rgba(225,29,72,0.05))',
            border: '1.5px solid rgba(225,29,72,0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 32,
          }}>
            🔑
          </div>
        </div>

        {submitted ? (
          /* Màn hình thành công */
          <div style={{ textAlign: 'center' }}>
            <div className="alert" style={{
              background: 'rgba(34,197,94,0.1)',
              border: '1px solid rgba(34,197,94,0.3)',
              borderRadius: 10,
              color: '#86efac',
              padding: '16px 20px',
              marginBottom: 24,
              lineHeight: 1.6,
            }}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>✉️</div>
              <strong>Email đã được gửi!</strong>
              <p style={{ marginTop: 8, marginBottom: 0, fontSize: 14, opacity: 0.9 }}>
                Nếu địa chỉ <strong>{email}</strong> tồn tại trong hệ thống,
                chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.
                Vui lòng kiểm tra hộp thư (kể cả thư rác).
              </p>
            </div>
            <p style={{ fontSize: 13, color: 'var(--on-surface-variant)' }}>
              Không nhận được email?{' '}
              <button
                onClick={() => { setSubmitted(false); setEmail(''); }}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--primary)',
                  cursor: 'pointer',
                  fontSize: 13,
                  padding: 0,
                  textDecoration: 'underline',
                }}
              >
                Thử lại
              </button>
            </p>
            <Link to="/login" className="auth-btn-primary" style={{
              display: 'block',
              textAlign: 'center',
              textDecoration: 'none',
              marginTop: 16,
            }}>
              Quay lại Đăng nhập
            </Link>
          </div>
        ) : (
          /* Form nhập email */
          <>
            {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
            )}

            <p style={{
              fontSize: 14,
              color: 'var(--on-surface-variant)',
              lineHeight: 1.6,
              marginBottom: 24,
              textAlign: 'center',
            }}>
              Nhập địa chỉ email đã đăng ký. Chúng tôi sẽ gửi đường dẫn
              để bạn đặt lại mật khẩu trong <strong style={{ color: 'var(--on-surface)' }}>15 phút</strong>.
            </p>

            <form onSubmit={handleSubmit}>
              <div className="auth-form-group">
                <label className="auth-form-label">Địa chỉ Email</label>
                <input
                  id="forgot-email"
                  type="email"
                  className="auth-input"
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              <button type="submit" className="auth-btn-primary mt-2" disabled={loading}>
                {loading ? (
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                ) : (
                  'GỬI LIÊN KẾT ĐẶT LẠI'
                )}
              </button>
            </form>

            <p className="text-center mt-4 mb-0" style={{ fontSize: 14, color: 'var(--on-surface-variant)' }}>
              Nhớ lại mật khẩu rồi? <Link to="/login" className="auth-link ms-1">Đăng nhập</Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
}
