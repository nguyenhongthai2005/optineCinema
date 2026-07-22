import React, { useEffect, useState, useContext } from 'react'
import { Container } from 'react-bootstrap'
import { Link } from 'react-router-dom'
import { AuthContext } from '../context/AuthContext'

export default function AppNavbar() {
  const [scrolled, setScrolled] = useState(false)
  const { user, logout } = useContext(AuthContext)

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50)
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  return (
    <nav className={`opticine-nav py-4 ${scrolled ? 'scrolled' : ''}`} id="main-nav">
      <Container fluid style={{ maxWidth: 1440 }}>
        <div className="d-flex justify-content-between align-items-center">
          {/* Left: Brand + Nav Links */}
          <div className="d-flex align-items-center gap-5">
            <Link to="/" className="nav-brand text-decoration-none">OPTICINE</Link>
            <div className="d-none d-md-flex align-items-center gap-4">
              <Link to="/" className="nav-link-item active text-decoration-none">Phim</Link>
              <Link to="/showtimes" className="nav-link-item text-decoration-none">Lịch chiếu</Link>
              {user && <Link to="/my-bookings" className="nav-link-item text-decoration-none">Vé của tôi</Link>}
            </div>
          </div>

          {/* Right: Search + Icons + CTA */}
          <div className="d-flex align-items-center gap-2">
            <div className="search-box d-none d-lg-flex me-3">
              <span className="material-symbols-outlined text-secondary" style={{ color: 'var(--on-surface-variant)', fontSize: 20 }}>search</span>
              <input type="text" placeholder="Tìm kiếm phim, rạp..." />
            </div>
            <button className="icon-btn">
              <span className="material-symbols-outlined">notifications</span>
            </button>
            {user ? (
              <>
                <Link to="/my-bookings" className="icon-btn d-inline-flex align-items-center justify-content-center text-decoration-none" title="Vé của tôi" style={{ color: 'inherit' }}>
                  <span className="material-symbols-outlined">confirmation_number</span>
                </Link>
                {user.roles?.includes('ROLE_CUSTOMER') && (
                  <Link to="/profile" className="icon-btn d-inline-flex align-items-center justify-content-center text-decoration-none" title="Hồ sơ của tôi" style={{ color: 'inherit' }}>
                    <span className="material-symbols-outlined">person</span>
                  </Link>
                )}
                <button className="icon-btn" onClick={logout} title="Đăng xuất">
                  <span className="material-symbols-outlined text-danger">logout</span>
                </button>
              </>
            ) : (
              <Link to="/login" className="icon-btn d-inline-flex align-items-center justify-content-center text-decoration-none" style={{ color: 'inherit' }}>
                <span className="material-symbols-outlined">person</span>
              </Link>
            )}
            <Link to="/showtimes" className="btn-book ms-3 text-decoration-none d-inline-flex align-items-center justify-content-center">Đặt vé</Link>
          </div>
        </div>
      </Container>
    </nav>
  )
}
