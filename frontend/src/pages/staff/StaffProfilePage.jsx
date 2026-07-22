import { useEffect, useState } from 'react';
import staffService from '../../services/staff.service';
import { greenButton, input, page, section, subtitle, title } from './staffUi';
import { useToast } from '../../context/ToastContext';

export default function StaffProfilePage() {
  const [profile, setProfile] = useState({ fullName: '', phone: '', email: '' });
  const [password, setPassword] = useState({ currentPassword: '', newPassword: '' });
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const toast = useToast();

  useEffect(() => {
    staffService.getProfile()
      .then((res) => setProfile(res.data))
      .catch(() => toast.error('Không thể tải hồ sơ cá nhân.'));
  }, []);

  const saveProfile = async (event) => {
    event.preventDefault();
    if (savingProfile) return;
    setSavingProfile(true);
    try {
      const res = await staffService.updateProfile(profile);
      setProfile(res.data);
      toast.success('Cập nhật hồ sơ thành công!');
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Không cập nhật được hồ sơ.';
      toast.error(typeof msg === 'string' ? msg : 'Không cập nhật được hồ sơ.');
    } finally {
      setSavingProfile(false);
    }
  };

  const changePassword = async (event) => {
    event.preventDefault();
    if (savingPassword) return;
    setSavingPassword(true);
    try {
      await staffService.changePassword(password);
      setPassword({ currentPassword: '', newPassword: '' });
      toast.success('Đổi mật khẩu thành công!');
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Không đổi được mật khẩu.';
      toast.error(typeof msg === 'string' ? msg : 'Không đổi được mật khẩu.');
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div style={page}>
      <h1 style={title}>Hồ sơ cá nhân</h1>
      <p style={subtitle}>Xem và cập nhật thông tin tài khoản nhân viên của bạn.</p>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 18 }}>
        <section style={section}>
          <h2 style={{ marginTop: 0, fontSize: 18 }}>Thông tin cá nhân</h2>
          <form onSubmit={saveProfile}>
            <Field label="Họ tên" value={profile.fullName || ''} onChange={(value) => setProfile({ ...profile, fullName: value })} />
            <Field label="Số điện thoại" value={profile.phone || ''} onChange={(value) => setProfile({ ...profile, phone: value })} />
            <Field label="Email" value={profile.email || ''} onChange={(value) => setProfile({ ...profile, email: value })} />
            <button style={{ ...greenButton, marginTop: 14, opacity: savingProfile ? 0.7 : 1 }} disabled={savingProfile}>
              {savingProfile ? 'Đang lưu...' : 'Lưu hồ sơ'}
            </button>
          </form>
        </section>
        <section style={section}>
          <h2 style={{ marginTop: 0, fontSize: 18 }}>Đổi mật khẩu</h2>
          <form onSubmit={changePassword}>
            <Field label="Mật khẩu hiện tại" type="password" value={password.currentPassword} onChange={(value) => setPassword({ ...password, currentPassword: value })} />
            <Field label="Mật khẩu mới" type="password" value={password.newPassword} onChange={(value) => setPassword({ ...password, newPassword: value })} />
            <button style={{ ...greenButton, marginTop: 14, opacity: savingPassword ? 0.7 : 1 }} disabled={savingPassword}>
              {savingPassword ? 'Đang xử lý...' : 'Đổi mật khẩu'}
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}

function Field({ label, value, onChange, type = 'text' }) {
  return (
    <label style={{ display: 'block', marginTop: 12, color: 'var(--color-text-muted)', fontWeight: 800, fontSize: 13 }}>
      {label}
      <input type={type} style={{ ...input, marginTop: 6 }} value={value} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}

