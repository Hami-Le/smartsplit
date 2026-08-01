import { apiRequest } from './client'

export type AuthUser = {
  id: number
  fullName: string
  email: string
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
