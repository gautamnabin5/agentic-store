export interface ProductResponse {
  id: string
  name: string
  description: string
  price: number
  stockQuantity: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  name: string
  description: string
  price: number
  stockQuantity: number
}

export interface OrderItemResponse {
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

export interface OrderResponse {
  id: string
  userId: string
  items: OrderItemResponse[]
  totalAmount: number
  createdAt: string
}

export interface PlaceOrderRequest {
  items: { productId: string; quantity: number }[]
}

export interface AuthResponse {
  token: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UserInfo {
  id: string
  email: string
  role: 'CUSTOMER' | 'ADMIN'
}
