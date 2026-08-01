import { apiRequest } from './client'

export type ReceiptScan = {
  id: number
  groupId: number
  status: 'COMPLETED' | 'MANUAL_REQUIRED' | 'FAILED' | 'ATTACHED'
  provider: string
  originalFileName: string | null
  hasFile: boolean
  merchant: string | null
  totalAmount: number | null
  expenseDate: string | null
  categoryId: number | null
  categoryName: string | null
  confidence: number | null
  rawText: string | null
  message: string | null
  createdAt: string
}

export function scanReceipt(groupId: number, file: File): Promise<ReceiptScan> {
  const body = new FormData()
  body.append('file', file)
  return apiRequest<ReceiptScan>(`/groups/${groupId}/receipt-scans`, {
    method: 'POST',
    body,
  })
}

export function parseReceiptText(groupId: number, rawText: string): Promise<ReceiptScan> {
  return apiRequest<ReceiptScan>(`/groups/${groupId}/receipt-scans/parse-text`, {
    method: 'POST',
    body: JSON.stringify({ rawText }),
  })
}

export function attachReceipt(expenseId: number, scanId: number): Promise<ReceiptScan> {
  return apiRequest<ReceiptScan>(`/expenses/${expenseId}/receipt-scans/${scanId}/attach`, {
    method: 'POST',
  })
}

export type ReceiptAttachment = {
  id: number
  fileUrl: string
  fileType: string
  ocrStatus: string
  createdAt: string
}

export function listReceiptAttachments(expenseId: number): Promise<ReceiptAttachment[]> {
  return apiRequest<ReceiptAttachment[]>(`/expenses/${expenseId}/attachments`)
}
