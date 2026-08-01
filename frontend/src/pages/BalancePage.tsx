import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import {
  cancelSettlement,
  createSettlement,
  getGroupBalances,
  listSettlements,
  type GroupBalance,
  type Settlement,
} from '../api/balanceApi'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'
import { navigate } from '../router'

const moneyFormatter = new Intl.NumberFormat('vi-VN')
const dateTimeFormatter = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatMoney(amount: number): string {
  return `${moneyFormatter.format(amount)} đ`
}

function toLocalDateTimeInput(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function BalancePage({ groupId }: { groupId: number }) {
  const { user } = useAuth()
  const [balance, setBalance] = useState<GroupBalance | null>(null)
  const [settlements, setSettlements] = useState<Settlement[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [payerId, setPayerId] = useState('')
  const [receiverId, setReceiverId] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [settledAt, setSettledAt] = useState(toLocalDateTimeInput(new Date()))

  const debtors = useMemo(
    () => balance?.members.filter((member) => member.balance < 0) ?? [],
    [balance],
  )
  const creditors = useMemo(
    () => balance?.members.filter((member) => member.balance > 0) ?? [],
    [balance],
  )
  const outstandingAmount = useMemo(
    () => debtors.reduce((sum, member) => sum + Math.abs(member.balance), 0),
    [debtors],
  )

  const load = async () => {
    setError('')
    try {
      const [balanceData, settlementData] = await Promise.all([
        getGroupBalances(groupId),
        listSettlements(groupId),
      ])
      setBalance(balanceData)
      setSettlements(settlementData)
      const firstSuggestion = balanceData.suggestedTransfers[0]
      if (firstSuggestion) {
        setPayerId(String(firstSuggestion.fromMemberId))
        setReceiverId(String(firstSuggestion.toMemberId))
        setAmount(String(firstSuggestion.amount))
      } else {
        setPayerId('')
        setReceiverId('')
        setAmount('')
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể tải dữ liệu công nợ')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setLoading(true)
    void load()
  }, [groupId])

  const selectedMaximum = useMemo(() => {
    const payer = debtors.find((member) => member.userId === Number(payerId))
    const receiver = creditors.find((member) => member.userId === Number(receiverId))
    if (!payer || !receiver) return 0
    return Math.min(Math.abs(payer.balance), receiver.balance)
  }, [creditors, debtors, payerId, receiverId])

  const handlePayerChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextPayerId = event.target.value
    setPayerId(nextPayerId)
    const suggestion = balance?.suggestedTransfers.find(
      (item) => item.fromMemberId === Number(nextPayerId),
    )
    if (suggestion) {
      setReceiverId(String(suggestion.toMemberId))
      setAmount(String(suggestion.amount))
    }
  }

  const handleReceiverChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextReceiverId = event.target.value
    setReceiverId(nextReceiverId)
    const payer = debtors.find((member) => member.userId === Number(payerId))
    const receiver = creditors.find((member) => member.userId === Number(nextReceiverId))
    if (payer && receiver) {
      setAmount(String(Math.min(Math.abs(payer.balance), receiver.balance)))
    }
  }

  const applySuggestion = (fromMemberId: number, toMemberId: number, suggestedAmount: number) => {
    setPayerId(String(fromMemberId))
    setReceiverId(String(toMemberId))
    setAmount(String(suggestedAmount))
    document.getElementById('settlement-form')?.scrollIntoView({ behavior: 'smooth' })
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await createSettlement(groupId, {
        payerId: Number(payerId),
        receiverId: Number(receiverId),
        amount: Number(amount),
        note: note.trim() || undefined,
        settledAt: settledAt || undefined,
      })
      setNote('')
      setSettledAt(toLocalDateTimeInput(new Date()))
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể ghi nhận thanh toán')
    } finally {
      setBusy(false)
    }
  }

  const handleCancel = async (settlement: Settlement) => {
    if (!window.confirm(`Hủy giao dịch ${formatMoney(settlement.amount)} từ ${settlement.payerName} đến ${settlement.receiverName}?`)) return
    setBusy(true)
    setError('')
    try {
      await cancelSettlement(settlement.id)
      await load()
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể hủy giao dịch')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <LoadingState label="Đang tính số dư công nợ…" />
  if (!balance) {
    return (
      <section className="narrow-page">
        <ErrorMessage message={error || 'Không thể tải bảng công nợ'} />
        <button className="button button-secondary" type="button" onClick={() => navigate(`/groups/${groupId}`)}>Quay lại nhóm</button>
      </section>
    )
  }

  return (
    <section className="page-section">
      <button className="back-button" type="button" onClick={() => navigate(`/groups/${groupId}`)}>← {balance.groupName}</button>
      {error && <ErrorMessage message={error} />}

      <div className="balance-hero">
        <div>
          <span className="role-badge">CÔNG NỢ NHÓM</span>
          <h1>Số dư và thanh toán</h1>
          <p>Số dư dương là cần nhận, số dư âm là cần trả. Các khoản đã ghi nhận được tự động trừ vào công nợ.</p>
        </div>
        <button className="button button-primary" type="button" onClick={() => document.getElementById('settlement-form')?.scrollIntoView({ behavior: 'smooth' })}>Ghi nhận thanh toán</button>
      </div>

      <div className="balance-stat-grid">
        <article className="balance-stat-card"><span>Tổng khoản chi</span><strong>{formatMoney(balance.totalExpense)}</strong><small>Tất cả khoản chi đang hoạt động</small></article>
        <article className="balance-stat-card"><span>Đã chuyển giữa thành viên</span><strong>{formatMoney(balance.totalSettled)}</strong><small>Không tính giao dịch đã hủy</small></article>
        <article className="balance-stat-card"><span>Còn cần thanh toán</span><strong>{formatMoney(outstandingAmount)}</strong><small>{balance.suggestedTransfers.length} giao dịch được đề xuất</small></article>
      </div>

      <section className="panel balance-members-panel">
        <div className="panel-heading"><div><h2>Số dư từng thành viên</h2><p>Được tính từ tiền đã trả, phần phải chịu và các giao dịch thanh toán.</p></div></div>
        <div className="balance-table-wrap">
          <table className="balance-table">
            <thead><tr><th>Thành viên</th><th>Đã trả</th><th>Phải chịu</th><th>Đã gửi</th><th>Đã nhận</th><th>Số dư</th></tr></thead>
            <tbody>
              {balance.members.map((member) => (
                <tr key={member.userId}>
                  <td><span className="balance-person"><span className="avatar-small">{member.fullName.charAt(0).toUpperCase()}</span><span><b>{member.fullName}{member.userId === user?.id ? ' (Bạn)' : ''}</b><small>{member.membershipStatus === 'ACTIVE' ? member.email : `${member.email} · Đã rời nhóm`}</small></span></span></td>
                  <td>{formatMoney(member.paidAmount)}</td>
                  <td>{formatMoney(member.shareAmount)}</td>
                  <td>{formatMoney(member.sentAmount)}</td>
                  <td>{formatMoney(member.receivedAmount)}</td>
                  <td><strong className={member.balance > 0 ? 'balance-positive' : member.balance < 0 ? 'balance-negative' : 'balance-zero'}>{member.balance > 0 ? '+' : ''}{formatMoney(member.balance)}</strong><small className="balance-caption">{member.balance > 0 ? 'Cần nhận' : member.balance < 0 ? 'Cần trả' : 'Đã cân bằng'}</small></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <div className="balance-layout">
        <section className="panel">
          <div className="panel-heading"><div><h2>Đề xuất chuyển khoản</h2><p>Greedy ghép người nợ và người nhận để rút gọn luồng tiền.</p></div></div>
          {balance.suggestedTransfers.length === 0 ? (
            <div className="balanced-empty"><span>✓</span><h3>Mọi người đã cân bằng</h3><p>Hiện không còn khoản nào cần thanh toán.</p></div>
          ) : (
            <div className="transfer-list">
              {balance.suggestedTransfers.map((transfer, index) => (
                <article className="transfer-row" key={`${transfer.fromMemberId}-${transfer.toMemberId}-${index}`}>
                  <span className="transfer-index">{index + 1}</span>
                  <div><strong>{transfer.fromMemberName}</strong><span>chuyển cho</span><strong>{transfer.toMemberName}</strong></div>
                  <b>{formatMoney(transfer.amount)}</b>
                  <button className="button button-secondary button-small" type="button" onClick={() => applySuggestion(transfer.fromMemberId, transfer.toMemberId, transfer.amount)}>Ghi nhận</button>
                </article>
              ))}
            </div>
          )}
        </section>

        <aside className="panel" id="settlement-form">
          <div className="panel-heading"><div><h2>Ghi nhận thanh toán</h2><p>Giao dịch được xác nhận ngay và cập nhật bảng công nợ.</p></div></div>
          {debtors.length === 0 || creditors.length === 0 ? (
            <p className="muted">Không có cặp người trả và người nhận phù hợp.</p>
          ) : (
            <form onSubmit={handleSubmit}>
              <label className="field"><span>Người trả</span><select value={payerId} onChange={handlePayerChange} required><option value="">Chọn người trả</option>{debtors.map((member) => <option key={member.userId} value={member.userId}>{member.fullName} · nợ {formatMoney(Math.abs(member.balance))}</option>)}</select></label>
              <label className="field"><span>Người nhận</span><select value={receiverId} onChange={handleReceiverChange} required><option value="">Chọn người nhận</option>{creditors.map((member) => <option key={member.userId} value={member.userId}>{member.fullName} · nhận {formatMoney(member.balance)}</option>)}</select></label>
              <label className="field"><span>Số tiền</span><input type="number" min="1" max={selectedMaximum || undefined} value={amount} onChange={(event: ChangeEvent<HTMLInputElement>) => setAmount(event.target.value)} required /><small>Tối đa: {formatMoney(selectedMaximum)}</small></label>
              <label className="field"><span>Thời gian</span><input type="datetime-local" value={settledAt} onChange={(event: ChangeEvent<HTMLInputElement>) => setSettledAt(event.target.value)} required /></label>
              <label className="field"><span>Ghi chú</span><textarea rows={3} value={note} onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setNote(event.target.value)} placeholder="Ví dụ: Đã chuyển khoản ngân hàng" /></label>
              <button className="button button-primary button-block" type="submit" disabled={busy || !selectedMaximum}>{busy ? 'Đang lưu…' : 'Xác nhận thanh toán'}</button>
            </form>
          )}
        </aside>
      </div>

      <section className="panel settlement-history-panel">
        <div className="panel-heading"><div><h2>Lịch sử thanh toán</h2><p>Giao dịch bị hủy vẫn được giữ lại để đối chiếu.</p></div><span className="role-badge">{settlements.length} giao dịch</span></div>
        {settlements.length === 0 ? (
          <p className="muted">Chưa có giao dịch thanh toán nào.</p>
        ) : (
          <div className="settlement-list">
            {settlements.map((settlement) => (
              <article className={`settlement-row ${settlement.status === 'CANCELLED' ? 'cancelled' : ''}`} key={settlement.id}>
                <span className="settlement-icon">{settlement.status === 'CANCELLED' ? '×' : '✓'}</span>
                <div className="settlement-main"><strong>{settlement.payerName} → {settlement.receiverName}</strong><span>{settlement.note || 'Không có ghi chú'} · {dateTimeFormatter.format(new Date(settlement.settledAt ?? settlement.createdAt))}</span><small>Ghi nhận bởi {settlement.createdByName}</small></div>
                <div className="settlement-amount"><strong>{formatMoney(settlement.amount)}</strong><span>{settlement.status === 'CANCELLED' ? 'Đã hủy' : 'Đã xác nhận'}</span></div>
                {settlement.status !== 'CANCELLED' && (settlement.createdById === user?.id || balance.currentUserRole === 'OWNER' || balance.currentUserRole === 'ADMIN') && <button className="icon-button danger-text" type="button" disabled={busy} onClick={() => void handleCancel(settlement)}>Hủy</button>}
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  )
}
