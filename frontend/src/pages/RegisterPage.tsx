import { useState, type ChangeEvent, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { navigate } from '../router'

export function RegisterPage() {
  const { register } = useAuth()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận chưa khớp')
      return
    }
    setSubmitting(true)
    try {
      await register({ fullName, email, password })
      const returnTo = sessionStorage.getItem('smartsplit.returnTo')
      sessionStorage.removeItem('smartsplit.returnTo')
      navigate(returnTo || '/groups', true)
    } catch (caught) {
      if (caught instanceof ApiError) {
        const fieldMessage = Object.values(caught.fields)[0]
        setError(fieldMessage || caught.message)
      } else {
        setError('Không thể tạo tài khoản lúc này')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-layout">
      <div className="auth-copy">
        <p className="eyebrow">ITERATION 1</p>
        <h1>Tạo tài khoản và nhóm đầu tiên.</h1>
        <p>Tài khoản mới sẽ được đăng nhập ngay sau khi đăng ký thành công.</p>
      </div>
      <form className="form-card" onSubmit={handleSubmit}>
        <div className="form-heading">
          <h2>Đăng ký</h2>
          <p>Mật khẩu cần ít nhất 8 ký tự, có chữ và số.</p>
        </div>
        {error && <ErrorMessage message={error} />}
        <label className="field">
          <span>Họ và tên</span>
          <input value={fullName} onChange={(event: ChangeEvent<HTMLInputElement>) => setFullName(event.target.value)} placeholder="Hà Mi" autoComplete="name" required />
        </label>
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(event: ChangeEvent<HTMLInputElement>) => setEmail(event.target.value)} placeholder="ha@example.com" autoComplete="email" required />
        </label>
        <label className="field">
          <span>Mật khẩu</span>
          <input type="password" value={password} onChange={(event: ChangeEvent<HTMLInputElement>) => setPassword(event.target.value)} placeholder="Ví dụ: Ha123456" autoComplete="new-password" required />
        </label>
        <label className="field">
          <span>Xác nhận mật khẩu</span>
          <input type="password" value={confirmPassword} onChange={(event: ChangeEvent<HTMLInputElement>) => setConfirmPassword(event.target.value)} placeholder="Nhập lại mật khẩu" autoComplete="new-password" required />
        </label>
        <button className="button button-primary button-block" type="submit" disabled={submitting}>
          {submitting ? 'Đang tạo tài khoản…' : 'Tạo tài khoản'}
        </button>
        <p className="form-footer">Đã có tài khoản? <button type="button" className="text-button" onClick={() => navigate('/login')}>Đăng nhập</button></p>
      </form>
    </section>
  )
}
