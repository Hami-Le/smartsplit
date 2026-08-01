import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
  downloadReport,
  getDashboard,
  type Dashboard,
} from '../api/reportApi'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'
import { navigate } from '../router'

const moneyFormatter = new Intl.NumberFormat('vi-VN')
const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' })

function formatMoney(value: number): string {
  return `${moneyFormatter.format(value)} đ`
}

export function DashboardPage({ groupId }: { groupId: number }) {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [loading, setLoading] = useState(true)
  const [busyFormat, setBusyFormat] = useState<'xlsx' | 'pdf' | null>(null)
  const [error, setError] = useState('')

  const load = async (filters: { from?: string; to?: string } = {}) => {
    setLoading(true)
    setError('')
    try {
      const data = await getDashboard(groupId, filters)
      setDashboard(data)
      setFrom(data.from)
      setTo(data.to)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể tải dashboard')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [groupId])

  const maxMonthlyAmount = useMemo(
    () => Math.max(1, ...(dashboard?.monthlyTrend.map((item) => item.amount) ?? [1])),
    [dashboard],
  )

  const topPayer = useMemo(
    () => dashboard?.memberSpending.reduce(
      (current, member) => !current || member.paidAmount > current.paidAmount ? member : current,
      undefined as Dashboard['memberSpending'][number] | undefined,
    ),
    [dashboard],
  )

  const topConsumer = useMemo(
    () => dashboard?.memberSpending.reduce(
      (current, member) => !current || member.shareAmount > current.shareAmount ? member : current,
      undefined as Dashboard['memberSpending'][number] | undefined,
    ),
    [dashboard],
  )

  const handleFilter = async (event: FormEvent) => {
    event.preventDefault()
    await load({ from, to })
  }

  const handleExport = async (format: 'xlsx' | 'pdf') => {
    setBusyFormat(format)
    setError('')
    try {
      await downloadReport(groupId, format, { from, to })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể xuất báo cáo')
    } finally {
      setBusyFormat(null)
    }
  }

  if (loading && !dashboard) return <LoadingState label="Đang tổng hợp dữ liệu chi tiêu…" />
  if (!dashboard) {
    return (
      <section className="narrow-page">
        <ErrorMessage message={error || 'Không thể tải dashboard'} />
        <button className="button button-secondary" type="button" onClick={() => navigate(`/groups/${groupId}`)}>Quay lại nhóm</button>
      </section>
    )
  }

  return (
    <section className="page-section dashboard-page">
      <button className="back-button" type="button" onClick={() => navigate(`/groups/${groupId}`)}>← {dashboard.groupName}</button>
      {error && <ErrorMessage message={error} />}

      <div className="dashboard-hero">
        <div>
          <span className="role-badge">DASHBOARD NHÓM</span>
          <h1>Phân tích chi tiêu</h1>
          <p>Theo dõi xu hướng, danh mục, thành viên và công nợ trong cùng một báo cáo.</p>
        </div>
        <div className="dashboard-export-actions">
          <button className="button button-secondary" type="button" disabled={busyFormat !== null} onClick={() => void handleExport('xlsx')}>
            {busyFormat === 'xlsx' ? 'Đang tạo Excel…' : 'Xuất Excel'}
          </button>
          <button className="button button-primary" type="button" disabled={busyFormat !== null} onClick={() => void handleExport('pdf')}>
            {busyFormat === 'pdf' ? 'Đang tạo PDF…' : 'Xuất PDF'}
          </button>
        </div>
      </div>

      <form className="dashboard-filter panel" onSubmit={handleFilter}>
        <label className="field"><span>Từ ngày</span><input type="date" value={from} onChange={(event) => setFrom(event.target.value)} required /></label>
        <label className="field"><span>Đến ngày</span><input type="date" value={to} onChange={(event) => setTo(event.target.value)} required /></label>
        <button className="button button-primary" type="submit" disabled={loading}>{loading ? 'Đang lọc…' : 'Áp dụng'}</button>
        <span className="dashboard-period">{dateFormatter.format(new Date(`${dashboard.from}T00:00:00`))} – {dateFormatter.format(new Date(`${dashboard.to}T00:00:00`))}</span>
      </form>

      <div className="dashboard-stat-grid">
        <article className="dashboard-stat-card">
          <span>Tổng chi</span>
          <strong>{formatMoney(dashboard.totalExpense)}</strong>
          <small>{dashboard.expenseCount} khoản trong kỳ</small>
        </article>
        <article className="dashboard-stat-card">
          <span>Chi trung bình</span>
          <strong>{formatMoney(dashboard.averageExpense)}</strong>
          <small>Mỗi khoản chi</small>
        </article>
        <article className="dashboard-stat-card">
          <span>Khoản lớn nhất</span>
          <strong>{formatMoney(dashboard.highestExpense)}</strong>
          <small>{dashboard.largestExpense?.title ?? 'Chưa có dữ liệu'}</small>
        </article>
        <article className="dashboard-stat-card">
          <span>Công nợ hiện tại</span>
          <strong>{formatMoney(dashboard.outstandingAmount)}</strong>
          <small>Đã thanh toán trong kỳ: {formatMoney(dashboard.totalSettled)}</small>
        </article>
      </div>

      <div className="dashboard-main-grid">
        <section className="panel dashboard-chart-panel">
          <div className="panel-heading">
            <div><h2>Xu hướng theo tháng</h2><p>Tổng tiền và số khoản chi phát sinh theo từng tháng.</p></div>
          </div>
          {dashboard.monthlyTrend.every((item) => item.amount === 0) ? (
            <p className="muted">Chưa có khoản chi trong khoảng thời gian này.</p>
          ) : (
            <div className="monthly-chart" role="img" aria-label="Biểu đồ chi tiêu theo tháng">
              {dashboard.monthlyTrend.map((item) => (
                <div className="monthly-column" key={item.month}>
                  <div className="monthly-value">{moneyFormatter.format(item.amount)}</div>
                  <div className="monthly-bar-track">
                    <div className="monthly-bar" style={{ height: item.amount === 0 ? '0%' : `${Math.max(4, item.amount / maxMonthlyAmount * 100)}%` }} />
                  </div>
                  <strong>{item.label}</strong>
                  <small>{item.expenseCount} khoản</small>
                </div>
              ))}
            </div>
          )}
        </section>

        <aside className="panel">
          <div className="panel-heading"><div><h2>Theo danh mục</h2><p>Những nhóm chi phí chiếm tỷ trọng cao nhất.</p></div></div>
          {dashboard.categoryBreakdown.length === 0 ? (
            <p className="muted">Chưa có dữ liệu danh mục.</p>
          ) : (
            <div className="category-report-list">
              {dashboard.categoryBreakdown.map((category, index) => (
                <article className="category-report-row" key={category.categoryId ?? 'uncategorized'}>
                  <span className={`category-report-icon report-color-${index % 6}`}>{category.icon || '•'}</span>
                  <div className="category-report-main">
                    <div><strong>{category.categoryName}</strong><span>{formatMoney(category.amount)}</span></div>
                    <div className="category-progress"><span className={`report-color-bg-${index % 6}`} style={{ width: `${category.percentage}%` }} /></div>
                    <small>{category.expenseCount} khoản · {category.percentage}%</small>
                  </div>
                </article>
              ))}
            </div>
          )}
        </aside>
      </div>

      <section className="panel">
        <div className="panel-heading">
          <div><h2>Chi tiêu theo thành viên</h2><p>So sánh số tiền đã trả và phần chi phí thực tế mỗi người phải chịu.</p></div>
          <div className="dashboard-leaders">
            <span>Trả nhiều nhất: <b>{topPayer?.fullName ?? '—'}</b></span>
            <span>Chi nhiều nhất: <b>{topConsumer?.fullName ?? '—'}</b></span>
          </div>
        </div>
        <div className="balance-table-wrap">
          <table className="balance-table">
            <thead><tr><th>Thành viên</th><th>Đã trả</th><th>Phải chịu</th><th>Tỷ trọng</th><th>So sánh</th></tr></thead>
            <tbody>
              {dashboard.memberSpending.map((member) => {
                const difference = member.paidAmount - member.shareAmount
                return (
                  <tr key={member.userId}>
                    <td><span className="balance-person"><span className="avatar-small">{member.fullName.charAt(0).toUpperCase()}</span><span><b>{member.fullName}</b><small>{member.email}</small></span></span></td>
                    <td>{formatMoney(member.paidAmount)}</td>
                    <td>{formatMoney(member.shareAmount)}</td>
                    <td>{member.sharePercentage}%</td>
                    <td><strong className={difference > 0 ? 'balance-positive' : difference < 0 ? 'balance-negative' : 'balance-zero'}>{difference > 0 ? '+' : ''}{formatMoney(difference)}</strong></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div><h2>Khoản chi gần đây</h2><p>Sáu khoản mới nhất trong khoảng thời gian đang xem.</p></div>
          <button className="button button-secondary button-small" type="button" onClick={() => navigate(`/groups/${groupId}`)}>Xem tất cả</button>
        </div>
        {dashboard.recentExpenses.length === 0 ? (
          <p className="muted">Chưa có khoản chi trong khoảng thời gian này.</p>
        ) : (
          <div className="dashboard-recent-list">
            {dashboard.recentExpenses.map((expense) => (
              <button className="dashboard-recent-row" type="button" key={expense.id} onClick={() => navigate(`/groups/${groupId}/expenses/${expense.id}`)}>
                <span className="dashboard-recent-icon">{expense.categoryIcon || '₫'}</span>
                <span><strong>{expense.title}</strong><small>{expense.categoryName} · {dateFormatter.format(new Date(`${expense.expenseDate}T00:00:00`))} · {expense.createdByName}</small></span>
                <b>{formatMoney(expense.totalAmount)}</b>
              </button>
            ))}
          </div>
        )}
      </section>
    </section>
  )
}
