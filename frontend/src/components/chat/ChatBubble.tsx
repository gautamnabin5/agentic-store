import { useState } from 'react'
import { MessageCircle, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/store/authStore'
import ChatPanel from './ChatPanel'

export default function ChatBubble() {
  const { token } = useAuthStore()
  const [open, setOpen] = useState(false)

  if (!token) return null

  return (
    <>
      <ChatPanel open={open} onClose={() => setOpen(false)} />

      <Button
        size="icon"
        className="fixed bottom-4 right-4 z-50 h-12 w-12 rounded-full shadow-lg"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? 'Close chat' : 'Open chat'}
      >
        {open ? <X className="h-5 w-5" /> : <MessageCircle className="h-5 w-5" />}
      </Button>
    </>
  )
}
