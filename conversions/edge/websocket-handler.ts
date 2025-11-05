/**
 * WebSocket Handler
 * WebSocket connection patterns for edge applications
 * (Pattern/interface - actual WS support varies by platform)
 */

export type MessageType = 'text' | 'binary' | 'ping' | 'pong' | 'close';

export interface WebSocketMessage {
  type: MessageType;
  data: string | ArrayBuffer;
  timestamp: number;
}

export interface WebSocketConnection {
  id: string;
  metadata: Record<string, any>;
  send(data: string | ArrayBuffer): void;
  close(code?: number, reason?: string): void;
}

export type MessageHandler = (connection: WebSocketConnection, message: WebSocketMessage) => void | Promise<void>;
export type ConnectionHandler = (connection: WebSocketConnection) => void | Promise<void>;

export class WebSocketRoom {
  private connections = new Map<string, WebSocketConnection>();
  private messageHandlers: MessageHandler[] = [];
  private connectHandlers: ConnectionHandler[] = [];
  private disconnectHandlers: ConnectionHandler[] = [];

  onMessage(handler: MessageHandler): this {
    this.messageHandlers.push(handler);
    return this;
  }

  onConnect(handler: ConnectionHandler): this {
    this.connectHandlers.push(handler);
    return this;
  }

  onDisconnect(handler: ConnectionHandler): this {
    this.disconnectHandlers.push(handler);
    return this;
  }

  async handleConnect(connection: WebSocketConnection): Promise<void> {
    this.connections.set(connection.id, connection);

    for (const handler of this.connectHandlers) {
      await handler(connection);
    }
  }

  async handleMessage(connection: WebSocketConnection, message: WebSocketMessage): Promise<void> {
    for (const handler of this.messageHandlers) {
      await handler(connection, message);
    }
  }

  async handleDisconnect(connection: WebSocketConnection): Promise<void> {
    this.connections.delete(connection.id);

    for (const handler of this.disconnectHandlers) {
      await handler(connection);
    }
  }

  broadcast(data: string | ArrayBuffer, exclude?: string): void {
    for (const [id, conn] of this.connections) {
      if (id !== exclude) {
        conn.send(data);
      }
    }
  }

  broadcastToGroup(data: string | ArrayBuffer, group: string): void {
    for (const conn of this.connections.values()) {
      if (conn.metadata.group === group) {
        conn.send(data);
      }
    }
  }

  getConnection(id: string): WebSocketConnection | undefined {
    return this.connections.get(id);
  }

  getConnections(): WebSocketConnection[] {
    return Array.from(this.connections.values());
  }

  getConnectionCount(): number {
    return this.connections.size;
  }
}

// Mock connection for testing
class MockWebSocketConnection implements WebSocketConnection {
  id: string;
  metadata: Record<string, any> = {};
  private messages: (string | ArrayBuffer)[] = [];
  private closed = false;

  constructor(id: string, metadata: Record<string, any> = {}) {
    this.id = id;
    this.metadata = metadata;
  }

  send(data: string | ArrayBuffer): void {
    if (this.closed) {
      throw new Error('Connection closed');
    }
    this.messages.push(data);
  }

  close(code?: number, reason?: string): void {
    this.closed = true;
  }

  getMessages(): (string | ArrayBuffer)[] {
    return [...this.messages];
  }

  isClosed(): boolean {
    return this.closed;
  }
}

// CLI demo
if (import.meta.url.includes("websocket-handler.ts")) {
  console.log("WebSocket Handler Demo\n");

  const room = new WebSocketRoom();

  // Set up handlers
  room
    .onConnect(async (conn) => {
      console.log(`  → User ${conn.id} connected`);
      conn.send(JSON.stringify({ type: 'welcome', message: 'Welcome to the room!' }));
    })
    .onMessage(async (conn, msg) => {
      if (msg.type === 'text') {
        console.log(`  → Message from ${conn.id}: ${msg.data}`);
        room.broadcast(msg.data, conn.id);
      }
    })
    .onDisconnect(async (conn) => {
      console.log(`  → User ${conn.id} disconnected`);
    });

  (async () => {
    console.log("Simulate connections:");

    const alice = new MockWebSocketConnection('alice', { group: 'admins' });
    const bob = new MockWebSocketConnection('bob', { group: 'users' });
    const charlie = new MockWebSocketConnection('charlie', { group: 'users' });

    await room.handleConnect(alice);
    await room.handleConnect(bob);
    await room.handleConnect(charlie);

    console.log(`\nConnections: ${room.getConnectionCount()}`);

    console.log("\nBroadcast to all:");
    room.broadcast(JSON.stringify({ type: 'announcement', text: 'Hello everyone!' }));

    console.log("\nBroadcast to group:");
    room.broadcastToGroup(JSON.stringify({ type: 'admin', text: 'Admin only' }), 'admins');
    room.broadcastToGroup(JSON.stringify({ type: 'info', text: 'User info' }), 'users');

    console.log("\nAlice received:", alice.getMessages().length, "messages");
    console.log("Bob received:", bob.getMessages().length, "messages");

    await room.handleDisconnect(bob);
    console.log(`\nAfter disconnect: ${room.getConnectionCount()} connections`);

    console.log("\n✅ WebSocket handler test passed");
    console.log("⚠️  Note: This is a pattern demo (actual WS support varies by platform)");
  })();
}
