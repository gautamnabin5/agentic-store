import { request } from './client'
import type { OrderResponse, PlaceOrderRequest } from './types'

export function placeOrder(data: PlaceOrderRequest): Promise<OrderResponse> {
  return request('/api/v1/orders', { method: 'POST', body: JSON.stringify(data) })
}

export function listOrders(): Promise<OrderResponse[]> {
  return request('/api/v1/orders')
}

export function getOrder(id: string): Promise<OrderResponse> {
  return request(`/api/v1/orders/${id}`)
}

export function listAllOrders(): Promise<OrderResponse[]> {
  return request('/api/v1/admin/orders')
}

export function getAnyOrder(id: string): Promise<OrderResponse> {
  return request(`/api/v1/admin/orders/${id}`)
}
