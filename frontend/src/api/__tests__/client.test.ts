import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/store/authStore', () => ({
  useAuthStore: {
    getState: vi.fn(),
  },
}))

import { request } from '../client'
import { useAuthStore } from '@/store/authStore'

const mockGetState = vi.mocked(useAuthStore.getState)

beforeEach(() => {
  vi.clearAllMocks()
  mockGetState.mockReturnValue({ token: null, logout: vi.fn() } as any)
})

describe('request()', () => {
  it('attaches Authorization header when token exists', async () => {
    mockGetState.mockReturnValue({ token: 'test-jwt', logout: vi.fn() } as any)
    global.fetch = vi.fn().mockResolvedValue({
      ok: true, status: 200,
      json: () => Promise.resolve({ id: '1' }),
    } as any)

    await request('/api/v1/products')

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/products'),
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer test-jwt' }),
      })
    )
  })

  it('omits Authorization header when token is null', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true, status: 200,
      json: () => Promise.resolve({}),
    } as any)

    await request('/api/v1/products')

    const callHeaders = (vi.mocked(global.fetch).mock.calls[0][1] as RequestInit)
      .headers as Record<string, string>
    expect(callHeaders['Authorization']).toBeUndefined()
  })

  it('calls logout and throws on 401', async () => {
    const mockLogout = vi.fn()
    mockGetState.mockReturnValue({ token: 'expired', logout: mockLogout } as any)
    global.fetch = vi.fn().mockResolvedValue({
      ok: false, status: 401,
      json: () => Promise.resolve({ error: 'Unauthorized' }),
    } as any)

    await expect(request('/api/v1/orders')).rejects.toThrow('Session expired')
    expect(mockLogout).toHaveBeenCalled()
  })

  it('throws with error message from response body on non-401 error', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false, status: 404,
      json: () => Promise.resolve({ error: 'Product not found' }),
    } as any)

    await expect(request('/api/v1/products/bad-id')).rejects.toThrow('Product not found')
  })

  it('returns undefined for 204 No Content', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true, status: 204,
      json: () => Promise.resolve(null),
    } as any)

    const result = await request('/api/v1/products/some-id')
    expect(result).toBeUndefined()
  })
})
