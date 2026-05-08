import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { ProductResponse } from '@/api/types'

export interface CartItem {
  product: ProductResponse
  quantity: number
}

interface CartState {
  items: CartItem[]
  add: (product: ProductResponse, quantity: number) => void
  remove: (productId: string) => void
  updateQuantity: (productId: string, quantity: number) => void
  clear: () => void
}

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      add: (product, quantity) =>
        set((state) => {
          const existing = state.items.find((i) => i.product.id === product.id)
          if (existing) {
            return {
              items: state.items.map((i) =>
                i.product.id === product.id ? { ...i, quantity: i.quantity + quantity } : i
              ),
            }
          }
          return { items: [...state.items, { product, quantity }] }
        }),
      remove: (productId) =>
        set((state) => ({ items: state.items.filter((i) => i.product.id !== productId) })),
      updateQuantity: (productId, quantity) =>
        set((state) => ({
          items: state.items.map((i) => (i.product.id === productId ? { ...i, quantity } : i)),
        })),
      clear: () => set({ items: [] }),
    }),
    { name: 'cart-storage', storage: createJSONStorage(() => localStorage) }
  )
)
