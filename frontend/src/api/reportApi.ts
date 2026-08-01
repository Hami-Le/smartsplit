import { apiDownload, apiRequest } from './client'

export type CategorySpending = {
  categoryId: number | null
  categoryName: string
  icon: string | null
  amount: number
  expenseCount: number
  percentage: number
}

export type MemberSpending = {
  userId: number
  fullName: string
  email: string
  avatarUrl: string | null
  membershipStatus: string
  paidAmount: number
  shareAmount: number
  sharePercentage: number
}

export type MonthlySpending = {
  month: string
  label: string
  amount: number
  expenseCount: number
}

export type DashboardExpense = {
  id: number
  title: string
  expenseDate: string
  totalAmount: number
  categoryName: string
  categoryIcon: string | null
  createdByName: string
}

export type Dashboard = {
  groupId: number
  groupName: string
  currency: string
  currentUserRole: 'OWNER' | 'ADMIN' | 'MEMBER'
  from: string
  to: string
  totalExpense: number
  expenseCount: number
  averageExpense: number
  highestExpense: number
  totalSettled: number
  outstandingAmount: number
  largestExpense: DashboardExpense | null
  categoryBreakdown: CategorySpending[]
  memberSpending: MemberSpending[]
  monthlyTrend: MonthlySpending[]
  recentExpenses: DashboardExpense[]
}

export type ReportFilters = {
  from?: string
  to?: string
}

function toQuery(filters: ReportFilters, format?: 'xlsx' | 'pdf'): string {
  const params = new URLSearchParams()
  if (format) params.set('format', format)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  const query = params.toString()
  return query ? `?${query}` : ''
}

export function getDashboard(
  groupId: number,
  filters: ReportFilters = {},
): Promise<Dashboard> {
  return apiRequest<Dashboard>(`/groups/${groupId}/dashboard${toQuery(filters)}`)
}

export async function downloadReport(
  groupId: number,
  format: 'xlsx' | 'pdf',
  filters: ReportFilters,
): Promise<void> {
  const file = await apiDownload(
    `/groups/${groupId}/reports/export${toQuery(filters, format)}`,
  )
  const url = URL.createObjectURL(file.blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}
