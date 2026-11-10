
import { useState, type FormEvent } from 'react'
import {
  Link,
  Navigate,
  useNavigate,
  useLocation,
} from 'react-router-dom'
import { useAuth } from '../contexts/useAuth'
function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()

  const locationState = location.state as {
    message?: string
    from?: string
  } | null

  const successMessage = locationState?.message

  const from =
    locationState?.from?.startsWith('/') &&
    !locationState.from.startsWith('//') &&
    !locationState.from.startsWith('/\\')
      ? locationState.from
      : '/profile'

  const { login, user, loading: authLoading } = useAuth()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault()

    if (loading) return

    setError('')
    setLoading(true)

    try {
      await login(email.trim(), password)

    navigate(from, { replace: true })
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : 'Đăng nhập thất bại. Vui lòng thử lại.'
      )
    } finally {
      setLoading(false)
    }
  }

  if (authLoading) {
  return <p>Đang kiểm tra phiên đăng nhập...</p>
  }

  if (user) {
  return <Navigate to={from} replace />
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-back">
          ← Quay về trang chủ
        </Link>

        <h1>Đăng nhập</h1>

        <p className="auth-subtitle">
          Chào mừng bạn đến với HoaKhanh Rental
        </p>
        {successMessage && (
        <div className="auth-success" role="status">
            {successMessage}
        </div>
        )}
        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="email">Email</label>

            <input
              id="email"
              type="email"
              placeholder="Nhập email của bạn"
              autoComplete="email"
              value={email}
              onChange={(event) =>
                setEmail(event.target.value)
              }
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="password">
              Mật khẩu
            </label>

            <input
              id="password"
              type="password"
              placeholder="Nhập mật khẩu"
              autoComplete="current-password"
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              required
            />
          </div>

          {error && (
            <div className="auth-error" role="alert">
              {error}
            </div>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            {loading
              ? 'Đang đăng nhập...'
              : 'Đăng nhập'}
          </button>
        </form>
        <p className="auth-switch">
        Chưa có tài khoản?{' '}
        <Link to="/register">Đăng ký ngay</Link>
        </p>
      </div>
    </div>
  )
}

export default LoginPage
