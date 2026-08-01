import { useAuth } from '../auth/AuthContext'
import { navigate } from '../router'

export function AppHeader() {
  const { user, isAuthenticated, logout } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="app-header">
      <button className="brand-button" type="button" onClick={() => navigate(isAuthenticated ? '/groups' : '/')}>SmartSplit</button>
      <nav className="header-actions" aria-label="Điều hướng chính">
        {isAuthenticated && user ? (
          <>
            <button className="nav-link" type="button" onClick={() => navigate('/groups')}>Nhóm của tôi</button>
            <button className="user-chip" type="button" onClick={() => navigate('/account')} aria-label="Mở tài khoản cá nhân">
              <span className="avatar-small">{user.fullName.trim().charAt(0).toUpperCase()}</span>
              <span>{user.fullName}</span>
            </button>
            <button className="button button-ghost button-small" type="button" onClick={handleLogout}>Đăng xuất</button>
          </>
        ) : (
          <>
            <button className="nav-link" type="button" onClick={() => navigate('/login')}>Đăng nhập</button>
            <button className="button button-primary button-small" type="button" onClick={() => navigate('/register')}>Tạo tài khoản</button>
          </>
        )}
      </nav>
    </header>
  )
}
