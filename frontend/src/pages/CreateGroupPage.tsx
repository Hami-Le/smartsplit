import { useState, type ChangeEvent, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { createGroup } from '../api/groupApi'
import { ErrorMessage } from '../components/ErrorMessage'
import { navigate } from '../router'

export function CreateGroupPage() {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const group = await createGroup({ name, description, defaultCurrency: 'VND' })
      navigate(`/groups/${group.id}`, true)
    } catch (caught) {
      if (caught instanceof ApiError) {
        setError(Object.values(caught.fields)[0] || caught.message)
      } else {
        setError('Không thể tạo nhóm lúc này')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="narrow-page">
      <button className="back-button" type="button" onClick={() => navigate('/groups')}>← Quay lại danh sách</button>
      <form className="form-card form-card-wide" onSubmit={handleSubmit}>
        <div className="form-heading">
          <p className="eyebrow">NHÓM MỚI</p>
          <h1>Tạo không gian chia sẻ chi phí.</h1>
          <p>Bạn sẽ tự động trở thành chủ nhóm và có thể mời thành viên sau đó.</p>
        </div>
        {error && <ErrorMessage message={error} />}
        <label className="field">
          <span>Tên nhóm</span>
          <input value={name} onChange={(event: ChangeEvent<HTMLInputElement>) => setName(event.target.value)} placeholder="Du lịch Đà Lạt" maxLength={150} required autoFocus />
        </label>
        <label className="field">
          <span>Mô tả</span>
          <textarea value={description} onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setDescription(event.target.value)} placeholder="Chi phí chuyến đi tháng 8" maxLength={500} rows={4} />
        </label>
        <label className="field">
          <span>Đơn vị tiền mặc định</span>
          <input value="VND" readOnly />
        </label>
        <div className="form-actions">
          <button className="button button-secondary" type="button" onClick={() => navigate('/groups')}>Hủy</button>
          <button className="button button-primary" type="submit" disabled={submitting}>{submitting ? 'Đang tạo…' : 'Tạo nhóm'}</button>
        </div>
      </form>
    </section>
  )
}
