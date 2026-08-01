import { useState, type ChangeEvent, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { navigate } from '../router'

export function LoginPage() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(() => {
    const message = sessionStorage.getItem('smartsplit.sessionMessage') ?? ''
    sessionStorage.removeItem('smartsplit.sessionMessage')
    return message
  })
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login({ email, password })
      const returnTo = sessionStorage.getItem('smartsplit.returnTo')
      sessionStorage.removeItem('smartsplit.returnTo')
      navigate(returnTo || '/groups', true)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể đăng nhập lúc này')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-layout">
      <div className="auth-copy">
        <p className="eyebrow">SMARTSPLIT</p>
        <h1>Quay lại quản lý chi phí nhóm.</h1>
        <p>Đăng nhập để xem những nhóm bạn tham gia và tiếp tục công việc đang dang dở.</p>
      </div>
      <form className="form-card" onSubmit={handleSubmit}>
        <div className="form-heading">
          <h2>Đăng nhập</h2>
          <p>Nhập email và mật khẩu đã đăng ký.</p>
        </div>
        {error && <ErrorMessage message={error} />}
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(event: ChangeEvent<HTMLInputElement>) => setEmail(event.target.value)} placeholder="ha@example.com" autoComplete="email" required />
        </label>
        <label className="field">
          <span>Mật khẩu</span>
          <input type="password" value={password} onChange={(event: ChangeEvent<HTMLInputElement>) => setPassword(event.target.value)} placeholder="Tối thiểu 8 ký tự" autoComplete="current-password" required />
        </label>
        <button className="button button-primary button-block" type="submit" disabled={submitting}>
          {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
        <p className="form-footer">Chưa có tài khoản? <button type="button" className="text-button" onClick={() => navigate('/register')}>Đăng ký ngay</button></p>
      </form>
    </section>
  )
}
