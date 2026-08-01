import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
  listCategories,
  listExpenses,
  type Category,
  type Expense,
} from '../api/expenseApi'
import {
  archiveGroup,
  getGroup,
  inviteMember,
  removeMember,
  updateGroup,
  updateMemberRole,
  type GroupDetail,
  type GroupMember,
  type Invitation,
} from '../api/groupApi'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'
import { UserAvatar } from '../components/UserAvatar'
import { navigate } from '../router'

const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})
const expenseDateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' })
const moneyFormatter = new Intl.NumberFormat('vi-VN')

type ExpenseSort = 'NEWEST' | 'OLDEST' | 'AMOUNT_DESC' | 'AMOUNT_ASC'

const normalizeSearchText = (value: string) => value
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .toLocaleLowerCase('vi-VN')

const roleLabels = {
  OWNER: 'Chủ nhóm',
  ADMIN: 'Quản trị viên',
  MEMBER: 'Thành viên',
}

export function GroupDetailPage({ groupId }: { groupId: number }) {
  const { user } = useAuth()
  const [group, setGroup] = useState<GroupDetail | null>(null)
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const [busy, setBusy] = useState(false)
  const [expenseSearch, setExpenseSearch] = useState('')
  const [expenseCategory, setExpenseCategory] = useState('')
  const [expensePayer, setExpensePayer] = useState('')
  const [expenseFrom, setExpenseFrom] = useState('')
  const [expenseTo, setExpenseTo] = useState('')
  const [expenseSort, setExpenseSort] = useState<ExpenseSort>('NEWEST')
  const [expenseFiltersOpen, setExpenseFiltersOpen] = useState(false)

  const canManage = useMemo(
    () => group?.currentUserRole === 'OWNER' || group?.currentUserRole === 'ADMIN',
    [group],
  )

  const filteredExpenses = useMemo(() => {
    const normalizedSearch = normalizeSearchText(expenseSearch.trim())
    const categoryId = expenseCategory ? Number(expenseCategory) : null
    const payerId = expensePayer ? Number(expensePayer) : null

    const result = expenses.filter((expense) => {
      const matchesSearch = !normalizedSearch || normalizeSearchText([
        expense.title,
        expense.description ?? '',
        expense.category?.name ?? '',
        ...expense.payers.map((payer) => payer.fullName),
      ].join(' ')).includes(normalizedSearch)
      const matchesCategory = categoryId === null || expense.category?.id === categoryId
      const matchesPayer = payerId === null
        || expense.payers.some((payer) => payer.userId === payerId)
      const matchesFrom = !expenseFrom || expense.expenseDate >= expenseFrom
      const matchesTo = !expenseTo || expense.expenseDate <= expenseTo
      return matchesSearch && matchesCategory && matchesPayer && matchesFrom && matchesTo
    })

    return result.sort((left, right) => {
      if (expenseSort === 'OLDEST') {
        return left.expenseDate.localeCompare(right.expenseDate)
          || left.createdAt.localeCompare(right.createdAt)
      }
      if (expenseSort === 'AMOUNT_DESC') return right.totalAmount - left.totalAmount
      if (expenseSort === 'AMOUNT_ASC') return left.totalAmount - right.totalAmount
      return right.expenseDate.localeCompare(left.expenseDate)
        || right.createdAt.localeCompare(left.createdAt)
    })
  }, [expenseCategory, expenseFrom, expensePayer, expenseSearch, expenseSort, expenseTo, expenses])

  const filteredExpenseTotal = useMemo(
    () => filteredExpenses.reduce((sum, expense) => sum + expense.totalAmount, 0),
    [filteredExpenses],
  )

  const activeExpenseFilterCount = [
    expenseCategory,
    expensePayer,
    expenseFrom,
    expenseTo,
  ].filter(Boolean).length

  const hasExpenseFilters = Boolean(expenseSearch.trim() || activeExpenseFilterCount)

  const loadGroup = async () => {
    setError('')
    try {
      const data = await getGroup(groupId)
      setGroup(data)
      setName(data.name)
      setDescription(data.description ?? '')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể tải thông tin nhóm')
    } finally {
      setLoading(false)
    }
  }

  const loadExpenseData = async () => {
    try {
      const [expenseData, categoryData] = await Promise.all([
        listExpenses(groupId),
        listCategories(),
      ])
      setExpenses(expenseData)
      setCategories(categoryData)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể tải khoản chi')
    }
  }

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      await Promise.all([loadGroup(), loadExpenseData()])
      setLoading(false)
    }
    void load()
  }, [groupId])
  const clearExpenseFilters = () => {
    setExpenseSearch('')
    setExpenseCategory('')
    setExpensePayer('')
    setExpenseFrom('')
    setExpenseTo('')
    setExpenseSort('NEWEST')
  }

  const handleUpdate = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const updated = await updateGroup(groupId, { name, description, defaultCurrency: 'VND' })
      setGroup(updated)
      setEditing(false)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể cập nhật nhóm')
    } finally {
      setBusy(false)
    }
  }

  const handleInvite = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    setInvitation(null)
    try {
      const created = await inviteMember(groupId, inviteEmail)
      setInvitation(created)
      setInviteEmail('')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể tạo lời mời')
    } finally {
      setBusy(false)
    }
  }

  const handleRoleChange = async (member: GroupMember, role: 'ADMIN' | 'MEMBER') => {
    setBusy(true)
    setError('')
    try {
      await updateMemberRole(groupId, member.userId, role)
      await loadGroup()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể cập nhật vai trò')
    } finally {
      setBusy(false)
    }
  }

  const handleRemove = async (member: GroupMember) => {
    if (!window.confirm(`Xóa ${member.fullName} khỏi nhóm?`)) return
    setBusy(true)
    setError('')
    try {
      await removeMember(groupId, member.userId)
      await loadGroup()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể xóa thành viên')
    } finally {
      setBusy(false)
    }
  }

  const handleArchive = async () => {
    if (!window.confirm('Lưu trữ nhóm này? Nhóm sẽ không còn xuất hiện trong danh sách.')) return
    setBusy(true)
    setError('')
    try {
      await archiveGroup(groupId)
      navigate('/groups', true)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể lưu trữ nhóm')
      setBusy(false)
    }
  }

  const copyInvitation = async () => {
    if (!invitation) return
    const link = `${window.location.origin}${invitation.invitationPath}`
    await navigator.clipboard.writeText(link)
  }

  if (loading) return <LoadingState label="Đang tải thông tin nhóm…" />
  if (!group) {
    return (
      <section className="narrow-page">
        <ErrorMessage message={error || 'Không tìm thấy nhóm'} />
        <button className="button button-secondary" type="button" onClick={() => navigate('/groups')}>Quay lại danh sách</button>
      </section>
    )
  }

  return (
    <section className="page-section">
      <button className="back-button" type="button" onClick={() => navigate('/groups')}>← Nhóm của tôi</button>
      {error && <ErrorMessage message={error} />}

      <div className="group-detail-hero">
        <div className="group-avatar group-avatar-large">{group.name.charAt(0).toUpperCase()}</div>
        <div className="group-detail-title">
          <span className="role-badge">{roleLabels[group.currentUserRole]}</span>
          <h1>{group.name}</h1>
          {group.description?.trim() && <p>{group.description}</p>}
          <div className="group-facts">
            <span>{group.members.length} thành viên</span>
            <span>{group.defaultCurrency}</span>
            <span>Tạo {dateFormatter.format(new Date(group.createdAt))}</span>
          </div>
        </div>
        <div className="detail-actions">
          <button className="button button-primary button-small" type="button" onClick={() => navigate(`/groups/${groupId}/expenses/new`)}>+ Thêm khoản chi</button>
          <button className="button button-secondary button-small" type="button" onClick={() => navigate(`/groups/${groupId}/dashboard`)}>Dashboard</button>
          <button className="button button-secondary button-small" type="button" onClick={() => navigate(`/groups/${groupId}/balances`)}>Công nợ</button>
          {canManage && <button className="button button-secondary button-small" type="button" onClick={() => setEditing((value) => !value)}>Sửa nhóm</button>}
          {group.currentUserRole === 'OWNER' && <button className="button button-danger button-small" type="button" onClick={handleArchive} disabled={busy}>Lưu trữ</button>}
        </div>
      </div>

      {editing && (
        <form className="panel" onSubmit={handleUpdate}>
          <div className="panel-heading"><div><h2>Chỉnh sửa nhóm</h2><p>Cập nhật tên và mô tả hiển thị.</p></div></div>
          <div className="form-grid-two">
            <label className="field"><span>Tên nhóm</span><input value={name} onChange={(event: ChangeEvent<HTMLInputElement>) => setName(event.target.value)} required /></label>
            <label className="field"><span>Tiền tệ</span><input value="VND" readOnly /></label>
          </div>
          <label className="field"><span>Mô tả</span><textarea rows={3} value={description} onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setDescription(event.target.value)} /></label>
          <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setEditing(false)}>Hủy</button><button className="button button-primary" type="submit" disabled={busy}>Lưu thay đổi</button></div>
        </form>
      )}

      <section className="panel expense-panel">
        <div className="panel-heading expense-panel-heading">
          <div><h2>Khoản chi</h2></div>
          <div className="expense-stat">
            <span>{filteredExpenses.length}{hasExpenseFilters ? ` / ${expenses.length}` : ''} khoản</span>
            <strong>{moneyFormatter.format(filteredExpenseTotal)} đ</strong>
          </div>
        </div>
        <div className="expense-search-row">
          <div className="expense-search-box">
            <span aria-hidden="true">⌕</span>
            <input
              aria-label="Tìm khoản chi"
              value={expenseSearch}
              onChange={(event: ChangeEvent<HTMLInputElement>) => setExpenseSearch(event.target.value)}
              placeholder="Tìm khoản chi…"
            />
          </div>
          <button
            className={`button button-secondary button-small${expenseFiltersOpen ? ' is-active' : ''}`}
            type="button"
            aria-expanded={expenseFiltersOpen}
            onClick={() => setExpenseFiltersOpen((value) => !value)}
          >
            Bộ lọc{activeExpenseFilterCount > 0 ? ` (${activeExpenseFilterCount})` : ''}
          </button>
          <select
            className="expense-sort-select"
            aria-label="Sắp xếp khoản chi"
            value={expenseSort}
            onChange={(event: ChangeEvent<HTMLSelectElement>) => setExpenseSort(event.target.value as ExpenseSort)}
          >
            <option value="NEWEST">Mới nhất</option>
            <option value="OLDEST">Cũ nhất</option>
            <option value="AMOUNT_DESC">Tiền cao nhất</option>
            <option value="AMOUNT_ASC">Tiền thấp nhất</option>
          </select>
          <button className="button button-primary button-small" type="button" onClick={() => navigate(`/groups/${groupId}/expenses/new`)}>Thêm khoản chi</button>
        </div>

        {expenseFiltersOpen && (
          <div className="expense-filter-panel">
            <label className="field">
              <span>Từ ngày</span>
              <input type="date" max={expenseTo || undefined} value={expenseFrom} onChange={(event: ChangeEvent<HTMLInputElement>) => setExpenseFrom(event.target.value)} />
            </label>
            <label className="field">
              <span>Đến ngày</span>
              <input type="date" min={expenseFrom || undefined} value={expenseTo} onChange={(event: ChangeEvent<HTMLInputElement>) => setExpenseTo(event.target.value)} />
            </label>
            <label className="field">
              <span>Danh mục</span>
              <select value={expenseCategory} onChange={(event: ChangeEvent<HTMLSelectElement>) => setExpenseCategory(event.target.value)}>
                <option value="">Tất cả danh mục</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </label>
            <label className="field">
              <span>Người thanh toán</span>
              <select value={expensePayer} onChange={(event: ChangeEvent<HTMLSelectElement>) => setExpensePayer(event.target.value)}>
                <option value="">Tất cả thành viên</option>
                {group.members.map((member) => <option key={member.userId} value={member.userId}>{member.fullName}</option>)}
              </select>
            </label>
            <button className="button button-ghost button-small" type="button" disabled={!hasExpenseFilters && expenseSort === 'NEWEST'} onClick={clearExpenseFilters}>Xóa tất cả</button>
          </div>
        )}

        {hasExpenseFilters && (
          <div className="expense-filter-chips" aria-label="Bộ lọc đang áp dụng">
            {expenseSearch.trim() && <button type="button" onClick={() => setExpenseSearch('')}>“{expenseSearch.trim()}” <span>×</span></button>}
            {expenseFrom && <button type="button" onClick={() => setExpenseFrom('')}>Từ {expenseDateFormatter.format(new Date(`${expenseFrom}T00:00:00`))} <span>×</span></button>}
            {expenseTo && <button type="button" onClick={() => setExpenseTo('')}>Đến {expenseDateFormatter.format(new Date(`${expenseTo}T00:00:00`))} <span>×</span></button>}
            {expenseCategory && <button type="button" onClick={() => setExpenseCategory('')}>{categories.find((category) => category.id === Number(expenseCategory))?.name} <span>×</span></button>}
            {expensePayer && <button type="button" onClick={() => setExpensePayer('')}>{group.members.find((member) => member.userId === Number(expensePayer))?.fullName} <span>×</span></button>}
            <button className="expense-clear-filters" type="button" onClick={clearExpenseFilters}>Xóa tất cả</button>
          </div>
        )}

        {expenses.length === 0 ? (
          <div className="expense-empty">
            <span>₫</span>
            <h3>Chưa có khoản chi</h3>
            <p>Thêm khoản chi đầu tiên để bắt đầu tính phần tiền của từng người.</p>
          </div>
        ) : filteredExpenses.length === 0 ? (
          <div className="expense-empty">
            <span>⌕</span>
            <h3>Không tìm thấy khoản chi phù hợp</h3>
            <p>Hãy thử thay đổi từ khóa hoặc bộ lọc.</p>
            <button className="button button-secondary button-small" type="button" onClick={clearExpenseFilters}>Xóa bộ lọc</button>
          </div>
        ) : (
          <div className="expense-list">
            {filteredExpenses.map((expense) => (
              <button className="expense-row" type="button" key={expense.id} onClick={() => navigate(`/groups/${groupId}/expenses/${expense.id}`)}>
                <span className="expense-date-box"><b>{new Date(`${expense.expenseDate}T00:00:00`).getDate()}</b><small>{new Date(`${expense.expenseDate}T00:00:00`).toLocaleDateString('vi-VN', { month: 'short' })}</small></span>
                <span className="expense-row-main"><strong>{expense.title}</strong><small>{expense.category?.name ?? 'Chưa phân loại'} · {expenseDateFormatter.format(new Date(`${expense.expenseDate}T00:00:00`))} · {expense.payers.map((payer) => payer.fullName).join(', ')} đã trả</small></span>
                <span className="expense-row-amount"><strong>{moneyFormatter.format(expense.totalAmount)} đ</strong><small>{expense.shares.length} người tham gia</small></span>
                <span className="expense-row-arrow">→</span>
              </button>
            ))}
          </div>
        )}
      </section>

      <div className="detail-grid">
        <section className="panel">
          <div className="panel-heading"><div><h2>Thành viên</h2></div></div>
          <div className="member-list">
            {group.members.map((member) => {
              const isCurrentUser = member.userId === user?.id
              const canEditRole = group.currentUserRole === 'OWNER' && member.role !== 'OWNER'
              const canRemove = canManage && member.role !== 'OWNER' && !isCurrentUser
              return (
                <div className="member-row" key={member.membershipId}>
                  <UserAvatar fullName={member.fullName} avatarUrl={member.avatarUrl} size="medium" />
                  <div className="member-main"><strong>{member.fullName}{isCurrentUser ? ' (Bạn)' : ''}</strong><span>{member.email}</span></div>
                  {canEditRole ? (
                    <select value={member.role} onChange={(event: ChangeEvent<HTMLSelectElement>) => void handleRoleChange(member, event.target.value as 'ADMIN' | 'MEMBER')} disabled={busy}><option value="MEMBER">Thành viên</option><option value="ADMIN">Quản trị viên</option></select>
                  ) : <span className="role-badge">{roleLabels[member.role]}</span>}
                  {canRemove && <button className="icon-button danger-text" type="button" onClick={() => void handleRemove(member)} disabled={busy}>Xóa</button>}
                </div>
              )
            })}
          </div>
        </section>

        <aside className="panel">
          <div className="panel-heading"><div><h2>Mời thành viên</h2><p>Tạo link có hiệu lực 7 ngày cho đúng email được mời.</p></div></div>
          {canManage ? (
            <>
              <form onSubmit={handleInvite}>
                <label className="field"><span>Email thành viên</span><input type="email" value={inviteEmail} onChange={(event: ChangeEvent<HTMLInputElement>) => setInviteEmail(event.target.value)} placeholder="minh@example.com" required /></label>
                <button className="button button-primary button-block" type="submit" disabled={busy}>{busy ? 'Đang xử lý…' : 'Tạo link mời'}</button>
              </form>
              {invitation && (
                <div className="invitation-box">
                  <strong>Link mời đã tạo</strong>
                  <p>Gửi link này cho <b>{invitation.email}</b>. Người nhận phải đăng nhập đúng email.</p>
                  <code>{window.location.origin}{invitation.invitationPath}</code>
                  <button className="button button-secondary button-block" type="button" onClick={() => void copyInvitation()}>Sao chép link</button>
                </div>
              )}
            </>
          ) : <p className="muted">Chỉ chủ nhóm hoặc quản trị viên mới có thể mời thành viên.</p>}
        </aside>
      </div>
    </section>
  )
}
