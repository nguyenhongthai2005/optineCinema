import { useContext } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const NAV_ITEMS = [
  { to: '/staff/dashboard', icon: 'dashboard', label: 'Tổng quan' },
  { to: '/staff/my-schedule', icon: 'calendar_month', label: 'Lịch làm việc' },
  { to: '/staff/attendance', icon: 'event_available', label: 'Chấm công' },
  { to: '/staff/sell-ticket', icon: 'point_of_sale', label: 'Bán vé tại quầy', positions: ['COUNTER_SALES'] },
  { to: '/staff/combo-sales', icon: 'fastfood', label: 'Bán bắp nước/combo', positions: ['COUNTER_SALES'] },
  { to: '/staff/check-in', icon: 'qr_code_scanner', label: 'Soát vé', positions: ['TICKET_CHECKER'] },
  { to: '/staff/payments/pending', icon: 'payments', label: 'Thanh toán chờ xác nhận', positions: ['COUNTER_SALES'] },
  { to: '/staff/orders', icon: 'receipt_long', label: 'Vé / Đơn hàng', positions: ['COUNTER_SALES'] },
  { to: '/staff/profile', icon: 'account_circle', label: 'Hồ sơ cá nhân' },
  { to: '/staff/availability', icon: 'schedule', label: 'Đăng ký giờ làm' },
];

export default function StaffLayout() {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const visibleItems = NAV_ITEMS.filter(item => !item.positions || user?.roles?.includes('ROLE_ADMIN') || item.positions.includes(user?.position));

  const doLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-shell">
      <aside className="app-sidebar staff">
        <div className="app-sidebar-brand">
          <div className="app-sidebar-logo">Opticine Staff</div>
          <div className="app-sidebar-meta">{user?.fullName || user?.username}</div>
        </div>
        <nav className="app-sidebar-nav">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 20 }}>{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="app-sidebar-footer">
          <button onClick={doLogout} style={{ ...navButton, width: '100%' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 20 }}>logout</span>
            Đăng xuất
          </button>
        </div>
      </aside>
      <main className="app-main staff">
        <Outlet />
      </main>
    </div>
  );
}

const navButton = {
  border: 'none',
  background: '#26344d',
  color: '#e5edf7',
  borderRadius: 8,
  padding: '11px 12px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 10,
  fontWeight: 800,
  cursor: 'pointer',
};
