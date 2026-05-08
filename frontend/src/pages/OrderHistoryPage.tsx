import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listOrders } from '@/api/orders'
import type { OrderResponse } from '@/api/types'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'

export default function OrderHistoryPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listOrders()
      .then(setOrders)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-muted-foreground">Loading orders…</div>
  if (error) return <div className="text-destructive">Error: {error}</div>

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Order History</h1>
      {orders.length === 0 ? (
        <p className="text-muted-foreground">You have no orders yet.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Order ID</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Items</TableHead>
              <TableHead className="text-right">Total</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.id} className="cursor-pointer hover:bg-accent/30">
                <TableCell>
                  <Link to={`/orders/${order.id}`} className="text-primary hover:underline font-mono text-xs">
                    {order.id.slice(0, 8)}…
                  </Link>
                </TableCell>
                <TableCell className="text-muted-foreground text-sm">
                  {new Date(order.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell className="text-muted-foreground text-sm">
                  {order.items.reduce((s, i) => s + i.quantity, 0)} items
                </TableCell>
                <TableCell className="text-right font-medium">
                  ${order.totalAmount.toFixed(2)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}
