// Elide Context Engine MCP Server (stdio JSON-RPC)
// Transport: stdio; Tools are registered and dispatched here.

// Feature mode
const MODE = (Polyglot.import && Polyglot.import('process')?.env?.ELIDE_MCP_MODE) || 'augment';

// Simple JSON-RPC loop over stdio
const { stdin, stdout } = (typeof process !== 'undefined') ? process : Polyglot.import('process');
let tools = [];

function modeFlags(mode) {
  const base = { embeddings: false };
  if (mode === 'universal') return { embeddings: true };
  if (mode === 'custom') return { embeddings: (Polyglot.import('process').env.ELIDE_MCP_EMBEDDINGS === 'on') };
  return base; // augment default
}

const FLAGS = modeFlags(MODE);

// Register tools (tool modules are loaded lazily)
function listTools() {
  const list = [
    {
      name: 'memory_suggest',
      title: 'Suggest project memories',
      description: 'Analyze text inputs to propose structured .mdc memory entries.',
      inputSchema: { type: 'object', properties: { text: { type: 'string' } }, required: ['text'] }
    },
    {
      name: 'memory_update',
      title: 'Apply memory updates',
      description: 'Write approved memory entries to disk (.mdc files).',
      inputSchema: { type: 'object', properties: { entries: { type: 'array' }, file: { type: 'string' } }, required: ['entries'] }
    },
    {
      name: 'memory_search',
      title: 'Search memories',
      description: 'Search across .mdc files (keyword; semantic optional).',
      inputSchema: { type: 'object', properties: { query: { type: 'string' } }, required: ['query'] }
    },
    {
      name: 'code_analyze',
      title: 'Analyze code basics',
      description: 'Static analysis summary (language heuristics).',
      inputSchema: { type: 'object', properties: { path: { type: 'string' } }, required: ['path'] }
    },
  ];
  if (FLAGS.embeddings) {
    list.push({
      name: 'semantic_search',
      title: 'Semantic search (local embeddings)',
      description: 'Search using local embeddings cache.',
      inputSchema: { type: 'object', properties: { query: { type: 'string' } }, required: ['query'] }
    });
  }
  return list;
}

