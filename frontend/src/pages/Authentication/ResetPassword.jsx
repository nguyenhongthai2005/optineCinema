import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import authService from '../../services/auth.service';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get('token');

  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setError('Liên kết không hợp lệ. Vui lòng yêu cầu đặt lại mật khẩu lại.');
    }
  }, [token]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const getPasswordStrength = (pwd) => {
    if (!pwd) return { level: 0, label: '', color: '' };
    if (pwd.length < 6) return { level: 1, label: 'Yếu', color: '#ef4444' };
    if (pwd.length < 10) return { level: 2, label: 'Trung bình', color: '#f97316' };
    if (/[A-Z]/.test(pwd) && /[0-9]/.test(pwd)) return { level: 4, label: 'Rất mạnh', color: '#22c55e' };
    return { level: 3, label: 'Mạnh', color: '#84cc16' };
  };

  const strength = getPasswordStrength(formData.newPassword);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (formData.newPassword !== formData.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp!');
      return;
    }
    if (formData.newPassword.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự.');
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword(token, formData.newPassword);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
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
        <p className="auth-subtitle">Tạo mật khẩu mới</p>

        {/* Icon */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
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
            🔒
          </div>
        </div>

        {success ? (
          /* Màn hình thành công */
          <div style={{ textAlign: 'center' }}>
            <div style={{
              background: 'rgba(34,197,94,0.1)',
              border: '1px solid rgba(34,197,94,0.3)',
              borderRadius: 10,
              color: '#86efac',
              padding: '20px 24px',
              marginBottom: 24,
              lineHeight: 1.6,
            }}>
              <div style={{ fontSize: 40, marginBottom: 12 }}>✅</div>
              <strong style={{ fontSize: 16 }}>Mật khẩu đã được đặt lại thành công!</strong>
              <p style={{ marginTop: 8, marginBottom: 0, fontSize: 14, opacity: 0.9 }}>
                Bạn sẽ được chuyển hướng đến trang đăng nhập sau 3 giây...
              </p>
            </div>
            <Link to="/login" className="auth-btn-primary" style={{
              display: 'block',
              textAlign: 'center',
              textDecoration: 'none',
            }}>
              Đăng nhập ngay
            </Link>
          </div>
        ) : (
          <>
            {error && (
              <div className="alert alert-danger" role="alert">
                {error}
                {!token && (
                  <div style={{ marginTop: 8 }}>
                    <Link to="/forgot-password" style={{ color: 'inherit', textDecoration: 'underline' }}>
                      Yêu cầu đặt lại mật khẩu
                    </Link>
                  </div>
                )}
              </div>
            )}

            {token && (
              <>
                <p style={{
                  fontSize: 14,
                  color: 'var(--on-surface-variant)',
                  marginBottom: 24,
                  textAlign: 'center',
                  lineHeight: 1.6,
                }}>
                  Nhập mật khẩu mới cho tài khoản của bạn.
                </p>

                <form onSubmit={handleSubmit}>
                  {/* Mật khẩu mới */}
                  <div className="auth-form-group">
                    <label className="auth-form-label">Mật khẩu mới</label>
                    <div style={{ position: 'relative' }}>
                      <input
                        id="new-password"
                        type={showPassword ? 'text' : 'password'}
                        name="newPassword"
                        className="auth-input"
                        placeholder="Nhập mật khẩu mới (ít nhất 6 ký tự)"
                        value={formData.newPassword}
                        onChange={handleChange}
                        required
                        minLength={6}
                        autoFocus
                        style={{ paddingRight: 44 }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        style={{
                          position: 'absolute',
                          right: 12,
                          top: '50%',
                          transform: 'translateY(-50%)',
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          color: 'var(--on-surface-variant)',
                          fontSize: 18,
                          padding: 0,
                          lineHeight: 1,
                        }}
                        aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                      >
                        {showPassword ? '🙈' : '👁️'}
                      </button>
                    </div>

                    {/* Thanh đo độ mạnh */}
                    {formData.newPassword && (
                      <div style={{ marginTop: 8 }}>
                        <div style={{
                          display: 'flex',
                          gap: 4,
                          marginBottom: 4,
                        }}>
                          {[1, 2, 3, 4].map((lvl) => (
                            <div key={lvl} style={{
                              flex: 1,
                              height: 4,
                              borderRadius: 2,
                              background: lvl <= strength.level ? strength.color : 'rgba(255,255,255,0.1)',
                              transition: 'background 0.3s',
                            }} />
                          ))}
                        </div>
                        <span style={{ fontSize: 12, color: strength.color }}>
                          Độ mạnh: {strength.label}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Xác nhận mật khẩu */}
                  <div className="auth-form-group">
                    <label className="auth-form-label">Xác nhận mật khẩu</label>
                    <input
                      id="confirm-password"
                      type={showPassword ? 'text' : 'password'}
                      name="confirmPassword"
                      className="auth-input"
                      placeholder="Nhập lại mật khẩu mới"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      required
                      minLength={6}
                    />
                    {formData.confirmPassword && formData.newPassword !== formData.confirmPassword && (
                      <p style={{ fontSize: 12, color: '#ef4444', marginTop: 4 }}>
                        Mật khẩu không khớp
                      </p>
                    )}
                    {formData.confirmPassword && formData.newPassword === formData.confirmPassword && (
                      <p style={{ fontSize: 12, color: '#22c55e', marginTop: 4 }}>
                        ✓ Mật khẩu khớp
                      </p>
                    )}
                  </div>

                  <button
                    type="submit"
                    className="auth-btn-primary mt-2"
                    disabled={loading || !token}
                  >
                    {loading ? (
                      <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                    ) : (
                      'ĐẶT LẠI MẬT KHẨU'
                    )}
                  </button>
                </form>
              </>
            )}

            <p className="text-center mt-4 mb-0" style={{ fontSize: 14, color: 'var(--on-surface-variant)' }}>
              <Link to="/login" className="auth-link">← Quay lại Đăng nhập</Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
}
