import { apiRequest } from './client'

export type SplitType = 'EQUAL' | 'PERCENTAGE' | 'EXACT'

export type Category = {
  id: number
  name: string
  icon: string | null
}

export type ExpensePersonAmount = {
  userId: number
  fullName: string
  email: string
  avatarUrl: string | null
  amount: number
  percentage: number | null
}

export type Expense = {
  id: number
  groupId: number
  groupName: string
  title: string
  description: string | null
  totalAmount: number
  expenseDate: string
  category: Category | null
  createdByUserId: number
  createdByName: string
  status: 'ACTIVE' | 'DELETED'
  splitType: SplitType
  version: number
  createdAt: string
  updatedAt: string
  payers: ExpensePersonAmount[]
  shares: ExpensePersonAmount[]
}

export type ExpenseInput = {
  title: string
  description?: string
  totalAmount: number
  expenseDate: string
  categoryId?: number | null
  payers: Array<{
    userId: number
    amount: number
  }>
  split: {
    type: SplitType
    participants: Array<{
      userId: number
      amount?: number | null
      percentage?: number | null
    }>
  }
}

export type ExpenseFilters = {
  from?: string
  to?: string
  categoryId?: number
  search?: string
}

function toQuery(filters: ExpenseFilters): string {
  const params = new URLSearchParams()
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  if (filters.categoryId) params.set('categoryId', String(filters.categoryId))
  if (filters.search?.trim()) params.set('search', filters.search.trim())
  const query = params.toString()
  return query ? `?${query}` : ''
}

export function listCategories(): Promise<Category[]> {
  return apiRequest<Category[]>('/categories')
}

export function listExpenses(
  groupId: number,
  filters: ExpenseFilters = {},
): Promise<Expense[]> {
  return apiRequest<Expense[]>(`/groups/${groupId}/expenses${toQuery(filters)}`)
}

export function getExpense(expenseId: number): Promise<Expense> {
  return apiRequest<Expense>(`/expenses/${expenseId}`)
}

export function createExpense(groupId: number, input: ExpenseInput): Promise<Expense> {
  return apiRequest<Expense>(`/groups/${groupId}/expenses`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateExpense(expenseId: number, input: ExpenseInput): Promise<Expense> {
  return apiRequest<Expense>(`/expenses/${expenseId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function deleteExpense(expenseId: number): Promise<void> {
  return apiRequest<void>(`/expenses/${expenseId}`, { method: 'DELETE' })
}
