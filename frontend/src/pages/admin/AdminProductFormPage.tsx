import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { getProduct, createProduct, updateProduct } from '@/api/products'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { ChevronLeft } from 'lucide-react'

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string(),
  price: z.number().positive('Price must be greater than 0'),
  stockQuantity: z.number().int('Must be a whole number').min(0, 'Cannot be negative'),
})
type FormValues = z.infer<typeof schema>

export default function AdminProductFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const navigate = useNavigate()
  const [apiError, setApiError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { description: '' },
  })

  useEffect(() => {
    if (!isEdit) return
    getProduct(id)
      .then((p) =>
        reset({
          name: p.name,
          description: p.description ?? '',
          price: p.price,
          stockQuantity: p.stockQuantity,
        })
      )
      .catch((e: Error) => setLoadError(e.message))
  }, [id, isEdit, reset])

  const onSubmit = async (data: FormValues) => {
    setApiError(null)
    try {
      if (isEdit) {
        await updateProduct(id, data)
      } else {
        await createProduct(data)
      }
      navigate('/admin/products')
    } catch (e: unknown) {
      setApiError((e as Error).message)
    }
  }

  if (loadError) return <div className="text-destructive">Error loading product: {loadError}</div>

  return (
    <div className="max-w-lg space-y-6">
      <Link
        to="/admin/products"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <ChevronLeft className="h-4 w-4" />
        Back to products
      </Link>

      <h1 className="text-xl font-bold">{isEdit ? 'Edit product' : 'New product'}</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {apiError && (
          <Alert variant="destructive">
            <AlertDescription>{apiError}</AlertDescription>
          </Alert>
        )}

        <div className="space-y-1.5">
          <Label htmlFor="name">Name</Label>
          <Input id="name" {...register('name')} />
          {errors.name && <p className="text-destructive text-xs">{errors.name.message}</p>}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="description">Description <span className="text-muted-foreground">(optional)</span></Label>
          <Input id="description" {...register('description')} />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="price">Price ($)</Label>
            <Input id="price" type="number" step="0.01" min="0.01" {...register('price', { valueAsNumber: true })} />
            {errors.price && <p className="text-destructive text-xs">{errors.price.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="stockQuantity">Stock quantity</Label>
            <Input id="stockQuantity" type="number" min="0" step="1" {...register('stockQuantity', { valueAsNumber: true })} />
            {errors.stockQuantity && (
              <p className="text-destructive text-xs">{errors.stockQuantity.message}</p>
            )}
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create product'}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/admin/products')}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  )
}
