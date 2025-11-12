
import { createServer } from "http";
import { Database } from "elide:sqlite";
import { handleApiRequest } from "./server.ts";
import config from "./config.ts";

/**
 * Database Studio - Entry Point (Imperative Server)
 *
 * Creates an HTTP server using Node.js http module with explicit binding.
 * This is an imperative server that calls createServer() and listen() directly.
 */

export { Database };

// Configuration loaded from config.ts
const { port, databases } = config;

// Server options with self-signed certificate for HTTPS/HTTP3
const certificate = {
  kind: 'selfSigned',
  subject: 'localhost'
};

const options = {
  elide: {
    https: { certificate },
    http3: { certificate },
  }
};

// Create HTTP server
const server = createServer(options, (req, res) => {
  let body = '';

  req.on('data', (chunk) => {
    body += chunk.toString('utf8');
  });

  req.on('end', async () => {
    try {
      const url = req.url || '/';
      const method = req.method || 'GET';

      const response = await handleApiRequest(url, method, body, databases, Database);

      res.writeHead(response.status, {
        ...response.headers,
        'Content-Length': Buffer.byteLength(response.body, 'utf8')
      });
      res.end(response.body);
    } catch (err) {
      console.error("Error handling request:", err);
      const errorBody = JSON.stringify({ error: 'Internal server error' });
      res.writeHead(500, {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(errorBody, 'utf8')
      });
      res.end(errorBody);
    }
  });
});

// Start listening on configured port
server.listen(port, () => {
  console.log(`Database Studio API started on http://localhost:${port} 🚀`);
});