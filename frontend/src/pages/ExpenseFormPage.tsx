import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import {
  createExpense,
  getExpense,
  listCategories,
  updateExpense,
  type Category,
  type ExpenseInput,
  type SplitType,
} from '../api/expenseApi'
import { getGroup, type GroupDetail } from '../api/groupApi'
import {
  attachReceipt,
  parseReceiptText,
  scanReceipt,
  type ReceiptScan,
} from '../api/ocrApi'
import { useAuth } from '../auth/AuthContext'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingState } from '../components/LoadingState'
import { navigate } from '../router'

const moneyFormatter = new Intl.NumberFormat('vi-VN')

function todayIso(): string {
  const now = new Date()
  const offset = now.getTimezoneOffset()
  return new Date(now.getTime() - offset * 60_000).toISOString().slice(0, 10)
}

function numberValue(value: string | undefined): number {
  if (!value?.trim()) return 0
  const parsed = Number(value.replace(/[^0-9]/g, ''))
  return Number.isSafeInteger(parsed) ? parsed : 0
}

function scanStatusLabel(scan: ReceiptScan): string {
  if (scan.status === 'COMPLETED' && (scan.confidence == null || scan.confidence < 0.75)) return 'Cần kiểm tra'
  if (scan.status === 'COMPLETED') return 'Đã nhận dạng'
  if (scan.status === 'ATTACHED') return 'Đã đính kèm'
  if (scan.status === 'FAILED') return 'OCR thất bại'
  return 'Cần nhập thủ công'
}

function ocrProviderLabel(provider: string): string {
  if (provider === 'TESSERACT_LOCAL') return 'Tesseract local'
  if (provider === 'MANUAL_TEXT') return 'Văn bản thủ công'
  return provider.replaceAll('_', ' ')
}

