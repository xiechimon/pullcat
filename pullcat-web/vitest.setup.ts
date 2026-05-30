import '@testing-library/jest-dom/vitest'

class MockEventSource {
  url: string
  onopen: (() => void) | null = null
  onerror: (() => void) | null = null
  private listeners: Record<string, Array<(event: MessageEvent) => void>> = {}

  constructor(url: string) {
    this.url = url
  }

  addEventListener(type: string, listener: (event: MessageEvent) => void): void {
    if (!this.listeners[type]) {
      this.listeners[type] = []
    }
    this.listeners[type].push(listener)
  }

  removeEventListener(type: string, listener: (event: MessageEvent) => void): void {
    if (this.listeners[type]) {
      this.listeners[type] = this.listeners[type].filter((l) => l !== listener)
    }
  }

  close(): void {
    this.listeners = {}
  }

  dispatchEvent(event: MessageEvent): void {
    const typeListeners = this.listeners[event.type]
    if (typeListeners) {
      for (const listener of typeListeners) {
        listener(event)
      }
    }
  }
}

Object.defineProperty(globalThis, 'EventSource', {
  value: MockEventSource,
  writable: true,
  configurable: true,
})
