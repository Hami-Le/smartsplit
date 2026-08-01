import { apiRequest } from './client'

export type AuthUser = {
  id: number
  fullName: string
  email: string
  avatarUrl: string | null
  role: string
}

export type UserProfile = AuthUser & {
  phone: string | null
}

export type UpdateProfileInput = {
  fullName: string
  phone: string
}

export type ChangePasswordInput = {
  currentPassword: string
  newPassword: string
}

export type AuthData = {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: AuthUser
}

export type RegisterInput = {
  fullName: string
  email: string
  password: string
}

export type LoginInput = {
  email: string
  password: string
}

export function register(input: RegisterInput): Promise<AuthData> {
  return apiRequest<AuthData>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function login(input: LoginInput): Promise<AuthData> {
  return apiRequest<AuthData>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function getProfile(): Promise<UserProfile> {
  return apiRequest<UserProfile>('/users/me')
}

export function updateProfile(input: UpdateProfileInput): Promise<UserProfile> {
  return apiRequest<UserProfile>('/users/me', {
    method: 'PATCH',
    body: JSON.stringify(input),
  })
}

export function changePassword(input: ChangePasswordInput): Promise<void> {
  return apiRequest<void>('/users/me/password', {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function uploadAvatar(file: File): Promise<UserProfile> {
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest<UserProfile>('/users/me/avatar', {
    method: 'POST',
    body: formData,
  })
}

export function deleteAvatar(): Promise<UserProfile> {
  return apiRequest<UserProfile>('/users/me/avatar', { method: 'DELETE' })
}
