export function decodeJwtPayload(token: string): { sub: string; email: string; role: string } {
  const parts = token.split('.')
  if (parts.length !== 3) throw new Error('Invalid JWT format')
  const payload = parts[1]
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
  try {
    return JSON.parse(atob(padded))
  } catch {
    throw new Error('Failed to decode JWT payload')
  }
}
