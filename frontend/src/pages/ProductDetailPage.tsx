import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getProduct } from '@/api/products'
import type { ProductResponse } from '@/api/types'
import { useCartStore } from '@/store/cartStore'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ChevronLeft, Minus, Plus, Package } from 'lucide-react'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [quantity, setQuantity] = useState(1)
  const addToCart = useCartStore((s) => s.add)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    getProduct(id)
      .then((p) => { setProduct(p); setQuantity(1) })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="h-4 w-28 bg-muted rounded animate-pulse mb-6" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="aspect-square bg-muted rounded-xl animate-pulse" />
          <div className="space-y-4">
            <div className="h-8 bg-muted rounded animate-pulse" />
            <div className="h-10 w-24 bg-muted rounded animate-pulse" />
            <div className="h-6 w-16 bg-muted rounded animate-pulse" />
            <div className="space-y-2">
              <div className="h-4 bg-muted rounded animate-pulse" />
              <div className="h-4 bg-muted rounded animate-pulse" />
              <div className="h-4 w-3/4 bg-muted rounded animate-pulse" />
            </div>
            <div className="h-11 bg-muted rounded animate-pulse" />
          </div>
        </div>
      </div>
    )
  }
  if (error || !product) return <div className="text-destructive">{error ?? 'Product not found'}</div>

  const inStock = product.stockQuantity > 0
  const maxQty = product.stockQuantity

  const decrement = () => setQuantity((q) => Math.max(1, q - 1))
  const increment = () => setQuantity((q) => Math.min(maxQty, q + 1))

  return (
    <div className="max-w-3xl mx-auto">
      <Link
        to="/"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors"
      >
        <ChevronLeft className="h-4 w-4" />
        Back to products
      </Link>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="aspect-square bg-accent/50 rounded-xl border border-border flex items-center justify-center">
          <Package className="h-20 w-20 text-muted-foreground/20" />
        </div>

        <div className="space-y-4">
          <h1 className="text-2xl font-bold">{product.name}</h1>
          <p className="text-3xl font-semibold text-primary">${product.price.toFixed(2)}</p>

          <Badge
            className={`w-fit ${
              inStock
                ? 'bg-emerald-900/50 text-emerald-400 border-emerald-800'
                : 'bg-muted text-muted-foreground'
            }`}
          >
            {inStock ? 'In Stock' : 'Out of Stock'}
          </Badge>

          {product.description && (
            <p className="text-muted-foreground text-sm leading-relaxed">{product.description}</p>
          )}

          {inStock && (
            <div className="flex items-center gap-3">
              <span className="text-sm text-muted-foreground">Qty</span>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" onClick={decrement} disabled={quantity <= 1}>
                  <Minus className="h-3 w-3" />
                </Button>
                <span className="w-8 text-center font-medium">{quantity}</span>
                <Button variant="outline" size="icon" onClick={increment} disabled={quantity >= maxQty}>
                  <Plus className="h-3 w-3" />
                </Button>
              </div>
            </div>
          )}

          <Button
            size="lg"
            className="w-full"
            disabled={!inStock}
            onClick={() => inStock && addToCart(product, quantity)}
          >
            {inStock ? 'Add to Cart' : 'Out of Stock'}
          </Button>
        </div>
      </div>
    </div>
  )
}
