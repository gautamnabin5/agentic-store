import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useChatSession } from '@/hooks/useChatSession'
import ChatInput from './ChatInput'
import ConfirmationCard from './ConfirmationCard'
import { cn } from '@/lib/utils'
import { useAuthStore } from '@/store/authStore'

interface Props {
  open: boolean
  onClose: () => void
}

export default function ChatPanel({ open, onClose }: Props) {
  const { token } = useAuthStore()
  const { messages, isLoading, sendMessage, confirmOrder, resetSession } = useChatSession()
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (open) resetSession()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  if (!token) return null

  return (
    <div
      className={cn(
        'fixed bottom-20 right-4 z-50 flex flex-col',
        'w-[360px] max-h-[560px] rounded-xl border bg-background shadow-xl',
        'transition-all duration-200',
        open
          ? 'opacity-100 translate-y-0 pointer-events-auto'
          : 'opacity-0 translate-y-4 pointer-events-none',
      )}
    >
      <div className="flex items-center justify-between border-b px-4 py-3">
        <span className="font-semibold text-sm">Store Assistant</span>
        <Button variant="ghost" size="icon" className="h-7 w-7" onClick={onClose} aria-label="Close chat">
          <X className="h-4 w-4" />
        </Button>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3 text-sm">
        {messages.length === 0 && (
          <p className="text-muted-foreground text-center mt-8">
            Hi! Ask me about products or your orders.
          </p>
        )}

        {messages.map((msg) => {
          if (msg.role === 'user') {
            return (
              <div key={msg.id} className="flex justify-end">
                <span className="bg-primary text-primary-foreground rounded-2xl rounded-tr-sm px-3 py-2 max-w-[80%]">
                  {msg.content}
                </span>
              </div>
            )
          }

          if (msg.role === 'assistant') {
            return (
              <div
                key={msg.id}
                className="bg-muted rounded-2xl rounded-tl-sm px-3 py-2 max-w-[85%] whitespace-pre-wrap"
              >
                {msg.content}
              </div>
            )
          }

          if (msg.role === 'tool-call' && msg.toolName === 'confirm_order') {
            return (
              <ConfirmationCard
                key={msg.id}
                toolCallId={msg.toolCallId}
                items={msg.args?.items ?? []}
                onConfirm={(id) => confirmOrder(id, true)}
                onCancel={(id) => confirmOrder(id, false)}
              />
            )
          }

          return null
        })}

        {isLoading && (
          <div className="bg-muted rounded-2xl rounded-tl-sm px-3 py-2 w-fit">
            <span className="animate-pulse text-muted-foreground">…</span>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      <ChatInput onSend={sendMessage} disabled={isLoading} />
    </div>
  )
}
