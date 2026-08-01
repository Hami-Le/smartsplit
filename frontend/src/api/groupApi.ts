import { apiRequest } from './client'

export type GroupRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export type GroupSummary = {
  id: number
  name: string
  description: string | null
  avatarUrl: string | null
  defaultCurrency: string
  currentUserRole: GroupRole
  memberCount: number
  createdAt: string
}

export type GroupMember = {
  membershipId: number
  userId: number
  fullName: string
  email: string
  avatarUrl: string | null
  role: GroupRole
  status: string
  joinedAt: string
}

export type GroupDetail = {
  id: number
  name: string
  description: string | null
  avatarUrl: string | null
  defaultCurrency: string
  currentUserRole: GroupRole
  createdBy: number
  createdAt: string
  updatedAt: string
  members: GroupMember[]
}

export type GroupInput = {
  name: string
  description?: string
  defaultCurrency?: string
}

export type Invitation = {
  id: number
  groupId: number
  groupName: string
  email: string
  status: string
  expiresAt: string
  token: string
  invitationPath: string
}

export type AcceptInvitationResult = {
  groupId: number
  groupName: string
  role: GroupRole
}

export function listGroups(): Promise<GroupSummary[]> {
  return apiRequest<GroupSummary[]>('/groups')
}

export function getGroup(groupId: number): Promise<GroupDetail> {
  return apiRequest<GroupDetail>(`/groups/${groupId}`)
}

export function createGroup(input: GroupInput): Promise<GroupDetail> {
  return apiRequest<GroupDetail>('/groups', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateGroup(
  groupId: number,
  input: Partial<GroupInput>,
): Promise<GroupDetail> {
  return apiRequest<GroupDetail>(`/groups/${groupId}`, {
    method: 'PATCH',
    body: JSON.stringify(input),
  })
}

export function archiveGroup(groupId: number): Promise<void> {
  return apiRequest<void>(`/groups/${groupId}`, { method: 'DELETE' })
}

export function inviteMember(groupId: number, email: string): Promise<Invitation> {
  return apiRequest<Invitation>(`/groups/${groupId}/invitations`, {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function updateMemberRole(
  groupId: number,
  userId: number,
  role: Exclude<GroupRole, 'OWNER'>,
): Promise<GroupMember> {
  return apiRequest<GroupMember>(`/groups/${groupId}/members/${userId}/role`, {
    method: 'PATCH',
    body: JSON.stringify({ role }),
  })
}

export function removeMember(groupId: number, userId: number): Promise<void> {
  return apiRequest<void>(`/groups/${groupId}/members/${userId}`, {
    method: 'DELETE',
  })
}

export function acceptInvitation(token: string): Promise<AcceptInvitationResult> {
  return apiRequest<AcceptInvitationResult>(`/invitations/${token}/accept`, {
    method: 'POST',
  })
}
