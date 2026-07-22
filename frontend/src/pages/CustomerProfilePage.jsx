import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import AppNavbar from '../components/AppNavbar';
import customerService from '../services/customer.service';
import { useToast } from '../context/ToastContext';

// ─── Membership tier config ───────────────────────────────────────────────────
const TIER_COLORS = {
  Bronze:   { bg: 'linear-gradient(135deg, #cd7f32, #a0522d)', text: '#fff8f0', glow: 'rgba(205,127,50,0.35)' },
  Silver:   { bg: 'linear-gradient(135deg, #c0c0c0, #808080)', text: '#f8f8f8', glow: 'rgba(192,192,192,0.35)' },
  Gold:     { bg: 'linear-gradient(135deg, #ffd700, #b8860b)', text: '#3a2a00', glow: 'rgba(255,215,0,0.4)'   },
  Platinum: { bg: 'linear-gradient(135deg, #e5e4e2, #a9a9a9)', text: '#1a1a1a', glow: 'rgba(229,228,226,0.4)' },
};

const TIER_ICONS = {
  Bronze: '🥉',
  Silver: '🥈',
  Gold:   '🥇',
  Platinum: '💎',
};

// ─── Helpers ─────────────────────────────────────────────────────────────────
const formatVnd = (amount) =>
  amount === null || amount === undefined
    ? '0 ₫'
    : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

const initials = (name) =>
  (name || '?')
    .split(' ')
    .filter(Boolean)
    .map((w) => w[0].toUpperCase())
    .slice(-2)
    .join('');

