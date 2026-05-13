import { useRef, useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { AGENT_BASE_URL, generateSessionId } from '@/api/chat'

export interface UserMessage {
  id: string
  role: 'user'
  content: string
}

export interface AssistantMessage {
  id: string
  role: 'assistant'
  content: string
}

export interface ToolCallMessage {
  id: string
  role: 'tool-call'
  toolCallId: string
  toolName: string
  args: {
    message?: string
    items?: Array<{ productId: number | string; quantity: number; unitPrice?: number; productName?: string }>
  }
}

export type ChatMessage = UserMessage | AssistantMessage | ToolCallMessage

export function useChatSession() {
  const { token } = useAuthStore()
  const sessionId = useRef(generateSessionId())
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const abortRef = useRef<AbortController | null>(null)

  async function streamChat(body: Record<string, unknown>) {
    if (!token) return
    abortRef.current?.abort()
    const ctrl = new AbortController()
    abortRef.current = ctrl
    setIsLoading(true)

    try {
      const response = await fetch(AGENT_BASE_URL + '/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + token,
        },
        body: JSON.stringify(body),
        signal: ctrl.signal,
      })

      if (!response.ok || !response.body) return

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let assistantId: string | null = null
      let assistantContent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        const chunks = buffer.split('\n')
        buffer = chunks.pop() ?? ''

        for (const line of chunks) {
          if (!line.startsWith('data: ')) continue
          const json = line.slice(6).trim()
          if (!json) continue

          let chunk: { type: string; content?: string; toolCallId?: string; toolName?: string; args?: Record<string, unknown> }
          try { chunk = JSON.parse(json) as typeof chunk } catch { continue }

          if (chunk.type === 'text' && chunk.content) {
            if (assistantId === null) {
              assistantId = crypto.randomUUID()
              assistantContent = chunk.content
              const newId = assistantId
              const initContent = assistantContent
              setMessages((prev) => [...prev, { id: newId, role: 'assistant', content: initContent }])
            } else {
              assistantContent += chunk.content
              const curId = assistantId
              const curContent = assistantContent
              setMessages((prev) => prev.map((m) => (m.id === curId ? { ...m, content: curContent } : m)))
            }
          } else if (chunk.type === 'tool-call' && chunk.toolCallId && chunk.toolName) {
            setMessages((prev) => [
              ...prev,
              {
                id: crypto.randomUUID(),
                role: 'tool-call' as const,
                toolCallId: chunk.toolCallId!,
                toolName: chunk.toolName!,
                args: (chunk.args as ToolCallMessage['args']) ?? {},
              },
            ])
          } else if (chunk.type === 'done' || chunk.type === 'error') {
            break
          }
        }
      }
    } catch (e) {
      if ((e as Error).name !== 'AbortError') console.error('Chat stream error:', e)
    } finally {
      setIsLoading(false)
    }
  }

  async function sendMessage(text: string) {
    if (!text.trim() || isLoading) return
    setMessages((prev) => [...prev, { id: crypto.randomUUID(), role: 'user', content: text }])
    await streamChat({ session_id: sessionId.current, message: text })
  }

  async function confirmOrder(toolCallId: string, approved: boolean) {
    if (isLoading) return
    await streamChat({ session_id: sessionId.current, tool_call_id: toolCallId, approved })
  }

  function resetSession() {
    abortRef.current?.abort()
    sessionId.current = generateSessionId()
    setMessages([])
    setIsLoading(false)
  }

  return { messages, isLoading, sendMessage, confirmOrder, resetSession }
}