export function ExpenseFormPage({
  groupId,
  expenseId,
}: {
  groupId: number
  expenseId?: number
}) {
  const { user } = useAuth()
  const [group, setGroup] = useState<GroupDetail | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [expenseDate, setExpenseDate] = useState(todayIso())
  const [categoryId, setCategoryId] = useState('')
  const [splitType, setSplitType] = useState<SplitType>('EQUAL')
  const [payerAmounts, setPayerAmounts] = useState<Record<number, string>>({})
  const [selectedParticipants, setSelectedParticipants] = useState<number[]>([])
  const [shareValues, setShareValues] = useState<Record<number, string>>({})

  const [receiptFile, setReceiptFile] = useState<File | null>(null)
  const [receiptScan, setReceiptScan] = useState<ReceiptScan | null>(null)
  const [manualOcrText, setManualOcrText] = useState('')
  const [ocrBusy, setOcrBusy] = useState(false)
  const [ocrError, setOcrError] = useState('')

  const isEditing = expenseId !== undefined
  const numericTotal = numberValue(totalAmount)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const [groupData, categoryData, expenseData] = await Promise.all([
          getGroup(groupId),
          listCategories(),
          expenseId ? getExpense(expenseId) : Promise.resolve(null),
        ])
        setGroup(groupData)
        setCategories(categoryData)

        if (expenseData) {
          if (expenseData.groupId !== groupId) {
            throw new Error('Khoản chi không thuộc nhóm này')
          }
          setTitle(expenseData.title)
          setDescription(expenseData.description ?? '')
          setTotalAmount(String(expenseData.totalAmount))
          setExpenseDate(expenseData.expenseDate)
          setCategoryId(expenseData.category ? String(expenseData.category.id) : '')
          setSplitType(expenseData.splitType)
          setPayerAmounts(Object.fromEntries(
            expenseData.payers.map((payer) => [payer.userId, String(payer.amount)]),
          ))
          setSelectedParticipants(expenseData.shares.map((share) => share.userId))
          setShareValues(Object.fromEntries(
            expenseData.shares.map((share) => [
              share.userId,
              expenseData.splitType === 'PERCENTAGE'
                ? String(share.percentage ?? '')
                : String(share.amount),
            ]),
          ))
        } else {
          setSelectedParticipants(groupData.members.map((member) => member.userId))
          const defaultPayer = groupData.members.find((member) => member.userId === user?.id)
            ?? groupData.members[0]
          if (defaultPayer) setPayerAmounts({ [defaultPayer.userId]: '' })
        }
      } catch (caught) {
        setError(caught instanceof ApiError ? caught.message : 'Không thể tải form khoản chi')
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [expenseId, groupId, user?.id])

  useEffect(() => {
    const payerIds = Object.keys(payerAmounts)
    if (payerIds.length === 1 && numericTotal > 0) {
      const payerId = Number(payerIds[0])
      setPayerAmounts((current) => {
        const nextValue = String(numericTotal)
        return current[payerId] === nextValue ? current : { [payerId]: nextValue }
      })
    }
  }, [numericTotal, Object.keys(payerAmounts).length])

  const previewShares = useMemo(() => {
    if (!group || selectedParticipants.length === 0 || numericTotal <= 0) return []
    return selectedParticipants.map((userId, index) => {
      let amount = 0
      if (splitType === 'EQUAL') {
        const base = Math.floor(numericTotal / selectedParticipants.length)
        const remainder = numericTotal % selectedParticipants.length
        amount = base + (index < remainder ? 1 : 0)
      } else if (splitType === 'EXACT') {
        amount = numberValue(shareValues[userId])
      } else {
        const percentage = Number(shareValues[userId] ?? 0)
        amount = index === selectedParticipants.length - 1
          ? numericTotal - selectedParticipants.slice(0, -1).reduce(
            (sum, id) => sum + Math.floor(numericTotal * Number(shareValues[id] ?? 0) / 100),
            0,
          )
          : Math.floor(numericTotal * percentage / 100)
      }
      const member = group.members.find((item) => item.userId === userId)
      return { userId, fullName: member?.fullName ?? 'Thành viên', amount }
    })
  }, [group, numericTotal, selectedParticipants, shareValues, splitType])

  const togglePayer = (userId: number) => {
    setPayerAmounts((current) => {
      if (userId in current) {
        const next = { ...current }
        delete next[userId]
        return next
      }
      return { ...current, [userId]: '' }
    })
  }

  const toggleParticipant = (userId: number) => {
    setSelectedParticipants((current) => current.includes(userId)
      ? current.filter((id) => id !== userId)
      : [...current, userId])
  }

  const handleScanReceipt = async () => {
    if (!receiptFile) {
      setOcrError('Hãy chọn ảnh hóa đơn trước.')
      return
    }
    setOcrBusy(true)
    setOcrError('')
    try {
      const result = await scanReceipt(groupId, receiptFile)
      setReceiptScan(result)
      if (result.rawText) setManualOcrText(result.rawText)
    } catch (caught) {
      setOcrError(caught instanceof ApiError ? caught.message : 'Không thể quét hóa đơn')
    } finally {
      setOcrBusy(false)
    }
  }

  const handleParseText = async () => {
    if (!manualOcrText.trim()) {
      setOcrError('Hãy dán văn bản OCR cần phân tích.')
      return
    }
    setOcrBusy(true)
    setOcrError('')
    try {
      setReceiptScan(await parseReceiptText(groupId, manualOcrText))
    } catch (caught) {
      setOcrError(caught instanceof ApiError ? caught.message : 'Không thể phân tích văn bản')
    } finally {
      setOcrBusy(false)
    }
  }

  const applyReceiptSuggestion = () => {
    if (!receiptScan) return
    if (receiptScan.merchant) setTitle(receiptScan.merchant)
    if (receiptScan.totalAmount) setTotalAmount(String(receiptScan.totalAmount))
    if (receiptScan.expenseDate) setExpenseDate(receiptScan.expenseDate)
    if (receiptScan.categoryId) setCategoryId(String(receiptScan.categoryId))
    setOcrError('')
  }

  const validate = (): string | null => {
    if (!title.trim()) return 'Nhập tên khoản chi.'
    if (numericTotal <= 0) return 'Tổng tiền phải lớn hơn 0.'

    const payerEntries = Object.entries(payerAmounts)
    if (payerEntries.length === 0) return 'Chọn ít nhất một người thanh toán.'
    const paidTotal = payerEntries.reduce((sum, [, value]) => sum + numberValue(value), 0)
    if (paidTotal !== numericTotal) {
      return `Tổng tiền người trả đang là ${moneyFormatter.format(paidTotal)} đ, phải bằng ${moneyFormatter.format(numericTotal)} đ.`
    }
    if (selectedParticipants.length === 0) return 'Chọn ít nhất một người tham gia khoản chi.'

    if (splitType === 'EXACT') {
      const sharedTotal = selectedParticipants.reduce(
        (sum, userId) => sum + numberValue(shareValues[userId]),
        0,
      )
      if (sharedTotal !== numericTotal) {
        return `Tổng phần chia đang là ${moneyFormatter.format(sharedTotal)} đ, phải bằng ${moneyFormatter.format(numericTotal)} đ.`
      }
    }
    if (splitType === 'PERCENTAGE') {
      const percentageTotal = selectedParticipants.reduce(
        (sum, userId) => sum + Number(shareValues[userId] ?? 0),
        0,
      )
      if (Math.abs(percentageTotal - 100) > 0.0001) {
        return `Tổng tỷ lệ đang là ${percentageTotal}%, phải bằng 100%.`
      }
    }
    return null
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const validationMessage = validate()
    if (validationMessage) {
      setError(validationMessage)
      return
    }

    const input: ExpenseInput = {
      title: title.trim(),
      description: description.trim() || undefined,
      totalAmount: numericTotal,
      expenseDate,
      categoryId: categoryId ? Number(categoryId) : null,
      payers: Object.entries(payerAmounts).map(([userId, amount]) => ({
        userId: Number(userId),
        amount: numberValue(amount),
      })),
      split: {
        type: splitType,
        participants: selectedParticipants.map((userId) => ({
          userId,
          amount: splitType === 'EXACT' ? numberValue(shareValues[userId]) : null,
          percentage: splitType === 'PERCENTAGE'
            ? Number(shareValues[userId] ?? 0)
            : null,
        })),
      },
    }

    setBusy(true)
    setError('')
    try {
      const saved = expenseId
        ? await updateExpense(expenseId, input)
        : await createExpense(groupId, input)
      if (receiptScan?.hasFile && receiptScan.status !== 'ATTACHED') {
        try {
          await attachReceipt(saved.id, receiptScan.id)
        } catch {
          sessionStorage.setItem(
            'smartsplit.expenseWarning',
            'Khoản chi đã lưu nhưng chưa đính kèm được ảnh hóa đơn.',
          )
        }
      }
      navigate(`/groups/${groupId}/expenses/${saved.id}`, true)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Không thể lưu khoản chi')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <LoadingState label="Đang tải dữ liệu khoản chi…" />
  if (!group) {
    return <section className="narrow-page"><ErrorMessage message={error || 'Không tìm thấy nhóm'} /></section>
  }

  const paidTotal = Object.values(payerAmounts).reduce((sum, value) => sum + numberValue(value), 0)
  const shareTotal = previewShares.reduce((sum, item) => sum + item.amount, 0)

  return (
    <section className="page-section expense-form-page">
      <button className="back-button" type="button" onClick={() => navigate(`/groups/${groupId}`)}>← {group.name}</button>
      <div className="page-heading-row">
        <div>
          <span className="eyebrow">KHOẢN CHI NHÓM</span>
          <h1>{isEditing ? 'Chỉnh sửa khoản chi' : 'Thêm khoản chi'}</h1>
          <p>Quét hóa đơn để tự điền dữ liệu, sau đó kiểm tra và chia tiền cho thành viên.</p>
        </div>
      </div>
      {error && <ErrorMessage message={error} />}

      <form className="expense-layout" onSubmit={handleSubmit}>
        <div className="expense-form-main">
          <section className="panel receipt-ocr-panel">
            <div className="panel-heading">
              <div><h2>OCR hóa đơn</h2><p>Tesseract xử lý ảnh ngay trên máy và đính kèm ảnh vào khoản chi.</p></div>
              {receiptScan && <span className={`ocr-status ocr-status-${receiptScan.status.toLowerCase()}`}>{scanStatusLabel(receiptScan)}</span>}
            </div>

            <div className="ocr-upload-row">
              <label className="receipt-file-picker">
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(event: ChangeEvent<HTMLInputElement>) => {
                    setReceiptFile(event.target.files?.[0] ?? null)
                    setReceiptScan(null)
                    setOcrError('')
                  }}
                />
                <span>{receiptFile ? receiptFile.name : 'Chọn ảnh JPG, PNG hoặc WebP'}</span>
              </label>
              <button className="button button-primary" type="button" onClick={() => void handleScanReceipt()} disabled={ocrBusy || !receiptFile}>
                {ocrBusy ? 'Đang xử lý…' : 'Quét hóa đơn'}
              </button>
            </div>

            {ocrError && <ErrorMessage message={ocrError} />}
            {receiptScan?.message && <p className="ocr-message">{receiptScan.message}</p>}

            {receiptScan && (
              <div className="ocr-result-card">
                <div className="ocr-result-grid">
                  <div><span>Cửa hàng</span><strong>{receiptScan.merchant || 'Chưa nhận dạng'}</strong></div>
                  <div><span>Tổng tiền</span><strong>{receiptScan.totalAmount ? `${moneyFormatter.format(receiptScan.totalAmount)} đ` : 'Chưa nhận dạng'}</strong></div>
                  <div><span>Ngày</span><strong>{receiptScan.expenseDate || 'Chưa nhận dạng'}</strong></div>
                  <div><span>Danh mục</span><strong>{receiptScan.categoryName || 'Chưa phân loại'}</strong></div>
                  <div><span>Độ tin cậy</span><strong>{receiptScan.confidence == null ? '—' : `${Math.round(receiptScan.confidence * 100)}%`}</strong></div>
                  <div><span>Nguồn</span><strong>{ocrProviderLabel(receiptScan.provider)}</strong></div>
                </div>
                {receiptScan.confidence != null && receiptScan.confidence < 0.75 && (
                  <p className="ocr-confidence-warning">Không tự động tin tuyệt đối kết quả này. Hãy đối chiếu ảnh và văn bản OCR trước khi áp dụng.</p>
                )}
                <button
                  className="button button-secondary"
                  type="button"
                  onClick={applyReceiptSuggestion}
                  disabled={receiptScan.status === 'FAILED' || receiptScan.totalAmount == null}
                >
                  Áp dụng vào biểu mẫu
                </button>
              </div>
            )}

            <details className="ocr-manual-box">
              <summary>Ảnh khó đọc? Dán văn bản để kiểm thử bộ phân tích</summary>
              <label className="field">
                <span>Văn bản trên hóa đơn</span>
                <textarea
                  rows={7}
                  value={manualOcrText}
                  onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setManualOcrText(event.target.value)}
                  placeholder={'HIGHLANDS COFFEE\n23/07/2026\nCoffee 89.000\nCake 45.000\nTOTAL 147.400 VND'}
                  maxLength={30000}
                />
              </label>
              <button className="button button-secondary" type="button" onClick={() => void handleParseText()} disabled={ocrBusy || !manualOcrText.trim()}>Phân tích văn bản</button>
              {receiptScan?.rawText && (
                <details className="ocr-raw-text">
                  <summary>Xem văn bản đã nhận dạng</summary>
                  <pre>{receiptScan.rawText}</pre>
                </details>
              )}
            </details>
          </section>

          <section className="panel">
            <div className="panel-heading"><div><h2>Thông tin chung</h2><p>Mô tả khoản tiền đã phát sinh.</p></div></div>
            <label className="field"><span>Tên khoản chi</span><input value={title} onChange={(event: ChangeEvent<HTMLInputElement>) => setTitle(event.target.value)} placeholder="Ví dụ: Quán BBQ" maxLength={180} required /></label>
            <div className="form-grid-two">
              <label className="field"><span>Tổng tiền (đ)</span><input inputMode="numeric" value={totalAmount} onChange={(event: ChangeEvent<HTMLInputElement>) => setTotalAmount(event.target.value.replace(/[^0-9]/g, ''))} placeholder="2300000" required /></label>
              <label className="field"><span>Ngày chi</span><input type="date" value={expenseDate} onChange={(event: ChangeEvent<HTMLInputElement>) => setExpenseDate(event.target.value)} required /></label>
              <label className="field"><span>Danh mục</span><select value={categoryId} onChange={(event: ChangeEvent<HTMLSelectElement>) => setCategoryId(event.target.value)}><option value="">Chưa phân loại</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
            </div>
            <label className="field"><span>Ghi chú</span><textarea rows={3} value={description} onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setDescription(event.target.value)} placeholder="Thông tin bổ sung…" maxLength={1000} /></label>
          </section>

          <section className="panel">
            <div className="panel-heading"><div><h2>Ai đã thanh toán?</h2><p>Có thể chọn một hoặc nhiều người; tổng phải bằng khoản chi.</p></div><strong>{moneyFormatter.format(paidTotal)} đ</strong></div>
            <div className="allocation-list">
              {group.members.map((member) => {
                const selected = member.userId in payerAmounts
                return (
                  <div className={`allocation-row ${selected ? 'selected' : ''}`} key={member.userId}>
                    <label className="member-check"><input type="checkbox" checked={selected} onChange={() => togglePayer(member.userId)} /><span className="avatar-small">{member.fullName.charAt(0).toUpperCase()}</span><span><strong>{member.fullName}</strong><small>{member.email}</small></span></label>
                    {selected && <label className="inline-money"><input aria-label={`Số tiền ${member.fullName} đã trả`} inputMode="numeric" value={payerAmounts[member.userId] ?? ''} onChange={(event: ChangeEvent<HTMLInputElement>) => setPayerAmounts((current) => ({ ...current, [member.userId]: event.target.value.replace(/[^0-9]/g, '') }))} /><span>đ</span></label>}
                  </div>
                )
              })}
            </div>
          </section>

          <section className="panel">
            <div className="panel-heading"><div><h2>Chia tiền</h2><p>Chọn người tham gia và cách phân bổ nghĩa vụ.</p></div><strong>{moneyFormatter.format(shareTotal)} đ</strong></div>
            <div className="segmented-control" role="group" aria-label="Cách chia tiền">
              <button type="button" className={splitType === 'EQUAL' ? 'active' : ''} onClick={() => setSplitType('EQUAL')}>Chia đều</button>
              <button type="button" className={splitType === 'PERCENTAGE' ? 'active' : ''} onClick={() => setSplitType('PERCENTAGE')}>Theo %</button>
              <button type="button" className={splitType === 'EXACT' ? 'active' : ''} onClick={() => setSplitType('EXACT')}>Theo số tiền</button>
            </div>
            <div className="allocation-list">
              {group.members.map((member) => {
                const selected = selectedParticipants.includes(member.userId)
                const preview = previewShares.find((item) => item.userId === member.userId)
                return (
                  <div className={`allocation-row ${selected ? 'selected' : ''}`} key={member.userId}>
                    <label className="member-check"><input type="checkbox" checked={selected} onChange={() => toggleParticipant(member.userId)} /><span className="avatar-small">{member.fullName.charAt(0).toUpperCase()}</span><span><strong>{member.fullName}</strong><small>{selected && preview ? `${moneyFormatter.format(preview.amount)} đ` : member.email}</small></span></label>
                    {selected && splitType !== 'EQUAL' && (
                      <label className="inline-money compact"><input aria-label={`Phần chia của ${member.fullName}`} inputMode="decimal" value={shareValues[member.userId] ?? ''} onChange={(event: ChangeEvent<HTMLInputElement>) => setShareValues((current) => ({ ...current, [member.userId]: splitType === 'EXACT' ? event.target.value.replace(/[^0-9]/g, '') : event.target.value.replace(/[^0-9.]/g, '') }))} /><span>{splitType === 'PERCENTAGE' ? '%' : 'đ'}</span></label>
                    )}
                  </div>
                )
              })}
            </div>
          </section>
        </div>

        <aside className="panel expense-summary-card">
          <span className="eyebrow">TÓM TẮT</span>
          <h2>{title.trim() || 'Khoản chi mới'}</h2>
          <strong className="expense-total-large">{moneyFormatter.format(numericTotal)} đ</strong>
          <dl className="summary-list">
            <div><dt>Người trả</dt><dd>{Object.keys(payerAmounts).length}</dd></div>
            <div><dt>Người tham gia</dt><dd>{selectedParticipants.length}</dd></div>
            <div><dt>Cách chia</dt><dd>{splitType === 'EQUAL' ? 'Chia đều' : splitType === 'PERCENTAGE' ? 'Theo %' : 'Theo số tiền'}</dd></div>
            <div><dt>Hóa đơn</dt><dd>{receiptScan ? scanStatusLabel(receiptScan) : 'Chưa có'}</dd></div>
          </dl>
          <button className="button button-primary button-block" type="submit" disabled={busy}>{busy ? 'Đang lưu…' : isEditing ? 'Lưu thay đổi' : 'Tạo khoản chi'}</button>
          <button className="button button-secondary button-block" type="button" onClick={() => navigate(`/groups/${groupId}`)} disabled={busy}>Hủy</button>
        </aside>
      </form>
    </section>
  )
}
