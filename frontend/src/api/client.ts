import { useAuthStore } from '@/store/authStore'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const { token, logout } = useAuthStore.getState()

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (response.status === 401) {
    logout()
    throw new Error('Session expired. Please log in again.')
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: 'Request failed' }))
    throw new Error((body as { error?: string }).error ?? 'Request failed')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}
