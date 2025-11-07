import { WebSocketServer, WebSocket } from 'ws';
import { IncomingMessage } from 'http';
import type {
  WebSocketMessage,
  SubscribeMessage,
  TerminalOutputMessage,
  TerminalInputMessage,
  BuildStartedMessage,
  BuildCompletedMessage,
  BuildBellMessage,
  ErrorMessage,
} from '../../../shared/types.js';
import { DbJobManager } from '../services/db-job-manager.js';

interface Client {
  ws: WebSocket;
  jobIds: Set<string>;
}

let jobHandlerRegistered = false;

/**
 * Setup WebSocket server for real-time updates
 */
export function setupWebSocketServer(wss: WebSocketServer): void {
  // Prevent duplicate handler registration
  if (jobHandlerRegistered) {
    return;
  }
  jobHandlerRegistered = true;

  const clients = new Map<WebSocket, Client>();
  const jobManager = DbJobManager.getInstance();

  wss.on('connection', (ws: WebSocket, req: IncomingMessage) => {
    // Only handle job WebSocket connections (path = /ws)
    if (req.url !== '/ws') {
      // Let other handlers deal with this connection
      return;
    }

    console.log('Client connected');

    const client: Client = {
      ws,
      jobIds: new Set(),
    };

    clients.set(ws, client);

    // Handle messages from client
    ws.on('message', (data: Buffer) => {
      try {
        const message = JSON.parse(data.toString()) as WebSocketMessage;
        handleClientMessage(client, message);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
        sendError(ws, 'Invalid message format');
      }
    });

    ws.on('close', () => {
      console.log('Client disconnected');
      clients.delete(ws);
    });

    ws.on('error', (error) => {
      console.error('WebSocket error:', error);
      clients.delete(ws);
    });
  });

  // Handle client messages
  function handleClientMessage(client: Client, message: WebSocketMessage): void {
    switch (message.type) {
      case 'subscribe':
        handleSubscribe(client, message as SubscribeMessage);
        break;
      case 'unsubscribe':
        handleUnsubscribe(client, message);
        break;
      case 'terminal_input':
        handleTerminalInput(message as TerminalInputMessage);
        break;
      default:
        sendError(client.ws, `Unknown message type: ${message.type}`);
    }
  }

  // Subscribe to job updates
  function handleSubscribe(client: Client, message: SubscribeMessage): void {
    const { jobId } = message.payload;
    client.jobIds.add(jobId);
    console.log(`Client subscribed to job ${jobId}`);
  }

  // Unsubscribe from job updates
  function handleUnsubscribe(client: Client, message: WebSocketMessage): void {
    const { jobId } = message.payload as { jobId: string };
    client.jobIds.delete(jobId);
    console.log(`Client unsubscribed from job ${jobId}`);
  }

  // Handle terminal input from client
  async function handleTerminalInput(message: TerminalInputMessage): Promise<void> {
    const { jobId, tool, data } = message.payload;
    console.log(`Terminal input for ${jobId}/${tool}: ${data}`);

    try {
      // Forward input to the sandbox runner
      const sandboxRunner = (jobManager as any).sandboxRunner;
      if (sandboxRunner && typeof sandboxRunner.sendInput === 'function') {
        await sandboxRunner.sendInput(jobId, tool, data);
      }
    } catch (error) {
      console.error('Error sending terminal input:', error);
    }
  }

  // Send error to client
  function sendError(ws: WebSocket, errorMessage: string): void {
    const message: ErrorMessage = {
      type: 'error',
      payload: {
        message: errorMessage,
      },
    };
    ws.send(JSON.stringify(message));
  }

  // Broadcast message to subscribed clients
  function broadcastToJob(jobId: string, message: WebSocketMessage): void {
    for (const [ws, client] of clients.entries()) {
      if (client.jobIds.has(jobId) && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(message));
      }
    }
  }

  // Listen to job manager events
  jobManager.on('job:started', (jobId: string) => {
    console.log(`Job ${jobId} started`);
  });

  jobManager.on('job:completed', (jobId: string) => {
    console.log(`Job ${jobId} completed`);
  });

  jobManager.on('job:failed', (jobId: string, error: Error) => {
    console.log(`Job ${jobId} failed:`, error);
  });

  // Listen to sandbox runner events for real-time updates
  const sandboxRunner = (jobManager as any).sandboxRunner;

  sandboxRunner.on('build:started', (data: { jobId: string; tool: string; timestamp: string }) => {
    const message: BuildStartedMessage = {
      type: 'build_started',
      payload: data,
    };
    broadcastToJob(data.jobId, message);
  });

  sandboxRunner.on('terminal:output', (output: TerminalOutputMessage['payload']) => {
    const message: TerminalOutputMessage = {
      type: 'terminal_output',
      payload: output,
    };
    broadcastToJob(output.jobId, message);
  });

  sandboxRunner.on('build:completed', (data: { jobId: string; tool: string; result: any }) => {
    const message: BuildCompletedMessage = {
      type: 'build_completed',
      payload: data,
    };
    broadcastToJob(data.jobId, message);
  });

  sandboxRunner.on('build:failed', (data: { jobId: string; tool: string; error: Error }) => {
    const message: ErrorMessage = {
      type: 'error',
      payload: {
        message: `Build failed: ${data.error.message}`,
      },
    };
    broadcastToJob(data.jobId, message);
  });

  sandboxRunner.on('build:bell', (data: { jobId: string; tool: string; timestamp: string; message?: string }) => {
    const message: BuildBellMessage = {
      type: 'build_bell',
      payload: data,
    };
    broadcastToJob(data.jobId, message);
    console.log(`🔔 Bell rung for ${data.tool} on job ${data.jobId}`);
  });

  console.log('WebSocket server setup complete');
}
