import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { listCategories, type Category } from '../api/expenseApi'
import {
  createPersonalExpense,
  deleteMonthlyBudget,
  deletePersonalExpense,
  getPersonalFinance,
  setMonthlyBudget,
  updatePersonalExpense,
  type PersonalExpense,
  type PersonalFinanceSummary,
} from '../api/personalFinanceApi'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'

const moneyFormatter = new Intl.NumberFormat('vi-VN')
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' })
const monthFormatter = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' })

const categoryIcons: Record<string, string> = {
  utensils: '🍜',
  car: '🛵',
  hotel: '🏠',
  'shopping-bag': '🛍️',
  gamepad: '🎮',
  'circle-ellipsis': '•••',
}

function currentMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function defaultDate(month: string): string {
  const today = new Date()
  const todayValue = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  return todayValue.startsWith(month) ? todayValue : `${month}-01`
}

function formatMoney(value: number): string {
  return `${moneyFormatter.format(value)} đ`
}

function categoryIcon(icon: string | null): string {
  return icon ? categoryIcons[icon] ?? '₫' : '₫'
}

export function PersonalFinancePage() {
  const [month, setMonth] = useState(currentMonth)
  const [summary, setSummary] = useState<PersonalFinanceSummary | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [budgetInput, setBudgetInput] = useState('')
  const [editing, setEditing] = useState<PersonalExpense | null>(null)
  const [title, setTitle] = useState('')
  const [amount, setAmount] = useState('')
  const [expenseDate, setExpenseDate] = useState(() => defaultDate(currentMonth()))
  const [categoryId, setCategoryId] = useState('')
  const [note, setNote] = useState('')

  const monthLabel = useMemo(
    () => monthFormatter.format(new Date(`${month}-01T00:00:00`)),
    [month],
  )
  const monthEndDate = useMemo(() => {
    const [year, monthNumber] = month.split('-').map(Number)
    const lastDay = new Date(year, monthNumber, 0).getDate()
    return `${month}-${String(lastDay).padStart(2, '0')}`
  }, [month])

  const load = async (selectedMonth: string) => {
    setLoading(true)
    setError('')
    try {
      const data = await getPersonalFinance(selectedMonth)
      setSummary(data)
      setBudgetInput(data.budgetAmount > 0 ? String(data.budgetAmount) : '')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể tải sổ chi tiêu cá nhân')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load(month)
  }, [month])

  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Không thể tải danh mục'))
  }, [])

  const resetForm = () => {
    setEditing(null)
    setTitle('')
    setAmount('')
    setExpenseDate(defaultDate(month))
    setCategoryId('')
    setNote('')
  }

  const handleExpenseSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const normalizedAmount = Number(amount)
    if (!Number.isSafeInteger(normalizedAmount) || normalizedAmount <= 0) {
      setError('Số tiền phải là số nguyên lớn hơn 0')
      return
    }
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const input = {
        title: title.trim(),
        amount: normalizedAmount,
        expenseDate,
        categoryId: categoryId ? Number(categoryId) : null,
        note: note.trim(),
      }
      if (editing) {
        await updatePersonalExpense(editing.id, input)
        setSuccess('Đã cập nhật khoản chi')
      } else {
        await createPersonalExpense(input)
        setSuccess('Đã ghi khoản chi mới')
      }
      resetForm()
      await load(month)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể lưu khoản chi')
    } finally {
      setBusy(false)
    }
  }

  const handleEdit = (expense: PersonalExpense) => {
    setEditing(expense)
    setTitle(expense.title)
    setAmount(String(expense.amount))
    setExpenseDate(expense.expenseDate)
    setCategoryId(expense.category ? String(expense.category.id) : '')
    setNote(expense.note ?? '')
    setError('')
    setSuccess('')
    window.scrollTo({ top: 360, behavior: 'smooth' })
  }

  const handleDelete = async (expense: PersonalExpense) => {
    if (!window.confirm(`Xóa khoản chi “${expense.title}”?`)) return
    setBusy(true)
    setError('')
    try {
      await deletePersonalExpense(expense.id)
      if (editing?.id === expense.id) resetForm()
      setSuccess('Đã xóa khoản chi')
      await load(month)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể xóa khoản chi')
    } finally {
      setBusy(false)
    }
  }

  const handleBudgetSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const normalizedBudget = Number(budgetInput)
    if (!Number.isSafeInteger(normalizedBudget) || normalizedBudget <= 0) {
      setError('Ngân sách phải là số nguyên lớn hơn 0')
      return
    }
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const data = await setMonthlyBudget(month, normalizedBudget)
      setSummary(data)
      setSuccess('Đã cập nhật ngân sách tháng')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể cập nhật ngân sách')
    } finally {
      setBusy(false)
    }
  }

  const handleDeleteBudget = async () => {
    if (!window.confirm(`Xóa ngân sách ${monthLabel}?`)) return
    setBusy(true)
    setError('')
    try {
      const data = await deleteMonthlyBudget(month)
      setSummary(data)
      setBudgetInput('')
      setSuccess('Đã xóa ngân sách tháng')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể xóa ngân sách')
    } finally {
      setBusy(false)
    }
  }

  if (loading && !summary) return <LoadingState label="Đang tải sổ chi tiêu…" />

  return (
    <section className="page-section personal-page">
      <div className="personal-hero">
        <div>
          <span className="role-badge">CHI TIÊU CÁ NHÂN</span>
          <h1>Sổ chi tiêu</h1>
          <p>Ghi lại các khoản chi, xem tiền đi đâu và giữ chi tiêu trong ngân sách tháng.</p>
        </div>
        <label className="personal-month-picker">
          <span>Tháng đang xem</span>
          <input
            type="month"
            value={month}
            required
            onChange={(event) => {
              const nextMonth = event.target.value
              setMonth(nextMonth)
              setEditing(null)
              setTitle('')
              setAmount('')
              setExpenseDate(defaultDate(nextMonth))
              setCategoryId('')
              setNote('')
              setSuccess('')
            }}
          />
        </label>
      </div>

      {error && <ErrorMessage message={error} />}
      {success && <div className="alert alert-success">{success}</div>}

      {summary && (
        <>
          <div className="dashboard-stat-grid personal-stat-grid">
            <article className="dashboard-stat-card">
              <span>Đã chi</span>
              <strong>{formatMoney(summary.totalSpent)}</strong>
              <small>{summary.expenseCount} khoản trong {monthLabel}</small>
            </article>
            <article className="dashboard-stat-card">
              <span>Ngân sách</span>
              <strong>{summary.budgetAmount > 0 ? formatMoney(summary.budgetAmount) : 'Chưa đặt'}</strong>
              <small>{summary.budgetAmount > 0 ? `${summary.usagePercentage}% đã sử dụng` : 'Đặt hạn mức để theo dõi'}</small>
            </article>
            <article className={`dashboard-stat-card${summary.overBudget ? ' personal-stat-danger' : ''}`}>
              <span>{summary.remainingAmount < 0 ? 'Vượt ngân sách' : 'Còn lại'}</span>
              <strong>{summary.budgetAmount > 0 ? formatMoney(Math.abs(summary.remainingAmount)) : '—'}</strong>
              <small>{summary.overBudget ? 'Cần điều chỉnh chi tiêu' : 'Có thể chi trong tháng'}</small>
            </article>
          </div>

          <section className="panel personal-budget-panel">
            <div className="personal-budget-copy">
              <div className="panel-heading">
                <div><h2>Ngân sách {monthLabel}</h2><p>Đặt một hạn mức chung cho toàn bộ khoản chi cá nhân trong tháng.</p></div>
              </div>
              <div className={`personal-budget-progress${summary.overBudget ? ' is-over' : ''}`}>
                <span style={{ width: `${Math.min(100, summary.usagePercentage)}%` }} />
              </div>
              <small className="personal-budget-caption">
                {summary.budgetAmount > 0
                  ? `${formatMoney(summary.totalSpent)} / ${formatMoney(summary.budgetAmount)}`
                  : 'Chưa có ngân sách cho tháng này'}
              </small>
            </div>
            <form className="personal-budget-form" onSubmit={handleBudgetSubmit}>
              <label className="field"><span>Hạn mức (VND)</span><input type="number" min="1" step="1" value={budgetInput} onChange={(event) => setBudgetInput(event.target.value)} placeholder="5.000.000" required /></label>
              <button className="button button-primary" type="submit" disabled={busy}>Lưu ngân sách</button>
              {summary.budgetAmount > 0 && <button className="button button-ghost danger-text" type="button" disabled={busy} onClick={() => void handleDeleteBudget()}>Xóa</button>}
            </form>
          </section>

          <div className="personal-main-grid">
            <section className="panel">
              <div className="panel-heading"><div><h2>Chi theo danh mục</h2><p>Tỷ trọng các khoản chi trong {monthLabel}.</p></div></div>
              {summary.categoryBreakdown.length === 0 ? (
                <div className="personal-mini-empty"><span>◎</span><p>Thêm khoản chi để xem thống kê danh mục.</p></div>
              ) : (
                <div className="category-report-list">
                  {summary.categoryBreakdown.map((category, index) => (
                    <article className="category-report-row" key={category.categoryId ?? 'uncategorized'}>
                      <span className={`category-report-icon report-color-${index % 6}`}>{categoryIcon(category.icon)}</span>
                      <div className="category-report-main">
                        <div><strong>{category.categoryName}</strong><span>{formatMoney(category.amount)}</span></div>
                        <div className="category-progress"><span className={`report-color-bg-${index % 6}`} style={{ width: `${category.percentage}%` }} /></div>
                        <small>{category.expenseCount} khoản · {category.percentage}%</small>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>

            <section className="panel personal-expense-form-panel">
              <div className="panel-heading">
                <div><h2>{editing ? 'Sửa khoản chi' : 'Ghi khoản chi'}</h2><p>{editing ? 'Cập nhật thông tin giao dịch đã chọn.' : 'Thêm một giao dịch cá nhân mới.'}</p></div>
                {editing && <button className="text-button" type="button" onClick={resetForm}>Hủy sửa</button>}
              </div>
              <form onSubmit={handleExpenseSubmit}>
                <label className="field"><span>Tên khoản chi</span><input value={title} maxLength={180} onChange={(event) => setTitle(event.target.value)} placeholder="Ví dụ: Ăn trưa" required /></label>
                <div className="personal-form-row">
                  <label className="field"><span>Số tiền (VND)</span><input type="number" min="1" step="1" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="50.000" required /></label>
                  <label className="field"><span>Ngày chi</span><input type="date" min={`${month}-01`} max={monthEndDate} value={expenseDate} onChange={(event) => setExpenseDate(event.target.value)} required /></label>
                </div>
                <label className="field"><span>Danh mục</span><select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}><option value="">Chưa phân loại</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
                <label className="field"><span>Ghi chú</span><textarea rows={3} maxLength={1000} value={note} onChange={(event) => setNote(event.target.value)} placeholder="Thông tin thêm (không bắt buộc)" /></label>
                <button className="button button-primary button-block" type="submit" disabled={busy}>{busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Thêm khoản chi'}</button>
              </form>
            </section>
          </div>

          <section className="panel personal-expense-list-panel">
            <div className="panel-heading"><div><h2>Giao dịch trong tháng</h2><p>{summary.expenseCount} khoản · {formatMoney(summary.totalSpent)}</p></div></div>
            {summary.expenses.length === 0 ? (
              <div className="expense-empty"><span>₫</span><h3>Chưa có khoản chi</h3><p>Ghi khoản chi đầu tiên để bắt đầu theo dõi tháng này.</p></div>
            ) : (
              <div className="personal-expense-list">
                {summary.expenses.map((expense) => (
                  <article className="personal-expense-row" key={expense.id}>
                    <span className="personal-expense-icon">{categoryIcon(expense.category?.icon ?? null)}</span>
                    <div className="personal-expense-main">
                      <strong>{expense.title}</strong>
                      <small>{expense.category?.name ?? 'Chưa phân loại'} · {dateFormatter.format(new Date(`${expense.expenseDate}T00:00:00`))}{expense.note ? ` · ${expense.note}` : ''}</small>
                    </div>
                    <b>{formatMoney(expense.amount)}</b>
                    <div className="personal-expense-actions">
                      <button className="text-button" type="button" disabled={busy} onClick={() => handleEdit(expense)}>Sửa</button>
                      <button className="text-button danger-text" type="button" disabled={busy} onClick={() => void handleDelete(expense)}>Xóa</button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </section>
  )
}
