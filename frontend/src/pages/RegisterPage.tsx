
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { registerApi } from '../services/authApi'

function RegisterPage() {
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault()

    if (loading) return

    setError('')

    if (!fullName.trim()) {
      setError('Vui lòng nhập họ và tên')
      return
    }

    if (password.length < 12 || password.length > 72) {
      setError('Mật khẩu phải có từ 12 đến 72 ký tự')
      return
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp')
      return
    }

    setLoading(true)

    try {
      await registerApi({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
      })

      navigate('/login', {
        replace: true,
        state: {
          message: 'Đăng ký thành công! Hãy đăng nhập.',
        },
      })
    } catch (err: unknown) {
      setError(
        err instanceof Error
          ? err.message
          : 'Không thể đăng ký tài khoản'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-back">
          ← Quay về trang chủ
        </Link>

        <h1>Đăng ký tài khoản</h1>

        <p className="auth-subtitle">
          Tạo tài khoản HoaKhanh Rental
        </p>

        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="fullName">
              Họ và tên
            </label>

            <input
              id="fullName"
              type="text"
              placeholder="Nhập họ và tên"
              autoComplete="name"
              value={fullName}
              onChange={(event) =>
                setFullName(event.target.value)
              }
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="registerEmail">
              Email
            </label>

            <input
              id="registerEmail"
              type="email"
              placeholder="Nhập email"
              autoComplete="email"
              value={email}
              onChange={(event) =>
                setEmail(event.target.value)
              }
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="registerPassword">
              Mật khẩu
            </label>

            <input
              id="registerPassword"
              type="password"
              placeholder="Ít nhất 12 ký tự"
              autoComplete="new-password"
              minLength={12}
              maxLength={72}
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              required
            />
          </div>

          <div className="auth-field">
            <label htmlFor="confirmPassword">
              Xác nhận mật khẩu
            </label>

            <input
              id="confirmPassword"
              type="password"
              placeholder="Nhập lại mật khẩu"
              autoComplete="new-password"
              minLength={12}
              maxLength={72}
              value={confirmPassword}
              onChange={(event) =>
                setConfirmPassword(event.target.value)
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
              ? 'Đang tạo tài khoản...'
              : 'Đăng ký'}
          </button>
        </form>

        <p className="auth-switch">
          Đã có tài khoản?{' '}
          <Link to="/login">Đăng nhập</Link>
        </p>
      </div>
    </div>
  )
}

export default RegisterPage
