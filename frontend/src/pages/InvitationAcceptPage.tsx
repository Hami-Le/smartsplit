import { useState } from 'react'
import { acceptInvitation } from '../api/groupApi'
import { ApiError } from '../api/client'
import { ErrorMessage } from '../components/ErrorMessage'
import { navigate } from '../router'

export function InvitationAcceptPage({ token }: { token: string }) {
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [acceptedGroup, setAcceptedGroup] = useState<{ id: number; name: string } | null>(null)

  const handleAccept = async () => {
    setSubmitting(true)
    setError('')
    try {
      const result = await acceptInvitation(token)
      setAcceptedGroup({ id: result.groupId, name: result.groupName })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể chấp nhận lời mời')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="narrow-page">
      <div className="form-card form-card-wide centered-card">
        <span className="empty-icon">✉</span>
        <p className="eyebrow">LỜI MỜI THAM GIA NHÓM</p>
        {acceptedGroup ? (
          <>
            <h1>Đã tham gia {acceptedGroup.name}</h1>
            <p>Bạn đã được thêm vào nhóm với vai trò thành viên.</p>
            <button className="button button-primary" type="button" onClick={() => navigate(`/groups/${acceptedGroup.id}`, true)}>Mở nhóm</button>
          </>
        ) : (
          <>
            <h1>Xác nhận tham gia nhóm</h1>
            <p>Hệ thống sẽ kiểm tra email tài khoản hiện tại có khớp với người được mời hay không.</p>
            {error && <ErrorMessage message={error} />}
            <div className="form-actions centered-actions">
              <button className="button button-secondary" type="button" onClick={() => navigate('/groups')}>Để sau</button>
              <button className="button button-primary" type="button" onClick={() => void handleAccept()} disabled={submitting}>{submitting ? 'Đang xác nhận…' : 'Chấp nhận lời mời'}</button>
            </div>
          </>
        )}
      </div>
    </section>
  )
}
