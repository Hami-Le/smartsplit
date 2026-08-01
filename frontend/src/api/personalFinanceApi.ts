import { apiRequest } from './client'
import type { Category } from './expenseApi'

export type PersonalExpense = {
  id: number
  title: string
  amount: number
  expenseDate: string
  category: Category | null
  note: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export type PersonalCategorySpending = {
  categoryId: number | null
  categoryName: string
  icon: string | null
  amount: number
  expenseCount: number
  percentage: number
}

export type PersonalFinanceSummary = {
  month: string
  budgetAmount: number
  totalSpent: number
  remainingAmount: number
  usagePercentage: number
  overBudget: boolean
  expenseCount: number
  categoryBreakdown: PersonalCategorySpending[]
  expenses: PersonalExpense[]
}

export type PersonalExpenseInput = {
  title: string
  amount: number
  expenseDate: string
  categoryId?: number | null
  note?: string
}

export function getPersonalFinance(month: string): Promise<PersonalFinanceSummary> {
  return apiRequest<PersonalFinanceSummary>(`/personal-finance?month=${encodeURIComponent(month)}`)
}

export function createPersonalExpense(input: PersonalExpenseInput): Promise<PersonalExpense> {
  return apiRequest<PersonalExpense>('/personal-finance/expenses', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updatePersonalExpense(
  expenseId: number,
  input: PersonalExpenseInput,
): Promise<PersonalExpense> {
  return apiRequest<PersonalExpense>(`/personal-finance/expenses/${expenseId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function deletePersonalExpense(expenseId: number): Promise<void> {
  return apiRequest<void>(`/personal-finance/expenses/${expenseId}`, { method: 'DELETE' })
}

export function setMonthlyBudget(
  month: string,
  amount: number,
): Promise<PersonalFinanceSummary> {
  return apiRequest<PersonalFinanceSummary>(`/personal-finance/budgets/${month}`, {
    method: 'PUT',
    body: JSON.stringify({ amount }),
  })
}

export function deleteMonthlyBudget(month: string): Promise<PersonalFinanceSummary> {
  return apiRequest<PersonalFinanceSummary>(`/personal-finance/budgets/${month}`, {
    method: 'DELETE',
  })
}
