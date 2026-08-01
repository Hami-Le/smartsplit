import { apiRequest } from './client'

export type AuthUser = {
  id: number
  fullName: string
  email: string
  role: string
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
