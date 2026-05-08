export function decodeJwtPayload(token: string): { sub: string; email: string; role: string } {
  const [, payload] = token.split('.')
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(atob(base64))
}
