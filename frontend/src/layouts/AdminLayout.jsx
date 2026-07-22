  import { useContext } from 'react'
  import { NavLink, Outlet } from 'react-router-dom'
  import { AuthContext } from '../context/AuthContext'

  const NAV_ITEMS = [
    { to: '/admin',           icon: 'dashboard',      label: 'Dashboard',           end: true },
    { to: '/admin/movies',    icon: 'movie',          label: 'Quản lý phim' },
    { to: '/admin/combos', icon: 'fastfood', label: 'Combo bắp nước' },
    { to: '/admin/theaters',  icon: 'theater_comedy', label: 'Quản lý Rạp' },
    { to: '/admin/seats',     icon: 'chair',          label: 'Quản lý Ghế' },
    { to: '/admin/showtimes', icon: 'schedule',       label: 'Quản lý Giờ chiếu' },
    { to: '/admin/auto-schedule', icon: 'auto_awesome', label: 'Tối ưu lịch chiếu', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
    { to: '/admin/staff',      icon: 'badge',          label: 'Nhân viên', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
    { to: '/admin/staff-assignments', icon: 'assignment_ind', label: 'Phân công nhân viên', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
    { to: '/admin/attendance', icon: 'event_available', label: 'Chấm công' },
    { to: '/admin/customers',  icon: 'group',          label: 'Khách hàng' },
    { to: '/admin/payments/pending', icon: 'payments', label: 'Thanh toán VietQR', roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
    { to: '/admin/ticket-checkin', icon: 'qr_code_scanner', label: 'Soát vé' },
    { to: '/admin/promotions', icon: 'local_offer', label: 'Khuyến mãi', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
    { to: '/admin/reports', icon: 'analytics', label: 'Báo cáo doanh thu', roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
  ]

  export default function AdminLayout() {
    const { user } = useContext(AuthContext)
    const navItems = NAV_ITEMS.filter(item => !item.roles || item.roles.some(role => user?.roles?.includes(role)))

    return (
      <div className="app-shell">
        <aside className="app-sidebar">
          <div className="app-sidebar-brand">
            <div className="app-sidebar-logo">OptiCine</div>
            <div className="app-sidebar-meta">
              Admin Panel
            </div>
          </div>

          <nav className="app-sidebar-nav">
            <div className="app-sidebar-section">
              Quản lý
            </div>
            {navItems.map(({ to, icon, label, end }) => (
              <NavLink
                key={to} to={to} end={end}
                className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 20 }}>{icon}</span>
                <span>{label}</span>
              </NavLink>
            ))}
          </nav>

          <div className="app-sidebar-footer">
            <NavLink to="/" className="app-nav-link">
              <span className="material-symbols-outlined" style={{ fontSize: 18 }}>arrow_back</span>
              <span>Về trang chủ</span>
            </NavLink>
          </div>
        </aside>

        <main className="app-main">
          <Outlet />
        </main>
      </div>
    )
  }
