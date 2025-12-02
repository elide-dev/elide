import { createServer, IncomingMessage, ServerResponse } from "http";
import { handleApiRequest } from "./http/server.ts";
import { errorResponse } from "./http/responses.ts";
import type { ApiResponse } from "./http/types.ts";
import config from "./config.ts";

/**
 * Database Studio - Entry Point
 *
 * Bootstraps the HTTP server for the Database Studio API.
 */

const { port, databases } = config;

/**
 * Parses the request body from an incoming HTTP request
 */
function parseRequestBody(req: IncomingMessage): Promise<string> {
  return new Promise((resolve, reject) => {
    let body = '';
    
    req.on('data', (chunk) => {
      body += chunk.toString('utf8');
    });
    
    req.on('end', () => resolve(body));
    req.on('error', (err) => reject(err));
  });
}

/**
 * Writes an ApiResponse to the HTTP ServerResponse
 */
function writeResponse(res: ServerResponse, response: ApiResponse): void {
  res.writeHead(response.status, {
    ...response.headers,
    'Content-Length': Buffer.byteLength(response.body, 'utf8')
  });
  res.end(response.body);
}

/**
 * Main request handler - processes incoming HTTP requests
 */
async function handleRequest(req: IncomingMessage, res: ServerResponse): Promise<void> {
  try {
    const body = await parseRequestBody(req);
    const url = req.url || '/';
    const method = req.method || 'GET';

    const response = await handleApiRequest(url, method, body, databases);
    writeResponse(res, response);
  } catch (err) {
    console.error("Error handling request:", err);
    const response = errorResponse('Internal server error', 500);
    writeResponse(res, response);
  }
}

// Create and configure HTTP server
const server = createServer(handleRequest);

// Start listening on configured port
server.listen(port, () => {
  console.log(`Database Studio API started on http://localhost:${port} 🚀`);
});