// ─── Main Component ───────────────────────────────────────────────────────────
export default function CustomerProfilePage() {
  const navigate = useNavigate();
  const toast = useToast();

  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ fullName: '', phone: '' });
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    customerService
      .getProfile()
      .then((res) => {
        setProfile(res.data);
        setForm({ fullName: res.data.fullName || '', phone: res.data.phone || '' });
      })
      .catch((err) => {
        if (err.response?.status === 403 || err.response?.status === 401) {
          navigate('/login', { state: { from: { pathname: '/profile' } } });
        } else {
          toast.error('Không thể tải hồ sơ cá nhân.');
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    if (saving) return;
    setSaving(true);
    try {
      const res = await customerService.updateProfile({
        fullName: form.fullName.trim() || undefined,
        phone:    form.phone.trim()    || undefined,
      });
      setProfile(res.data);
      setForm({ fullName: res.data.fullName || '', phone: res.data.phone || '' });
      toast.success('Cập nhật hồ sơ thành công!');
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Không cập nhật được hồ sơ.';
      toast.error(typeof msg === 'string' ? msg : 'Không cập nhật được hồ sơ.');
    } finally {
      setSaving(false);
    }
  };

  // ── Loading skeleton ────────────────────────────────────────────────────────
  if (loading) {
    return (
      <>
        <AppNavbar />
        <div style={styles.page}>
          <div style={styles.skeleton} />
        </div>
      </>
    );
  }

  if (!profile) return null;

  const tierColors  = TIER_COLORS[profile.membershipName] || TIER_COLORS.Bronze;
  const tierIcon    = TIER_ICONS[profile.membershipName]  || '🥉';
  const progressPct = Math.min(100, Math.max(0, profile.progressPercent ?? 0));

  return (
    <>
      <AppNavbar />
      <div style={styles.page}>
        <div style={styles.container}>

          {/* ── Header ─────────────────────────────────────────────────────── */}
          <div style={styles.header}>
            <h1 style={styles.pageTitle}>Hồ sơ của tôi</h1>
            <p style={styles.pageSub}>Quản lý thông tin cá nhân và hạng thành viên</p>
          </div>

          <div style={styles.grid}>

            {/* ── LEFT COLUMN ────────────────────────────────────────────── */}
            <div style={styles.leftCol}>

              {/* Avatar + basic info card */}
              <div style={styles.card}>
                <div style={styles.avatarWrap}>
                  <div style={styles.avatar}>{initials(profile.fullName)}</div>
                  <div>
                    <div style={styles.userName}>{profile.fullName || 'Khách hàng'}</div>
                    <div style={styles.userEmail}>{profile.email}</div>
                  </div>
                </div>

                <hr style={styles.divider} />

                <form onSubmit={handleSave}>
                  <ProfileField
                    label="Họ và tên"
                    icon="person"
                    value={form.fullName}
                    onChange={(v) => setForm((f) => ({ ...f, fullName: v }))}
                    placeholder="Nhập họ và tên"
                  />
                  <ProfileField
                    label="Email"
                    icon="mail"
                    value={profile.email || ''}
                    disabled
                    placeholder=""
                  />
                  <ProfileField
                    label="Số điện thoại"
                    icon="phone"
                    value={form.phone}
                    onChange={(v) => setForm((f) => ({ ...f, phone: v }))}
                    placeholder="0901234567"
                    type="tel"
                  />
                  <button type="submit" style={{ ...styles.btn, opacity: saving ? 0.7 : 1 }} disabled={saving}>
                    <span className="material-symbols-outlined" style={{ fontSize: 18 }}>save</span>
                    {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </button>
                </form>
              </div>

              {/* Booking shortcut */}
              <Link to="/my-bookings" style={{ textDecoration: 'none' }}>
                <div style={styles.bookingCard}>
                  <span className="material-symbols-outlined" style={{ fontSize: 28, color: '#ec6a06' }}>
                    confirmation_number
                  </span>
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--color-text)' }}>Lịch sử đặt vé</div>
                    <div style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>Xem tất cả vé đã đặt</div>
                  </div>
                  <span className="material-symbols-outlined" style={{ marginLeft: 'auto', color: 'var(--color-text-muted)' }}>
                    arrow_forward_ios
                  </span>
                </div>
              </Link>
            </div>

            {/* ── RIGHT COLUMN ───────────────────────────────────────────── */}
            <div style={styles.rightCol}>

              {/* Membership badge card */}
              <div style={{ ...styles.card, ...styles.memberCard, background: tierColors.bg, boxShadow: `0 0 40px ${tierColors.glow}` }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                  <span style={{ fontSize: 40 }}>{tierIcon}</span>
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 600, color: tierColors.text, opacity: 0.75, textTransform: 'uppercase', letterSpacing: 1 }}>
                      Hạng thành viên
                    </div>
                    <div style={{ fontSize: 26, fontWeight: 900, color: tierColors.text }}>
                      {profile.membershipName || 'Bronze'}
                    </div>
                  </div>
                </div>

                {profile.membershipDiscount > 0 && (
                  <div style={{ ...styles.discountBadge, color: tierColors.text, border: `1px solid ${tierColors.text}50` }}>
                    Giảm {profile.membershipDiscount}% mỗi giao dịch
                  </div>
                )}

                {/* Progress bar */}
                {profile.nextMembershipName && (
                  <div style={{ marginTop: 18 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: tierColors.text, opacity: 0.85, marginBottom: 6 }}>
                      <span>{profile.membershipName}</span>
                      <span>{profile.nextMembershipName}</span>
                    </div>
                    <div style={styles.progressBg}>
                      <div style={{ ...styles.progressFill, width: `${progressPct}%` }} />
                    </div>
                    <div style={{ fontSize: 12, color: tierColors.text, opacity: 0.8, marginTop: 6, textAlign: 'center' }}>
                      Còn {formatVnd(profile.nextMembershipMinSpent - profile.totalSpent)} để lên hạng{' '}
                      <strong>{profile.nextMembershipName}</strong>
                    </div>
                  </div>
                )}

                {!profile.nextMembershipName && (
                  <div style={{ marginTop: 14, fontSize: 13, color: tierColors.text, opacity: 0.85, textAlign: 'center' }}>
                    🎉 Bạn đang ở hạng cao nhất!
                  </div>
                )}
              </div>

              {/* Stats cards */}
              <div style={styles.statsGrid}>
                <StatCard
                  icon="payments"
                  label="Tổng chi tiêu"
                  value={formatVnd(profile.totalSpent)}
                  color="#ec6a06"
                />
                <StatCard
                  icon="stars"
                  label="Điểm tích lũy"
                  value={`${(profile.points ?? 0).toLocaleString('vi-VN')} điểm`}
                  color="#ffd700"
                />
              </div>

              {/* Benefits list */}
              <div style={styles.card}>
                <div style={styles.cardTitle}>
                  <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#ec6a06' }}>card_membership</span>
                  Quyền lợi thành viên
                </div>
                <BenefitList tier={profile.membershipName} discount={profile.membershipDiscount} />
              </div>

            </div>
          </div>
        </div>
      </div>
    </>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────
function ProfileField({ label, icon, value, onChange, placeholder, type = 'text', disabled = false }) {
  return (
    <label style={styles.fieldLabel}>
      <span style={styles.fieldLabelText}>
        <span className="material-symbols-outlined" style={{ fontSize: 15 }}>{icon}</span>
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={onChange ? (e) => onChange(e.target.value) : undefined}
        placeholder={placeholder}
        disabled={disabled}
        style={{ ...styles.input, ...(disabled ? styles.inputDisabled : {}) }}
      />
    </label>
  );
}

function StatCard({ icon, label, value, color }) {
  return (
    <div style={styles.statCard}>
      <span className="material-symbols-outlined" style={{ fontSize: 28, color }}>{icon}</span>
      <div style={{ fontSize: 12, color: 'var(--color-text-muted)', marginTop: 4 }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--color-text)', marginTop: 2 }}>{value}</div>
    </div>
  );
}

function BenefitList({ tier, discount }) {
  const base = [
    { icon: 'movie', text: 'Mua vé online 24/7' },
    { icon: 'confirmation_number', text: 'Tích điểm mỗi giao dịch' },
    { icon: 'notifications', text: 'Thông báo lịch chiếu mới' },
  ];

  const tierBenefits = {
    Silver:   [{ icon: 'percent', text: `Giảm ${discount}% mỗi giao dịch` }, { icon: 'priority_high', text: 'Ưu tiên đặt vé sớm' }],
    Gold:     [{ icon: 'percent', text: `Giảm ${discount}% mỗi giao dịch` }, { icon: 'priority_high', text: 'Ưu tiên đặt vé sớm' }, { icon: 'local_offer', text: 'Combo ưu đãi hàng tháng' }],
    Platinum: [{ icon: 'percent', text: `Giảm ${discount}% mỗi giao dịch` }, { icon: 'priority_high', text: 'Ưu tiên đặt vé sớm' }, { icon: 'local_offer', text: 'Combo miễn phí hàng tháng' }, { icon: 'workspace_premium', text: 'Ghế VIP ưu tiên' }],
  };

  const extra = tierBenefits[tier] || [];
  const all = [...base, ...extra];

  return (
    <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
      {all.map((b, i) => (
        <li key={i} style={styles.benefitItem}>
          <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#22c55e' }}>{b.icon}</span>
          {b.text}
        </li>
      ))}
    </ul>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────
const styles = {
  page: {
    minHeight: '100vh',
    background: 'var(--color-bg)',
    paddingTop: 100,
    paddingBottom: 60,
  },
  container: {
    maxWidth: 1100,
    margin: '0 auto',
    padding: '0 20px',
  },
  header: {
    marginBottom: 32,
  },
  pageTitle: {
    fontSize: 28,
    fontWeight: 900,
    color: 'var(--color-text)',
    margin: 0,
  },
  pageSub: {
    color: 'var(--color-text-muted)',
    marginTop: 6,
    fontSize: 14,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1.5fr',
    gap: 24,
    alignItems: 'start',
  },
  leftCol: { display: 'flex', flexDirection: 'column', gap: 16 },
  rightCol: { display: 'flex', flexDirection: 'column', gap: 16 },
  card: {
    background: 'var(--color-surface)',
    border: '1px solid var(--color-border)',
    borderRadius: 16,
    padding: 24,
  },
  memberCard: {
    borderRadius: 20,
    padding: 28,
    border: 'none',
  },
  cardTitle: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    fontWeight: 700,
    fontSize: 15,
    color: 'var(--color-text)',
    marginBottom: 16,
  },
  avatarWrap: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    marginBottom: 4,
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #6366f1, #ec6a06)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 22,
    fontWeight: 900,
    color: '#fff',
    flexShrink: 0,
    letterSpacing: 1,
  },
  userName: {
    fontWeight: 800,
    fontSize: 16,
    color: 'var(--color-text)',
  },
  userEmail: {
    fontSize: 13,
    color: 'var(--color-text-muted)',
    marginTop: 2,
  },
  divider: {
    borderColor: 'var(--color-border)',
    margin: '20px 0',
  },
  fieldLabel: {
    display: 'block',
    marginBottom: 14,
  },
  fieldLabelText: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    fontSize: 12,
    fontWeight: 700,
    color: 'var(--color-text-muted)',
    marginBottom: 6,
    textTransform: 'uppercase',
    letterSpacing: 0.6,
  },
  input: {
    width: '100%',
    background: 'var(--color-surface-2)',
    border: '1px solid var(--color-border)',
    borderRadius: 8,
    padding: '10px 14px',
    color: 'var(--color-text)',
    fontSize: 14,
    outline: 'none',
    boxSizing: 'border-box',
  },
  inputDisabled: {
    opacity: 0.55,
    cursor: 'not-allowed',
  },
  btn: {
    width: '100%',
    marginTop: 8,
    padding: '11px 0',
    background: 'linear-gradient(90deg, #6366f1, #ec6a06)',
    border: 'none',
    borderRadius: 10,
    color: '#fff',
    fontWeight: 700,
    fontSize: 14,
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    transition: 'opacity 0.2s',
  },
  bookingCard: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    background: 'var(--color-surface)',
    border: '1px solid var(--color-border)',
    borderRadius: 14,
    padding: '16px 20px',
    cursor: 'pointer',
    transition: 'border-color 0.2s',
  },
  discountBadge: {
    display: 'inline-block',
    padding: '4px 14px',
    borderRadius: 20,
    fontSize: 13,
    fontWeight: 700,
    marginTop: 8,
  },
  progressBg: {
    height: 8,
    borderRadius: 100,
    background: 'rgba(255,255,255,0.25)',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: 100,
    background: 'rgba(255,255,255,0.85)',
    transition: 'width 0.6s ease',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: 14,
  },
  statCard: {
    background: 'var(--color-surface)',
    border: '1px solid var(--color-border)',
    borderRadius: 14,
    padding: '18px 16px',
    textAlign: 'center',
  },
  benefitItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '8px 0',
    borderBottom: '1px solid var(--color-border)',
    fontSize: 14,
    color: 'var(--color-text)',
  },
  skeleton: {
    maxWidth: 1100,
    margin: '0 auto',
    height: 400,
    borderRadius: 16,
    background: 'linear-gradient(90deg, var(--color-surface) 25%, var(--color-surface-2) 50%, var(--color-surface) 75%)',
    backgroundSize: '200% 100%',
    animation: 'shimmer 1.5s infinite',
  },
};
