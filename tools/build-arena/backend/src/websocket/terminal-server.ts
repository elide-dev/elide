import { WebSocketServer, WebSocket } from 'ws';
import Docker from 'dockerode';
import { IncomingMessage } from 'http';
import { WebSocketRecorder } from '../services/websocket-recorder';

const docker = new Docker();

interface TerminalClient {
  ws: WebSocket;
  containerId: string;
  exec?: Docker.Exec;
  stream?: NodeJS.ReadWriteStream;
  recorder?: WebSocketRecorder;
  enableRecording?: boolean;
  isInteractive?: boolean; // Only interactive clients can send input
}

// Track all clients watching each container
interface ContainerSession {
  containerId: string;
  exec: Docker.Exec;
  stream: NodeJS.ReadWriteStream;
  clients: Set<TerminalClient>;
  recorder?: WebSocketRecorder;
  startTime: number; // Epoch timestamp when session started
}

let terminalHandlerRegistered = false;

/**
 * Setup WebSocket server for direct terminal connections
 * Supports multiple clients watching the same container (broadcast)
 */
export function setupTerminalWebSocketServer(wss: WebSocketServer): void {
  // Prevent duplicate handler registration
  if (terminalHandlerRegistered) {
    return;
  }
  terminalHandlerRegistered = true;

  const clients = new Map<WebSocket, TerminalClient>();
  const containerSessions = new Map<string, ContainerSession>();

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

    // Check for recording flag and interactive mode in query params
    const url = new URL(req.url || '', `http://${req.headers.host}`);
    const enableRecording = url.searchParams.get('record') === 'true';
    const isInteractive = url.searchParams.get('interactive') !== 'false'; // Default to interactive

    // Extract metadata for recording
    const jobId = url.searchParams.get('jobId') || containerId;
    const buildType = url.searchParams.get('buildType') || 'unknown';
    const repoUrl = url.searchParams.get('repoUrl') || 'unknown';
    const claudeVersion = url.searchParams.get('claudeVersion') || '2.0.35';
    const dockerImage = url.searchParams.get('dockerImage') || 'elide-builder:latest';

    console.log(`Terminal WebSocket connected for container: ${containerId} (recording: ${enableRecording}, interactive: ${isInteractive}, job: ${jobId})`);

    const client: TerminalClient = {
      ws,
      containerId,
      enableRecording,
      isInteractive,
    };

    clients.set(ws, client);

    // Check if we already have a session for this container
    let session = containerSessions.get(containerId);

    try {
      if (!session) {
        // First client for this container - create new session
        console.log(`Creating new session for container: ${containerId}`);

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

        // Set encoding for proper text handling
        stream.setEncoding('utf8');

        // Create session
        session = {
          containerId,
          exec,
          stream,
          clients: new Set<TerminalClient>(),
          startTime: Date.now(), // Record when this session started
        };

        // Initialize recorder if recording is enabled
        if (enableRecording) {
          session.recorder = new WebSocketRecorder(
            jobId,
            buildType,
            {
              jobId,
              tool: buildType,
              repositoryUrl: repoUrl,
              claudeVersion,
              dockerImage
            }
          );
          session.recorder.start();
          console.log(`Started recording for job ${jobId} (${buildType})`);
        }

        containerSessions.set(containerId, session);

        // Forward container output to ALL connected clients (broadcast)
        stream.on('data', (data: string | Buffer) => {
          const output = typeof data === 'string' ? data : data.toString('utf-8');

          // Log output (first 200 chars to avoid spam)
          const preview = output.length > 200 ? output.substring(0, 200) + '...' : output;
          console.log(`Container ${containerId} output (${session!.clients.size} clients):`, JSON.stringify(preview));

          const now = Date.now();
          const message = {
            type: 'output',
            data: output,
            timestamp: now, // When this message was generated
            startTime: session!.startTime, // When the session started
            elapsed: Math.floor((now - session!.startTime) / 1000), // Elapsed seconds
          };

          // Record message if recording is enabled
          if (session!.recorder) {
            session!.recorder.record(message);
          }

          // Broadcast to all clients watching this container
          session!.clients.forEach(client => {
            if (client.ws.readyState === WebSocket.OPEN) {
              client.ws.send(JSON.stringify(message));
            }
          });
        });

        stream.on('end', () => {
          console.log(`Stream ended for container: ${containerId}`);
          // Close all clients
          session!.clients.forEach(client => {
            if (client.ws.readyState === WebSocket.OPEN) {
              client.ws.close(1000, 'Stream ended');
            }
          });
          containerSessions.delete(containerId);
        });

        stream.on('error', (error: Error) => {
          console.error(`Stream error for container ${containerId}:`, error);
          const errorMessage = {
            type: 'error',
            message: error.message,
          };
          // Broadcast error to all clients
          session!.clients.forEach(client => {
            if (client.ws.readyState === WebSocket.OPEN) {
              client.ws.send(JSON.stringify(errorMessage));
            }
          });
        });
      } else {
        console.log(`Joining existing session for container: ${containerId} (${session.clients.size} existing clients)`);

        // Replay buffered history to new client if recorder exists
        if (session.recorder) {
          const messages = session.recorder.getMessages();
          if (messages.length > 0) {
            console.log(`Replaying ${messages.length} buffered messages to new client`);
            // Send all buffered messages to this client
            messages.forEach(({ msg }) => {
              if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify(msg));
              }
            });
          }
        }
      }

      // Add this client to the session
      session.clients.add(client);
      client.exec = session.exec;
      client.stream = session.stream;

      // Handle messages from WebSocket (user input)
      ws.on('message', (data: Buffer) => {
        try {
          const message = JSON.parse(data.toString());

          if (message.type === 'input') {
            // Only interactive clients can send input
            if (!client.isInteractive) {
              console.log(`Non-interactive client tried to send input - ignoring`);
              return;
            }

            console.log(`Terminal input for ${containerId}:`, message);

            if (session!.stream) {
              // Record input if recording is enabled
              if (session!.recorder) {
                session!.recorder.record({ type: 'input', data: message.data });
              }

              // Write user input to container stdin
              console.log(`Writing to container stdin:`, JSON.stringify(message.data));
              session!.stream.write(message.data);
            }
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

    ws.on('close', async () => {
      console.log(`Terminal WebSocket disconnected for container: ${containerId}`);

      // Remove client from session
      const session = containerSessions.get(containerId);
      if (session) {
        session.clients.delete(client);
        console.log(`Client removed from session. Remaining clients: ${session.clients.size}`);

        // If this was the last client, clean up the session
        if (session.clients.size === 0) {
          console.log(`Last client disconnected. Cleaning up session for container: ${containerId}`);

          // Stop and save recording if enabled
          if (session.recorder) {
            try {
              session.recorder.stop();
              // Import generateCacheKey from websocket-recorder
              const { generateCacheKey } = await import('../services/websocket-recorder.js');
              const { CONFIG } = await import('../config/constants.js');

              // Generate proper cache key from recorder metadata
              const metadata = (session.recorder as any).metadata;
              const cacheKey = generateCacheKey({
                repositoryUrl: metadata.repositoryUrl,
                tool: metadata.tool,
                claudeVersion: metadata.claudeVersion,
                dockerImage: metadata.dockerImage
              });

              const recordingsDir = CONFIG.RECORDINGS?.DIR || './recordings';
              const recordingPath = await session.recorder.save(cacheKey, recordingsDir);
              if (recordingPath) {
                console.log(`✅ Saved recording for job ${metadata.jobId} (${metadata.tool}): ${recordingPath}`);
                console.log(`   Messages: ${session.recorder.getMessageCount()}`);
                console.log(`   Duration: ${session.recorder.getDuration()}ms`);
                console.log(`   Bell rung: ${session.recorder.isBellRung()}`);
                console.log(`   Build successful: ${session.recorder.isBuildSuccessful()}`);
              } else {
                console.log(`⚠️  Recording not saved for job ${metadata.jobId} (${metadata.tool}) - build did not complete successfully`);
              }
            } catch (error) {
              console.error(`❌ Error saving recording for container ${containerId}:`, error);
            }
          }

          // Clean up stream
          if (session.stream) {
            session.stream.end();
          }

          containerSessions.delete(containerId);
        }
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
