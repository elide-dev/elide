import express from 'express';
import cors from 'cors';
import { WebSocketServer } from 'ws';
import { createServer } from 'http';
import { apiRouter } from './routes/api.js';
import { testApiRouter } from './routes/test-api.js';
import { raceApiRouter } from './routes/race-api.js';
import { statusApiRouter } from './routes/status-api.js';
import { setupWebSocketServer } from './websocket/server.js';
import { setupTerminalWebSocketServer } from './websocket/terminal-server.js';
import { setupRaceWebSocketServer } from './websocket/race-server.js';

const app = express();
const server = createServer(app);

// Single WebSocket server for all WebSocket connections
// We'll route based on the URL path in each handler
const wss = new WebSocketServer({ server });

// Middleware
app.use(cors());
app.use(express.json());

// Health check
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// API routes
app.use('/api', apiRouter);
app.use('/api', testApiRouter);
app.use('/api/races', raceApiRouter);
app.use('/api/status', statusApiRouter);

// Setup WebSocket servers with path-based routing
setupWebSocketServer(wss);
setupTerminalWebSocketServer(wss);
setupRaceWebSocketServer(wss);

const PORT = process.env.PORT || 3001;

server.listen(PORT, () => {
  console.log(`Build Arena backend running on port ${PORT}`);
  console.log(`WebSocket server available at ws://localhost:${PORT}/ws`);
});
