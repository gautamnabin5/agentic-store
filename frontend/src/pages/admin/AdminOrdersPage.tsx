import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listAllOrders } from '@/api/orders'
import type { OrderResponse } from '@/api/types'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    listAllOrders()
      .then(setOrders)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="text-muted-foreground">Loading…</div>
  if (error) return <div className="text-destructive">Error: {error}</div>

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-bold">Orders</h1>
        <p className="text-muted-foreground text-sm">{orders.length} total</p>
      </div>

      {orders.length === 0 ? (
        <p className="text-muted-foreground">No orders yet.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Order ID</TableHead>
              <TableHead>Customer</TableHead>
              <TableHead>Date</TableHead>
              <TableHead className="text-right">Items</TableHead>
              <TableHead className="text-right">Total</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.id} className="cursor-pointer hover:bg-accent/30">
                <TableCell>
                  <Link
                    to={`/admin/orders/${order.id}`}
                    className="text-primary hover:underline font-mono text-xs"
                  >
                    {order.id.slice(0, 8)}…
                  </Link>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground font-mono text-xs">
                  {order.userId.slice(0, 8)}…
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {new Date(order.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell className="text-right text-muted-foreground text-sm">
                  {order.items.reduce((s, i) => s + i.quantity, 0)}
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
