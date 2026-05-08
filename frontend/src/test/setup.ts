/// <reference types="vitest/globals" />
import '@testing-library/jest-dom'

// Node 25 exposes a stub globalThis.localStorage with no methods, which breaks
// vitest's jsdom environment (the stub wins over jsdom's proper Storage impl).
// Fix: redirect globalThis.localStorage/sessionStorage to jsdom's window objects.
if (typeof globalThis.jsdom !== 'undefined') {
  const jsdomWindow = (globalThis as any).jsdom.window
  Object.defineProperty(globalThis, 'localStorage', {
    get: () => jsdomWindow.localStorage,
    configurable: true,
  })
  Object.defineProperty(globalThis, 'sessionStorage', {
    get: () => jsdomWindow.sessionStorage,
    configurable: true,
  })
}
