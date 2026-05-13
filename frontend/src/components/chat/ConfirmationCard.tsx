import { Button } from '@/components/ui/button'

interface OrderItem {
  productId: string | number
  productName?: string
  quantity: number
  unitPrice?: number
}

interface Props {
  toolCallId: string
  items: OrderItem[]
  onConfirm: (toolCallId: string) => void
  onCancel: (toolCallId: string) => void
}

export default function ConfirmationCard({ toolCallId, items, onConfirm, onCancel }: Props) {
  return (
    <div className="rounded-lg border bg-muted/40 p-4 text-sm space-y-3 my-2">
      <p className="font-semibold text-foreground">Confirm your order</p>

      <ul className="space-y-1">
        {items.map((item, i) => (
          <li key={i} className="flex justify-between text-muted-foreground">
            <span>{item.productName ?? String(item.productId)} × {item.quantity}</span>
            {item.unitPrice !== undefined && (
              <span>${(item.unitPrice * item.quantity).toFixed(2)}</span>
            )}
          </li>
        ))}
      </ul>

      <div className="flex gap-2 pt-1">
        <Button size="sm" onClick={() => onConfirm(toolCallId)}>
          Confirm Order
        </Button>
        <Button size="sm" variant="outline" onClick={() => onCancel(toolCallId)}>
          Cancel
        </Button>
      </div>
    </div>
  )
}
