import { useEffect, useMemo, useState } from 'react'
import { ApiError, apiDownload } from '../api/client'
import { deleteExpense, getExpense, type Expense } from '../api/expenseApi'
import { getGroup, type GroupDetail } from '../api/groupApi'
import { listReceiptAttachments, type ReceiptAttachment } from '../api/ocrApi'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'
import { UserAvatar } from '../components/UserAvatar'
import { navigate } from '../router'

const moneyFormatter = new Intl.NumberFormat('vi-VN')
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'long' })

const splitLabels = {
  EQUAL: 'Chia đều',
  PERCENTAGE: 'Chia theo phần trăm',
  EXACT: 'Chia theo số tiền',
}

export function ExpenseDetailPage({
  groupId,
  expenseId,
}: {
  groupId: number
  expenseId: number
}) {
  const { user } = useAuth()
  const [expense, setExpense] = useState<Expense | null>(null)
  const [group, setGroup] = useState<GroupDetail | null>(null)
  const [attachments, setAttachments] = useState<ReceiptAttachment[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(() => {
    const warning = sessionStorage.getItem('smartsplit.expenseWarning') ?? ''
    sessionStorage.removeItem('smartsplit.expenseWarning')
    return warning
  })

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [expenseData, groupData, attachmentData] = await Promise.all([
          getExpense(expenseId),
          getGroup(groupId),
          listReceiptAttachments(expenseId),
        ])
        if (expenseData.groupId !== groupId) throw new Error('Khoản chi không thuộc nhóm này')
        setExpense(expenseData)
        setGroup(groupData)
        setAttachments(attachmentData)
      } catch (caught) {
        setError(caught instanceof ApiError ? caught.message : 'Không thể tải khoản chi')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [expenseId, groupId])

  const canEdit = useMemo(() => {
    if (!expense || !group) return false
    return expense.createdByUserId === user?.id
      || group.currentUserRole === 'OWNER'
      || group.currentUserRole === 'ADMIN'
  }, [expense, group, user?.id])

  const handleDelete = async () => {
    if (!window.confirm('Xóa khoản chi này? Công nợ ở các bước sau sẽ không còn tính khoản chi đã xóa.')) return
    setBusy(true)
    setError('')
    try {
      await deleteExpense(expenseId)
      navigate(`/groups/${groupId}`, true)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể xóa khoản chi')
      setBusy(false)
    }
  }

  const openAttachment = async (attachment: ReceiptAttachment) => {
    setError('')
    try {
      const path = attachment.fileUrl.startsWith('/api')
        ? attachment.fileUrl.slice(4)
        : attachment.fileUrl
      const downloaded = await apiDownload(path)
      const objectUrl = URL.createObjectURL(downloaded.blob)
      window.open(objectUrl, '_blank', 'noopener,noreferrer')
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể mở ảnh hóa đơn')
    }
  }

  if (loading) return <LoadingState label="Đang tải khoản chi…" />
  if (!expense || !group) {
    return (
      <section className="narrow-page">
        <ErrorMessage message={error || 'Không tìm thấy khoản chi'} />
        <button className="button button-secondary" type="button" onClick={() => navigate(`/groups/${groupId}`)}>Quay lại nhóm</button>
      </section>
    )
  }

  return (
    <section className="page-section">
      <button className="back-button" type="button" onClick={() => navigate(`/groups/${groupId}`)}>← {group.name}</button>
      {error && <ErrorMessage message={error} />}

      <div className="expense-detail-hero panel">
        <div>
          <div className="expense-detail-meta">
            <span className="category-pill">{expense.category?.name ?? 'Chưa phân loại'}</span>
            <span>{dateFormatter.format(new Date(`${expense.expenseDate}T00:00:00`))}</span>
          </div>
          <h1>{expense.title}</h1>
          <p>{expense.description || 'Không có ghi chú.'}</p>
          <span className="muted">Tạo bởi {expense.createdByName}</span>
        </div>
        <div className="expense-detail-total">
          <span>Tổng khoản chi</span>
          <strong>{moneyFormatter.format(expense.totalAmount)} đ</strong>
          {canEdit && (
            <div className="detail-actions">
              <button className="button button-secondary button-small" type="button" onClick={() => navigate(`/groups/${groupId}/expenses/${expenseId}/edit`)}>Chỉnh sửa</button>
              <button className="button button-danger button-small" type="button" onClick={() => void handleDelete()} disabled={busy}>Xóa</button>
            </div>
          )}
        </div>
      </div>

      {attachments.length > 0 && (
        <section className="panel receipt-attachments-panel">
          <div className="panel-heading"><div><h2>Ảnh hóa đơn</h2><p>Tệp được lưu cùng kết quả OCR của khoản chi.</p></div><span>{attachments.length} tệp</span></div>
          <div className="receipt-attachment-list">
            {attachments.map((attachment) => (
              <div className="receipt-attachment-row" key={attachment.id}>
                <span className="receipt-attachment-icon">▧</span>
                <div><strong>Hóa đơn #{attachment.id}</strong><small>{attachment.fileType} · {attachment.ocrStatus.replaceAll('_', ' ')}</small></div>
                <button className="button button-secondary button-small" type="button" onClick={() => void openAttachment(attachment)}>Mở ảnh</button>
              </div>
            ))}
          </div>
        </section>
      )}

      <div className="detail-grid expense-detail-grid">
        <section className="panel">
          <div className="panel-heading"><div><h2>Người đã thanh toán</h2><p>Tổng tiền thực tế từng thành viên đã ứng.</p></div></div>
          <div className="money-person-list">
            {expense.payers.map((payer) => (
              <div className="money-person-row" key={payer.userId}>
                <UserAvatar fullName={payer.fullName} avatarUrl={payer.avatarUrl} size="medium" />
                <div><strong>{payer.fullName}</strong><span>{payer.email}</span></div>
                <b>{moneyFormatter.format(payer.amount)} đ</b>
              </div>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading"><div><h2>Phần tiền phải chịu</h2><p>{splitLabels[expense.splitType]}</p></div></div>
          <div className="money-person-list">
            {expense.shares.map((share) => (
              <div className="money-person-row" key={share.userId}>
                <UserAvatar fullName={share.fullName} avatarUrl={share.avatarUrl} size="medium" />
                <div><strong>{share.fullName}</strong><span>{share.percentage !== null ? `${share.percentage}%` : share.email}</span></div>
                <b>{moneyFormatter.format(share.amount)} đ</b>
              </div>
            ))}
          </div>
        </section>
      </div>
    </section>
  )
}
