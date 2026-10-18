import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

export default function Layout() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            <span className="brand-mark" aria-hidden="true">
              <svg viewBox="0 0 32 32" width="22" height="22">
                <rect width="32" height="32" rx="8" fill="currentColor" />
                <path d="M9 12h14M9 17h10M9 22h6" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </span>
            SupportDesk
          </NavLink>

          <nav className="nav">
            <NavLink to="/" end>Tickets</NavLink>
            <NavLink to="/tickets/new">New ticket</NavLink>
            {isAdmin && <NavLink to="/admin">Admin</NavLink>}
          </nav>

          <div className="user-menu">
            <div className="user-info">
              <span className="user-name">{user.fullName}</span>
              <span className={`role-tag role-${user.role.toLowerCase()}`}>{user.role}</span>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={handleLogout}>Log out</button>
          </div>
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
