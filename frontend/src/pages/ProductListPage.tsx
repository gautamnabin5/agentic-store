import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { listProducts } from '@/api/products'
import type { ProductResponse } from '@/api/types'
import { useCartStore } from '@/store/cartStore'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Slider } from '@/components/ui/slider'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

type SortKey = 'default' | 'price-asc' | 'price-desc' | 'name'

export default function ProductListPage() {
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [inStockOnly, setInStockOnly] = useState(false)
  const [sortBy, setSortBy] = useState<SortKey>('default')
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 9999])
  const addToCart = useCartStore((s) => s.add)

  useEffect(() => {
    listProducts()
      .then((data) => {
        setProducts(data)
        if (data.length > 0) {
          const prices = data.map((p) => p.price)
          setPriceRange([Math.floor(Math.min(...prices)), Math.ceil(Math.max(...prices))])
        }
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  const maxPrice = useMemo(() => Math.ceil(Math.max(...products.map((p) => p.price), 0)), [products])
  const minPrice = useMemo(() => Math.floor(Math.min(...products.map((p) => p.price), 0)), [products])

  const filtered = useMemo(() => {
    return products
      .filter((p) => !inStockOnly || p.stockQuantity > 0)
      .filter((p) => p.price >= priceRange[0] && p.price <= priceRange[1])
      .sort((a, b) => {
        if (sortBy === 'price-asc') return a.price - b.price
        if (sortBy === 'price-desc') return b.price - a.price
        if (sortBy === 'name') return a.name.localeCompare(b.name)
        return 0
      })
  }, [products, inStockOnly, priceRange, sortBy])

  if (loading) return <div className="text-muted-foreground">Loading products…</div>
  if (error) return <div className="text-destructive">Error: {error}</div>

  return (
    <div className="flex gap-6">
      {/* Sidebar filters */}
      <aside className="w-56 flex-shrink-0 space-y-6">
        <div className="space-y-3">
          <h3 className="text-sm font-semibold">Price range</h3>
          <Slider
            min={minPrice}
            max={maxPrice}
            step={1}
            value={priceRange}
            onValueChange={(v) => setPriceRange(v as [number, number])}
          />
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>${priceRange[0]}</span>
            <span>${priceRange[1]}</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Switch id="in-stock" checked={inStockOnly} onCheckedChange={setInStockOnly} />
          <Label htmlFor="in-stock" className="text-sm">In stock only</Label>
        </div>

        <div className="space-y-1.5">
          <Label className="text-sm font-semibold">Sort by</Label>
          <Select value={sortBy} onValueChange={(v) => setSortBy(v as SortKey)}>
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="default">Default</SelectItem>
              <SelectItem value="price-asc">Price: Low to High</SelectItem>
              <SelectItem value="price-desc">Price: High to Low</SelectItem>
              <SelectItem value="name">Name</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </aside>

      {/* Product grid */}
      <div className="flex-1">
        {filtered.length === 0 ? (
          <p className="text-muted-foreground">No products match your filters.</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {filtered.map((product) => {
              const inStock = product.stockQuantity > 0
              return (
                <div
                  key={product.id}
                  className="bg-card rounded-lg border border-border p-4 flex flex-col gap-2"
                >
                  <Link to={`/products/${product.id}`}>
                    <div className="aspect-square bg-accent rounded-md mb-1" />
                  </Link>
                  <Link
                    to={`/products/${product.id}`}
                    className="font-semibold text-sm leading-tight hover:text-primary transition-colors line-clamp-2"
                  >
                    {product.name}
                  </Link>
                  <p className="text-primary font-medium text-sm">${product.price.toFixed(2)}</p>
                  <Badge
                    variant={inStock ? 'default' : 'secondary'}
                    className={`w-fit text-xs ${inStock ? 'bg-emerald-900/50 text-emerald-400 border-emerald-800' : 'text-muted-foreground'}`}
                  >
                    {inStock ? 'In Stock' : 'Out of Stock'}
                  </Badge>
                  <Button
                    size="sm"
                    className="mt-auto"
                    disabled={!inStock}
                    onClick={() => addToCart(product, 1)}
                  >
                    Add to Cart
                  </Button>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
