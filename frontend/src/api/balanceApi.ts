import { apiRequest } from './client'

export type MemberBalance = {
  userId: number
  fullName: string
  email: string
  avatarUrl: string | null
  membershipStatus: 'ACTIVE' | 'LEFT' | 'REMOVED'
  paidAmount: number
  shareAmount: number
  sentAmount: number
  receivedAmount: number
  balance: number
}

export type TransferSuggestion = {
  fromMemberId: number
  fromMemberName: string
  toMemberId: number
  toMemberName: string
  amount: number
}

export type GroupBalance = {
  groupId: number
  groupName: string
  currency: string
  currentUserRole: 'OWNER' | 'ADMIN' | 'MEMBER'
  totalExpense: number
  totalSettled: number
  members: MemberBalance[]
  suggestedTransfers: TransferSuggestion[]
}

export type Settlement = {
  id: number
  groupId: number
  payerId: number
  payerName: string
  receiverId: number
  receiverName: string
  amount: number
  note: string | null
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  settledAt: string | null
  createdById: number
  createdByName: string
  createdAt: string
}

export type SettlementInput = {
  payerId: number
  receiverId: number
  amount: number
  note?: string
  settledAt?: string
}

export function getGroupBalances(groupId: number): Promise<GroupBalance> {
  return apiRequest<GroupBalance>(`/groups/${groupId}/balances`)
}

export function listSettlements(groupId: number): Promise<Settlement[]> {
  return apiRequest<Settlement[]>(`/groups/${groupId}/settlements`)
}

export function createSettlement(
  groupId: number,
  input: SettlementInput,
): Promise<Settlement> {
  return apiRequest<Settlement>(`/groups/${groupId}/settlements`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function cancelSettlement(settlementId: number): Promise<void> {
  return apiRequest<void>(`/settlements/${settlementId}`, { method: 'DELETE' })
}
