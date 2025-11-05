/**
 * Server-Sent Events (SSE) Handler
 * Server-sent event streaming for edge applications
 */

export interface SSEEvent {
  id?: string;
  event?: string;
  data: string;
  retry?: number;
}

export class SSEStream {
  private id = 0;

  formatEvent(event: SSEEvent): string {
    const parts: string[] = [];

    if (event.id) {
      parts.push(`id: ${event.id}`);
    } else {
      parts.push(`id: ${++this.id}`);
    }

    if (event.event) {
      parts.push(`event: ${event.event}`);
    }

    // Handle multi-line data
    const dataLines = event.data.split('\n');
    dataLines.forEach(line => {
      parts.push(`data: ${line}`);
    });

    if (event.retry) {
      parts.push(`retry: ${event.retry}`);
    }

    parts.push(''); // Empty line to end event
    parts.push(''); // Double newline

    return parts.join('\n');
  }

  createHeaders(): Record<string, string> {
    return {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no' // Disable nginx buffering
    };
  }

  // Helper to create common events
  message(data: string | object): string {
    const dataStr = typeof data === 'string' ? data : JSON.stringify(data);
    return this.formatEvent({ data: dataStr });
  }

  custom(event: string, data: string | object): string {
    const dataStr = typeof data === 'string' ? data : JSON.stringify(data);
    return this.formatEvent({ event, data: dataStr });
  }

  comment(text: string): string {
    return `: ${text}\n\n`;
  }

  keepAlive(): string {
    return this.comment('keep-alive');
  }
}

export class SSEClient {
  private listeners = new Map<string, ((data: string) => void)[]>();

  parseEvent(raw: string): SSEEvent | null {
    const lines = raw.split('\n').filter(l => l.length > 0);
    const event: SSEEvent = { data: '' };
    const dataLines: string[] = [];

    for (const line of lines) {
      if (line.startsWith(':')) continue; // Comment

      const colonIndex = line.indexOf(':');
      if (colonIndex === -1) continue;

      const field = line.slice(0, colonIndex);
      const value = line.slice(colonIndex + 1).trimStart();

      switch (field) {
        case 'id':
          event.id = value;
          break;
        case 'event':
          event.event = value;
          break;
        case 'data':
          dataLines.push(value);
          break;
        case 'retry':
          event.retry = parseInt(value, 10);
          break;
      }
    }

    if (dataLines.length > 0) {
      event.data = dataLines.join('\n');
      return event;
    }

    return null;
  }

  on(eventType: string, callback: (data: string) => void): this {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, []);
    }
    this.listeners.get(eventType)!.push(callback);
    return this;
  }

  dispatch(event: SSEEvent): void {
    const eventType = event.event || 'message';
    const callbacks = this.listeners.get(eventType);

    if (callbacks) {
      callbacks.forEach(cb => cb(event.data));
    }
  }
}

// CLI demo
if (import.meta.url.includes("sse-handler.ts")) {
  console.log("Server-Sent Events Demo\n");

  const stream = new SSEStream();

  console.log("SSE Headers:");
  console.log(JSON.stringify(stream.createHeaders(), null, 2));

  console.log("\nFormatted events:");

  console.log("1. Simple message:");
  console.log(stream.message('Hello, World!').replace(/\n/g, '\\n'));

  console.log("\n2. JSON data:");
  console.log(stream.message({ user: 'Alice', action: 'login' }).replace(/\n/g, '\\n'));

  console.log("\n3. Custom event:");
  console.log(stream.custom('notification', { type: 'info', text: 'New message' }).replace(/\n/g, '\\n'));

  console.log("\n4. Multi-line data:");
  const multiLine = stream.formatEvent({
    id: '42',
    event: 'update',
    data: 'Line 1\nLine 2\nLine 3',
    retry: 5000
  });
  console.log(multiLine.replace(/\n/g, '\\n'));

  console.log("\n5. Keep-alive:");
  console.log(stream.keepAlive().replace(/\n/g, '\\n'));

  console.log("\nClient parsing:");
  const client = new SSEClient();

  client.on('message', (data) => {
    console.log("  → Message:", data);
  });

  client.on('notification', (data) => {
    console.log("  → Notification:", data);
  });

  const event1 = client.parseEvent('data: Hello\n');
  const event2 = client.parseEvent('event: notification\ndata: {"type":"info"}\n');

  if (event1) client.dispatch(event1);
  if (event2) client.dispatch(event2);

  console.log("\nStreaming simulation:");
  const events = [
    { data: 'Event 1' },
    { data: 'Event 2' },
    { event: 'custom', data: 'Custom event' }
  ];

  events.forEach((evt, i) => {
    console.log(`  → ${i + 1}:`, stream.message(evt.data).split('\n')[1]);
  });

  console.log("\n✅ SSE handler test passed");
}
