import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listProducts, deleteProduct } from '@/api/products'
import type { ProductResponse } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Pencil, Trash2, Plus } from 'lucide-react'

function stockBadgeClass(qty: number): string {
  if (qty === 0) return 'bg-red-900/50 text-red-400 border-red-800'
  if (qty < 10) return 'bg-amber-900/50 text-amber-400 border-amber-800'
  return 'bg-emerald-900/50 text-emerald-400 border-emerald-800'
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const navigate = useNavigate()

  const load = () => {
    setLoading(true)
    listProducts()
      .then(setProducts)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const confirmDelete = async () => {
    if (!deletingId) return
    try {
      await deleteProduct(deletingId)
      setDeletingId(null)
      load()
    } catch (e: unknown) {
      setDeleteError((e as Error).message)
    }
  }

  if (loading) return <div className="text-muted-foreground">Loading…</div>
  if (error) return <div className="text-destructive">Error: {error}</div>

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Products</h1>
          <p className="text-muted-foreground text-sm">{products.length} items</p>
        </div>
        <Button size="sm" onClick={() => navigate('/admin/products/new')}>
          <Plus className="h-4 w-4 mr-1" />
          New Product
        </Button>
      </div>

      {deleteError && <p className="text-destructive text-sm">{deleteError}</p>}

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Name</TableHead>
            <TableHead className="text-right">Price</TableHead>
            <TableHead className="text-right">Stock</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {products.map((p) => (
            <TableRow key={p.id}>
              <TableCell className="font-medium">{p.name}</TableCell>
              <TableCell className="text-right text-primary">${p.price.toFixed(2)}</TableCell>
              <TableCell className="text-right">
                <Badge className={`${stockBadgeClass(p.stockQuantity)} font-mono`}>
                  {p.stockQuantity}
                </Badge>
              </TableCell>
              <TableCell>
                <Badge variant="outline" className={p.active ? 'text-emerald-400 border-emerald-800' : 'text-muted-foreground'}>
                  {p.active ? 'Active' : 'Inactive'}
                </Badge>
              </TableCell>
              <TableCell className="text-right">
                <div className="flex justify-end gap-2">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    onClick={() => navigate(`/admin/products/${p.id}/edit`)}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 text-muted-foreground hover:text-destructive"
                    onClick={() => { setDeleteError(null); setDeletingId(p.id) }}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <AlertDialog open={!!deletingId} onOpenChange={(o) => !o && setDeletingId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete product?</AlertDialogTitle>
            <AlertDialogDescription>
              This will deactivate the product. Existing orders are not affected.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={confirmDelete}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
