import { useEffect, useMemo, useState } from 'react'
import { navigate } from '../router'

type BackendState = 'checking' | 'online' | 'offline'

const formatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const modules = [
  ['Nhóm & thành viên', 'Tạo nhóm, mời thành viên và phân quyền rõ ràng.'],
  ['Khoản chi', 'Ghi nhận người trả và chia tiền theo nhiều phương thức.'],
  ['Công nợ', 'Tính số dư ròng và đề xuất giao dịch thanh toán gọn hơn.'],
  ['OCR hóa đơn', 'Đọc hóa đơn và cho phép xác nhận trước khi lưu.'],
]

export function LandingPage() {
  const [backendState, setBackendState] = useState<BackendState>('checking')
  const sampleTotal = useMemo(() => formatter.format(2_300_000), [])

  useEffect(() => {
    fetch('/api/health')
      .then((response) => {
        if (!response.ok) throw new Error('Backend offline')
        setBackendState('online')
      })
      .catch(() => setBackendState('offline'))
  }, [])

  const statusText = {
    checking: 'Đang kiểm tra backend…',
    online: 'Backend đang hoạt động',
    offline: 'Chưa kết nối được backend',
  }[backendState]

  return (
    <>
      <section className="hero">
        <div>
          <div className={`status status-${backendState}`}>{statusText}</div>
          <p className="eyebrow">ĐỒ ÁN CƠ SỞ · CLIENT–SERVER · AI/OCR</p>
          <h1>Chia chi phí nhóm rõ ràng, thanh toán gọn hơn.</h1>
          <p className="lead">
            Tạo nhóm, mời thành viên và quản lý chi phí trên một hệ thống có xác thực,
            phân quyền và API riêng biệt.
          </p>
          <div className="actions">
            <button className="button button-primary" type="button" onClick={() => navigate('/register')}>Bắt đầu miễn phí</button>
            <button className="button button-secondary" type="button" onClick={() => navigate('/login')}>Tôi đã có tài khoản</button>
          </div>
        </div>

        <article className="expense-card">
          <div className="expense-head">
            <span>Quán BBQ</span>
            <strong>{sampleTotal}</strong>
          </div>
          <p>Hà Mi đã thanh toán · 23/07/2026</p>
          <div className="divider" />
          <div className="balance-row"><span>Minh cần trả</span><b>{formatter.format(750_000)}</b></div>
          <div className="balance-row"><span>Lan cần trả</span><b>{formatter.format(800_000)}</b></div>
          <div className="balance-row"><span>Hà Mi được nhận</span><b>{formatter.format(1_550_000)}</b></div>
        </article>
      </section>

      <section className="module-grid" aria-label="Các module chính">
        {modules.map(([title, description]) => (
          <article key={title} className="module-card">
            <h2>{title}</h2>
            <p>{description}</p>
          </article>
        ))}
      </section>
    </>
  )
}
