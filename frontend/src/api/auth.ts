import { request } from './client'
import { decodeJwtPayload } from '@/lib/jwt'
import type { AuthResponse, RegisterRequest, LoginRequest, UserInfo } from './types'

async function tokenToUserInfo(token: string): Promise<{ token: string; user: UserInfo }> {
  const { sub, email, role } = decodeJwtPayload(token)
  return { token, user: { id: sub, email, role: role as UserInfo['role'] } }
}

export async function register(data: RegisterRequest): Promise<{ token: string; user: UserInfo }> {
  const { token } = await request<AuthResponse>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  return tokenToUserInfo(token)
}

export async function login(data: LoginRequest): Promise<{ token: string; user: UserInfo }> {
  const { token } = await request<AuthResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
  })
  return tokenToUserInfo(token)
}
