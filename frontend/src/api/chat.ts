export const AGENT_BASE_URL =
  (import.meta.env.VITE_AGENT_BASE_URL as string | undefined) ?? 'http://localhost:8000'

export function generateSessionId(): string {
  return crypto.randomUUID()
}
