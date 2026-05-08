import { beforeEach, describe, it, expect } from 'vitest'
import { useCartStore } from '../cartStore'
import type { ProductResponse } from '@/api/types'

function makeProduct(id: string): ProductResponse {
  return { id, name: `Product ${id}`, description: '', price: 29.99,
    stockQuantity: 10, active: true, createdAt: '', updatedAt: '' }
}

beforeEach(() => {
  localStorage.clear()
  useCartStore.setState({ items: [] })
})

describe('cartStore', () => {
  it('adds a new product', () => {
    useCartStore.getState().add(makeProduct('p1'), 2)
    expect(useCartStore.getState().items).toHaveLength(1)
    expect(useCartStore.getState().items[0].quantity).toBe(2)
  })

  it('increments quantity when adding an existing product', () => {
    useCartStore.getState().add(makeProduct('p1'), 1)
    useCartStore.getState().add(makeProduct('p1'), 3)
    expect(useCartStore.getState().items[0].quantity).toBe(4)
  })

  it('removes a product by id', () => {
    useCartStore.getState().add(makeProduct('p1'), 1)
    useCartStore.getState().add(makeProduct('p2'), 2)
    useCartStore.getState().remove('p1')
    const { items } = useCartStore.getState()
    expect(items).toHaveLength(1)
    expect(items[0].product.id).toBe('p2')
  })

  it('updates quantity for a product', () => {
    useCartStore.getState().add(makeProduct('p1'), 1)
    useCartStore.getState().updateQuantity('p1', 5)
    expect(useCartStore.getState().items[0].quantity).toBe(5)
  })

  it('clears all items', () => {
    useCartStore.getState().add(makeProduct('p1'), 1)
    useCartStore.getState().add(makeProduct('p2'), 2)
    useCartStore.getState().clear()
    expect(useCartStore.getState().items).toHaveLength(0)
  })
})
