import { request } from './client'
import type { ProductResponse, ProductRequest } from './types'

export function listProducts(): Promise<ProductResponse[]> {
  return request('/api/v1/products')
}

export function getProduct(id: string): Promise<ProductResponse> {
  return request(`/api/v1/products/${id}`)
}

export function createProduct(data: ProductRequest): Promise<ProductResponse> {
  return request('/api/v1/products', { method: 'POST', body: JSON.stringify(data) })
}

export function updateProduct(id: string, data: ProductRequest): Promise<ProductResponse> {
  return request(`/api/v1/products/${id}`, { method: 'PUT', body: JSON.stringify(data) })
}

export function deleteProduct(id: string): Promise<void> {
  return request(`/api/v1/products/${id}`, { method: 'DELETE' })
}
