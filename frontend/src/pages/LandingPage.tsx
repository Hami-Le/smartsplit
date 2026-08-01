import { useMemo } from 'react'
import { navigate } from '../router'

const formatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const modules = ['Nhóm & thành viên', 'Khoản chi', 'Công nợ', 'OCR hóa đơn']

export function LandingPage() {
  const sampleTotal = useMemo(() => formatter.format(2_300_000), [])

  return (
    <>
      <section className="hero">
        <div>
          <h1>Chia chi phí nhóm rõ ràng, thanh toán gọn hơn.</h1>
          <p className="lead">
            Tạo nhóm, ghi nhận khoản chi và biết chính xác ai cần trả cho ai.
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
        {modules.map((title) => (
          <article key={title} className="module-card">
            <h2>{title}</h2>
          </article>
        ))}
      </section>
    </>
  )
}
