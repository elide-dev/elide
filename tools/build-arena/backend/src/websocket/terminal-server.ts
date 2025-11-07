import { WebSocketServer, WebSocket } from 'ws';
import Docker from 'dockerode';
import { IncomingMessage } from 'http';

const docker = new Docker();

interface TerminalClient {
  ws: WebSocket;
  containerId: string;
  exec?: Docker.Exec;
  stream?: NodeJS.ReadWriteStream;
}

let terminalHandlerRegistered = false;

/**
 * Setup WebSocket server for direct terminal connections
 */
export function setupTerminalWebSocketServer(wss: WebSocketServer): void {
  // Prevent duplicate handler registration
  if (terminalHandlerRegistered) {
    return;
  }
  terminalHandlerRegistered = true;

  const clients = new Map<WebSocket, TerminalClient>();

  wss.on('connection', async (ws: WebSocket, req: IncomingMessage) => {
    // Only handle terminal WebSocket connections
    if (!req.url?.startsWith('/ws/terminal/')) {
      // Let other handlers deal with this connection
      return;
    }

    // Extract container ID from path: /ws/terminal/:containerId
    const match = req.url?.match(/\/ws\/terminal\/([a-f0-9]+)/);
    if (!match) {
      ws.close(1008, 'Invalid URL: missing container ID');
      return;
    }

    const containerId = match[1];
    console.log(`Terminal WebSocket connected for container: ${containerId}`);

    const client: TerminalClient = {
      ws,
      containerId,
    };

    clients.set(ws, client);

    try {
      // Get container
      const container = docker.getContainer(containerId);

      // Create exec instance for interactive bash
      const exec = await container.exec({
        Cmd: ['/bin/bash'],
        AttachStdin: true,
        AttachStdout: true,
        AttachStderr: true,
        Tty: true,
        Env: ['TERM=xterm-256color'],
      });

      // Start exec and get stream
      const stream = await exec.start({
        Tty: true,
        stdin: true,
        hijack: true, // Important: hijack the connection for interactive use
      }) as NodeJS.ReadWriteStream;

      client.exec = exec;
      client.stream = stream;

      // Set encoding for proper text handling
      stream.setEncoding('utf8');

      // Forward container output to WebSocket
      stream.on('data', (data: string | Buffer) => {
        const output = typeof data === 'string' ? data : data.toString('utf-8');
        console.log(`Container ${containerId} output:`, JSON.stringify(output));
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'output',
            data: output,
          }));
        }
      });

      stream.on('end', () => {
        console.log(`Stream ended for container: ${containerId}`);
        if (ws.readyState === WebSocket.OPEN) {
          ws.close(1000, 'Stream ended');
        }
      });

      stream.on('error', (error: Error) => {
        console.error(`Stream error for container ${containerId}:`, error);
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'error',
            message: error.message,
          }));
        }
      });

      // Handle messages from WebSocket (user input)
      ws.on('message', (data: Buffer) => {
        try {
          const message = JSON.parse(data.toString());
          console.log(`Terminal input for ${containerId}:`, message);

          if (message.type === 'input' && stream) {
            // Write user input to container stdin
            console.log(`Writing to container stdin:`, JSON.stringify(message.data));
            stream.write(message.data);
          }
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      });

    } catch (error) {
      console.error(`Error setting up terminal for container ${containerId}:`, error);
      ws.send(JSON.stringify({
        type: 'error',
        message: `Failed to connect to container: ${error instanceof Error ? error.message : 'Unknown error'}`,
      }));
      ws.close(1011, 'Internal error');
      clients.delete(ws);
      return;
    }

    ws.on('close', () => {
      console.log(`Terminal WebSocket disconnected for container: ${containerId}`);

      // Clean up stream
      if (client.stream) {
        client.stream.end();
      }

      clients.delete(ws);
    });

    ws.on('error', (error) => {
      console.error(`WebSocket error for container ${containerId}:`, error);
      clients.delete(ws);
    });
  });

  console.log('Terminal WebSocket server setup complete');
}
