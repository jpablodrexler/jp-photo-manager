export class MockEventSource {
  private listeners: Record<string, EventListenerOrEventListenerObject[]> = {};

  readonly CONNECTING = 0 as const;
  readonly OPEN = 1 as const;
  readonly CLOSED = 2 as const;
  readyState: number = 1;
  url = '';
  withCredentials = false;
  onopen: ((this: EventSource, ev: Event) => unknown) | null = null;
  onmessage: ((this: EventSource, ev: MessageEvent) => unknown) | null = null;
  onerror: ((this: EventSource, ev: Event) => unknown) | null = null;

  addEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    if (!this.listeners[type]) this.listeners[type] = [];
    this.listeners[type].push(listener);
  }

  removeEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    this.listeners[type] = (this.listeners[type] ?? []).filter(l => l !== listener);
  }

  dispatchEvent(event: Event): boolean {
    (this.listeners[event.type] ?? []).forEach(l => {
      if (typeof l === 'function') l(event);
      else l.handleEvent(event);
    });
    return true;
  }

  emit(type: string, data: unknown): void {
    const event = new MessageEvent(type, { data: JSON.stringify(data) });
    this.dispatchEvent(event);
  }

  emitRaw(type: string, data: string): void {
    const event = new MessageEvent(type, { data });
    this.dispatchEvent(event);
  }

  close(): void {
    this.readyState = 2;
  }
}
