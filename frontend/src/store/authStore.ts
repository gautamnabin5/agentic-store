import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { UserInfo } from '@/api/types'

interface AuthState {
  token: string | null
  user: UserInfo | null
  login: (token: string, user: UserInfo) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      login: (token, user) => {
        import('@/store/cartStore').then(({ useCartStore }) => useCartStore.getState().clear()).catch(() => {})
        set({ token, user })
      },
      logout: () => {
        import('@/store/cartStore').then(({ useCartStore }) => useCartStore.getState().clear()).catch(() => {})
        set({ token: null, user: null })
      },
    }),
    { name: 'auth-storage', storage: createJSONStorage(() => localStorage) }
  )
)
