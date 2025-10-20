// Minimal smoke test for stdio JSON-RPC loop
import { spawn } from 'node:child_process';

const child = spawn('/home/pug/elide/elide', ['./src/server.mjs'], { cwd: '/home/pug/code/elide-context-engine-mcp' });

function rpc(id, method, params) {
  const pkt = JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n';
  child.stdin.write(pkt);
}

child.stdout.setEncoding('utf8');
child.stdout.on('data', (chunk) => {
  for (const line of chunk.split('\n')) {
    if (!line.trim()) continue;
    console.log('OUT:', line);
  }
});

child.stderr.setEncoding('utf8');
child.stderr.on('data', (d) => console.error('ERR:', d));

setTimeout(() => rpc(1, 'initialize', {}), 250);
setTimeout(() => rpc(2, 'tools/list', {}), 500);
setTimeout(() => rpc(3, 'tools/call', { name: 'memory_suggest', arguments: { text: 'Decision: Use stdio. API: /v1/tools' } }), 750);
setTimeout(() => rpc(4, 'tools/call', { name: 'memory_update', arguments: { entries: [{ type: 'note', text: 'Use stdio MCP by default' }], file: '.mcp/memory/test.mdc' } }), 1000);
setTimeout(() => rpc(5, 'tools/call', { name: 'memory_search', arguments: { query: 'stdio' } }), 1250);
setTimeout(() => child.kill('SIGTERM'), 2500);

