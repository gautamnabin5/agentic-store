import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Minus, Plus, Trash2 } from 'lucide-react'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { useCartStore } from '@/store/cartStore'
import { useAuthStore } from '@/store/authStore'
import { placeOrder } from '@/api/orders'

interface Props {
  open: boolean
  onClose: () => void
}

export default function CartDrawer({ open, onClose }: Props) {
  const { items, remove, updateQuantity, clear } = useCartStore()
  const token = useAuthStore((s) => s.token)
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const total = items.reduce((sum, i) => sum + i.product.price * i.quantity, 0)

  const handlePlaceOrder = async () => {
    if (!token) {
      onClose()
      navigate('/login?redirect=/')
      return
    }
    setPlacing(true)
    setError(null)
    try {
      const order = await placeOrder({
        items: items.map((i) => ({ productId: i.product.id, quantity: i.quantity })),
      })
      clear()
      onClose()
      navigate(`/orders/${order.id}`)
    } catch (e: unknown) {
      setError((e as Error).message)
    } finally {
      setPlacing(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent side="right" className="w-full max-w-sm flex flex-col bg-card border-border">
        <SheetHeader className="border-b border-border pb-4">
          <SheetTitle>Cart ({items.length})</SheetTitle>
        </SheetHeader>

        {items.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <p className="text-muted-foreground text-sm">Your cart is empty.</p>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-y-auto py-4 space-y-4">
              {items.map(({ product, quantity }) => (
                <div key={product.id} className="flex gap-3">
                  <div className="w-14 h-14 bg-accent rounded-md flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium leading-tight truncate">{product.name}</p>
                    <p className="text-primary text-sm">${product.price.toFixed(2)}</p>
                    <div className="flex items-center gap-2 mt-1.5">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-6 w-6"
                        onClick={() =>
                          quantity <= 1 ? remove(product.id) : updateQuantity(product.id, quantity - 1)
                        }
                      >
                        <Minus className="h-3 w-3" />
                      </Button>
                      <span className="text-sm w-5 text-center">{quantity}</span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-6 w-6"
                        onClick={() => updateQuantity(product.id, quantity + 1)}
                      >
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <p className="text-sm font-medium">${(product.price * quantity).toFixed(2)}</p>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-6 w-6 text-muted-foreground hover:text-destructive"
                      onClick={() => remove(product.id)}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>

            <div className="border-t border-border pt-4 space-y-4">
              {error && (
                <Alert variant="destructive">
                  <AlertDescription className="text-xs">{error}</AlertDescription>
                </Alert>
              )}
              <Separator />
              <div className="flex justify-between font-semibold">
                <span>Total</span>
                <span>${total.toFixed(2)}</span>
              </div>
              <Button className="w-full" onClick={handlePlaceOrder} disabled={placing}>
                {placing ? 'Placing order…' : 'Place Order'}
              </Button>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}
