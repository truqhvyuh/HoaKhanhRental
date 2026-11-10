
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'

function ProfilePage() {
  const navigate = useNavigate()

const { user, logout } = useAuth()

  if (!user) {
  return null
  }

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-back">
          ← Quay về trang chủ
        </Link>

        <h1>Tài khoản của tôi</h1>

        <p className="auth-subtitle">
          Thông tin tài khoản HoaKhanh Rental
        </p>

        <div className="profile-info">
          <div className="profile-row">
            <span>Họ và tên</span>
            <strong>{user.fullName}</strong>
          </div>

          <div className="profile-row">
            <span>Email</span>
            <strong>{user.email}</strong>
          </div>

          <div className="profile-row">
            <span>Vai trò</span>
            <strong>{user.role}</strong>
          </div>
        </div>

        <button
          type="button"
          className="auth-submit"
          onClick={handleLogout}
        >
          Đăng xuất
        </button>
      </div>
    </div>
  )
}

export default ProfilePage
