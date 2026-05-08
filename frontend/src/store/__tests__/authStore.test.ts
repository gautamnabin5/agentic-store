import { beforeEach, describe, it, expect } from 'vitest'
import { useAuthStore } from '../authStore'
import { useCartStore } from '../cartStore'
import type { ProductResponse } from '@/api/types'

function makeProduct(): ProductResponse {
  return { id: 'p1', name: 'Test', description: '', price: 10,
    stockQuantity: 5, active: true, createdAt: '', updatedAt: '' }
}

beforeEach(() => {
  localStorage.clear()
  useCartStore.setState({ items: [] })
  useAuthStore.setState({ token: null, user: null })
})

describe('authStore', () => {
  it('login sets token and user', () => {
    useAuthStore.getState().login('jwt-token', { id: 'u1', email: 'a@b.com', role: 'CUSTOMER' })
    const { token, user } = useAuthStore.getState()
    expect(token).toBe('jwt-token')
    expect(user?.email).toBe('a@b.com')
    expect(user?.role).toBe('CUSTOMER')
  })

  it('login clears cart first', async () => {
    useCartStore.getState().add(makeProduct(), 3)
    expect(useCartStore.getState().items).toHaveLength(1)
    useAuthStore.getState().login('jwt', { id: 'u1', email: 'a@b.com', role: 'CUSTOMER' })
    await new Promise(r => setTimeout(r, 0))
    expect(useCartStore.getState().items).toHaveLength(0)
  })

  it('logout clears token and user', () => {
    useAuthStore.setState({ token: 'jwt', user: { id: 'u1', email: 'a@b.com', role: 'CUSTOMER' } })
    useAuthStore.getState().logout()
    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('logout clears cart', async () => {
    useAuthStore.setState({ token: 'jwt', user: { id: 'u1', email: 'a@b.com', role: 'CUSTOMER' } })
    useCartStore.getState().add(makeProduct(), 1)
    useAuthStore.getState().logout()
    await new Promise(r => setTimeout(r, 0))
    expect(useCartStore.getState().items).toHaveLength(0)
  })
})
