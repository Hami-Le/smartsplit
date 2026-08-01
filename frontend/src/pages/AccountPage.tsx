import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import {
  changePassword,
  getProfile,
  updateProfile,
  type UserProfile,
} from '../api/authApi'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'

function errorMessage(caught: unknown, fallback: string): string {
  if (!(caught instanceof ApiError)) return fallback
  return Object.values(caught.fields)[0] || caught.message
}

export function AccountPage() {
  const { user, updateUser } = useAuth()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [profileError, setProfileError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordSuccess, setPasswordSuccess] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getProfile()
      .then((data) => {
        setProfile(data)
        setFullName(data.fullName)
        setPhone(data.phone ?? '')
      })
      .catch((caught) => setProfileError(errorMessage(caught, 'Không thể tải thông tin tài khoản')))
      .finally(() => setLoading(false))
  }, [])

  const handleProfileSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setProfileError('')
    setProfileSuccess('')
    setSavingProfile(true)
    try {
      const updated = await updateProfile({ fullName, phone })
      setProfile(updated)
      setFullName(updated.fullName)
      setPhone(updated.phone ?? '')
      updateUser({ id: updated.id, fullName: updated.fullName, email: updated.email, role: updated.role })
      setProfileSuccess('Đã cập nhật thông tin cá nhân.')
    } catch (caught) {
      setProfileError(errorMessage(caught, 'Không thể cập nhật thông tin'))
    } finally {
      setSavingProfile(false)
    }
  }

  const handlePasswordSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setPasswordError('')
    setPasswordSuccess('')
    if (newPassword !== confirmPassword) {
      setPasswordError('Mật khẩu xác nhận chưa khớp')
      return
    }
    setSavingPassword(true)
    try {
      await changePassword({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setPasswordSuccess('Đã đổi mật khẩu.')
    } catch (caught) {
      setPasswordError(errorMessage(caught, 'Không thể đổi mật khẩu'))
    } finally {
      setSavingPassword(false)
    }
  }

  if (loading) return <LoadingState label="Đang tải tài khoản…" />

  return (
    <section className="page-section">
      <div className="account-heading">
        <p className="eyebrow">TÀI KHOẢN CÁ NHÂN</p>
        <h1 className="page-title">Thông tin của bạn.</h1>
      </div>

      <div className="account-grid">
        <form className="form-card account-card" onSubmit={handleProfileSubmit}>
          <span className="account-avatar" aria-hidden="true">
            {(profile?.fullName || user?.fullName || '?').trim().charAt(0).toUpperCase()}
          </span>
          <div className="form-heading">
            <h2>Thông tin cá nhân</h2>
          </div>
          {profileError && <ErrorMessage message={profileError} />}
          {profileSuccess && <p className="alert alert-success">{profileSuccess}</p>}
          <label className="field">
            <span>Họ và tên</span>
            <input value={fullName} onChange={(event: ChangeEvent<HTMLInputElement>) => setFullName(event.target.value)} autoComplete="name" maxLength={120} required />
          </label>
          <label className="field">
            <span>Email</span>
            <input value={profile?.email ?? user?.email ?? ''} type="email" readOnly />
            <small>Email được dùng để đăng nhập nên không thể thay đổi tại đây.</small>
          </label>
          <label className="field">
            <span>Số điện thoại <small>(không bắt buộc)</small></span>
            <input value={phone} onChange={(event: ChangeEvent<HTMLInputElement>) => setPhone(event.target.value)} type="tel" autoComplete="tel" maxLength={30} placeholder="Ví dụ: 090 123 4567" />
          </label>
          <div className="form-actions">
            <button className="button button-primary" type="submit" disabled={savingProfile || !profile}>
              {savingProfile ? 'Đang lưu…' : 'Lưu thay đổi'}
            </button>
          </div>
        </form>

        <form className="form-card account-card" onSubmit={handlePasswordSubmit}>
          <div className="form-heading">
            <h2>Đổi mật khẩu</h2>
            <p>Mật khẩu mới cần ít nhất 8 ký tự, có chữ và số.</p>
          </div>
          {passwordError && <ErrorMessage message={passwordError} />}
          {passwordSuccess && <p className="alert alert-success">{passwordSuccess}</p>}
          <label className="field">
            <span>Mật khẩu hiện tại</span>
            <input value={currentPassword} onChange={(event: ChangeEvent<HTMLInputElement>) => setCurrentPassword(event.target.value)} type="password" autoComplete="current-password" required />
          </label>
          <label className="field">
            <span>Mật khẩu mới</span>
            <input value={newPassword} onChange={(event: ChangeEvent<HTMLInputElement>) => setNewPassword(event.target.value)} type="password" autoComplete="new-password" minLength={8} maxLength={72} required />
          </label>
          <label className="field">
            <span>Xác nhận mật khẩu mới</span>
            <input value={confirmPassword} onChange={(event: ChangeEvent<HTMLInputElement>) => setConfirmPassword(event.target.value)} type="password" autoComplete="new-password" minLength={8} maxLength={72} required />
          </label>
          <div className="form-actions">
            <button className="button button-secondary" type="submit" disabled={savingPassword}>
              {savingPassword ? 'Đang đổi…' : 'Đổi mật khẩu'}
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