async function callTool(name, args) {
  switch (name) {
    case 'memory_suggest': {
      const suggest = Polyglot.eval('python', `
import re
from typing import List, Dict

def extract_memories(text: str) -> List[Dict]:
    lines = [l.strip() for l in text.split('\n') if l.strip()]
    out = []
    for l in lines:
        if any(k in l.lower() for k in ['decision', 'rule', 'note', 'todo', 'guideline']):
            out.append({'type': 'note', 'text': l, 'tags': ['auto']})
        elif re.search(r'\b(API|endpoint|config|path)\b', l, re.I):
            out.append({'type': 'config', 'text': l, 'tags': ['auto']})
    # dedupe by text
    seen = set()
    dedup = []
    for e in out:
        if e['text'] not in seen:
            dedup.append(e)
            seen.add(e['text'])
    return dedup
extract_memories
`);
      const entries = suggest(args.text || '');
      return { content: [{ type: 'text', text: JSON.stringify(entries) }], structuredContent: { entries } };
    }
    case 'memory_update': {
      // Use Java NIO for portability
      const Paths = Java.type('java.nio.file.Paths');
      const Files = Java.type('java.nio.file.Files');
      const StandardOpenOption = Java.type('java.nio.file.StandardOpenOption');
      const JString = Java.type('java.lang.String');
      const file = args.file || '.mcp/memory/project.mdc';
      const p = Paths.get(file);
      const parent = p.getParent();
      if (parent && !Files.exists(parent)) Files.createDirectories(parent);
      const jsLines = (args.entries || []).map(e => `- [${e.type}] ${e.text}`);
      const payload = new JString(jsLines.join('\n') + '\n');
      Files.write(p, payload.getBytes(), Java.to([StandardOpenOption.CREATE, StandardOpenOption.APPEND], 'java.nio.file.OpenOption[]'));
      return { content: [{ type: 'text', text: `Wrote ${jsLines.length} entries to ${file}` }] };
    }
    case 'memory_search': {
      const Files = Java.type('java.nio.file.Files');
      const Paths = Java.type('java.nio.file.Paths');
      const StandardCharsets = Java.type('java.nio.charset.StandardCharsets');
      const path = Paths.get('.mcp/memory');
      let results = [];
      if (Files.exists(path)) {
        const stream = Files.walk(path);
        const it = stream.iterator();
        while (it.hasNext()) {
          const p = it.next();
          if (Files.isRegularFile(p) && p.toString().endsWith('.mdc')) {
            const bytes = Files.readAllBytes(p);
            const JString = Java.type('java.lang.String');
            const text = new JString(bytes, StandardCharsets.UTF_8);
            if (text.toLowerCase().contains(String(args.query || '').toLowerCase())) {
              results.push({ file: p.toString(), excerpt: text.substring(0, Math.min(200, text.length())) });
            }
          }
        }
        stream.close();
      }
      return { content: [{ type: 'text', text: JSON.stringify(results) }], structuredContent: { results } };
    }
    case 'code_analyze': {
      const Files = Java.type('java.nio.file.Files');
      const Paths = Java.type('java.nio.file.Paths');
      const p = Paths.get(args.path);
      const isDir = Files.isDirectory(p);
      let count = 0, bytes = 0;
      const exts = ['.ts','.tsx','.js','.jsx','.py','.kt','.java','.rb','.go','.rs','.c','.cpp'];
      if (isDir) {
        const stream = Files.walk(p);
        const it = stream.iterator();
        while (it.hasNext()) {
          const fp = it.next();
          if (Files.isRegularFile(fp)) {
            const name = fp.toString();
            if (exts.some(e => name.endsWith(e))) {
              count += 1;
              bytes += Files.size(fp);
            }
          }
        }
        stream.close();
      } else if (Files.isRegularFile(p)) {
        count = 1; bytes = Files.size(p);
      }
      return { content: [{ type: 'text', text: `files=${count}, bytes=${bytes}` }], structuredContent: { files: count, bytes } };
    }
    case 'semantic_search': {
      if (!FLAGS.embeddings) return { isError: true, content: [{ type: 'text', text: 'Embeddings disabled in current mode' }] };
      return { content: [{ type: 'text', text: 'TODO: semantic search (Phase 2)'}] };
    }
    default:
      return { isError: true, content: [{ type: 'text', text: `Unknown tool: ${name}` }] };
  }
}

function write(resp) {
  const pkt = JSON.stringify(resp) + '\n';
  stdout.write(pkt);
}

function handle(req) {
  try {
    if (req.method === 'initialize') {
      return { jsonrpc: '2.0', id: req.id, result: { capabilities: { tools: { listChanged: false } } } };
    }
    if (req.method === 'tools/list') {
      return { jsonrpc: '2.0', id: req.id, result: { tools: listTools() } };
    }
    if (req.method === 'tools/call') {
      return Promise.resolve(callTool(req.params.name, req.params.arguments || {})).then(r => ({ jsonrpc: '2.0', id: req.id, result: r }));
    }
    return { jsonrpc: '2.0', id: req.id, error: { code: -32601, message: 'Method not found' } };
  } catch (e) {
    return { jsonrpc: '2.0', id: req?.id, error: { code: -32000, message: String(e) } };
  }
}

let buffer = '';
stdin.setEncoding('utf8');
stdin.on('data', async (chunk) => {
  buffer += chunk;
  let idx;
  while ((idx = buffer.indexOf('\n')) >= 0) {
    const line = buffer.slice(0, idx);
    buffer = buffer.slice(idx + 1);
    if (!line.trim()) continue;
    let req;
    try { req = JSON.parse(line); } catch (e) { write({ jsonrpc: '2.0', id: null, error: { code: -32700, message: 'Parse error' } }); continue; }
    const resp = await handle(req);
    write(resp);
  }
});

