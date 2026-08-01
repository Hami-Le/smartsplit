import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { listGroups, type GroupSummary } from '../api/groupApi'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'
import { navigate } from '../router'

const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' })

function roleLabel(role: GroupSummary['currentUserRole']) {
  return { OWNER: 'Chủ nhóm', ADMIN: 'Quản trị viên', MEMBER: 'Thành viên' }[role]
}

export function GroupListPage() {
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    listGroups()
      .then(setGroups)
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Không thể tải danh sách nhóm'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState label="Đang tải các nhóm của bạn…" />

  return (
    <section className="page-section">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">NHÓM CỦA TÔI</p>
          <h1 className="page-title">Quản lý mọi nhóm chi phí.</h1>
        </div>
        <button className="button button-primary" type="button" onClick={() => navigate('/groups/new')}>+ Tạo nhóm</button>
      </div>

      {error && <ErrorMessage message={error} />}

      {groups.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">+</span>
          <h2>Bạn chưa có nhóm nào</h2>
          <p>Tạo nhóm đầu tiên để bắt đầu mời thành viên và ghi nhận chi phí.</p>
          <button className="button button-primary" type="button" onClick={() => navigate('/groups/new')}>Tạo nhóm đầu tiên</button>
        </div>
      ) : (
        <div className="group-grid">
          {groups.map((group) => (
            <button className="group-card" type="button" key={group.id} onClick={() => navigate(`/groups/${group.id}`)}>
              <div className="group-card-top">
                <span className="group-avatar">{group.name.charAt(0).toUpperCase()}</span>
                <span className="role-badge">{roleLabel(group.currentUserRole)}</span>
              </div>
              <h2>{group.name}</h2>
              {group.description?.trim() && <p>{group.description}</p>}
              <div className="group-card-meta">
                <span>{group.memberCount} thành viên</span>
                <span>{group.defaultCurrency}</span>
                <span>{dateFormatter.format(new Date(group.createdAt))}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </section>
  )
}
