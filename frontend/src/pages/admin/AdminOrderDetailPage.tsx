import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getAnyOrder } from '@/api/orders'
import type { OrderResponse } from '@/api/types'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { ChevronLeft } from 'lucide-react'
import { Separator } from '@/components/ui/separator'

export default function AdminOrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    getAnyOrder(id)
      .then(setOrder)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="text-muted-foreground">Loading…</div>
  if (error || !order) return <div className="text-destructive">{error ?? 'Order not found'}</div>

  return (
    <div className="max-w-2xl space-y-6">
      <Link
        to="/admin/orders"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <ChevronLeft className="h-4 w-4" />
        Back to orders
      </Link>

      <div>
        <h1 className="text-xl font-bold">Order detail</h1>
        <p className="text-muted-foreground text-sm font-mono mt-1">{order.id}</p>
        <p className="text-muted-foreground text-sm">
          Customer: <span className="font-mono text-xs">{order.userId}</span>
        </p>
        <p className="text-muted-foreground text-sm">
          Placed on {new Date(order.createdAt).toLocaleString()}
        </p>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Product</TableHead>
            <TableHead className="text-right">Unit Price</TableHead>
            <TableHead className="text-right">Qty</TableHead>
            <TableHead className="text-right">Line Total</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {order.items.map((item) => (
            <TableRow key={item.productId}>
              <TableCell>{item.productName}</TableCell>
              <TableCell className="text-right text-muted-foreground">
                ${item.unitPrice.toFixed(2)}
              </TableCell>
              <TableCell className="text-right text-muted-foreground">{item.quantity}</TableCell>
              <TableCell className="text-right font-medium">
                ${(item.unitPrice * item.quantity).toFixed(2)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Separator />
      <div className="flex justify-end">
        <div className="space-y-1 text-right">
          <p className="text-muted-foreground text-sm">Order total</p>
          <p className="text-2xl font-bold text-primary">${order.totalAmount.toFixed(2)}</p>
        </div>
      </div>
    </div>
  )
}
