# Build Arena Architecture - Hot-Standby Design

## Overview

Build Arena uses a **hot-standby architecture** where lightweight "glue" servers run 24/7 and spawn isolated Docker containers on-demand for each build competition.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
            ┌──────────────────────┐
            │   CDN / Static Host  │
            │   (Vite Frontend)    │
            └──────────┬───────────┘
                       │
                       │ HTTP/WS
                       ▼
            ┌──────────────────────┐
            │    Glue Server       │◄─── Always running (hot)
            │  (Node.js/Express)   │     Lightweight (~50MB RAM)
            │                      │     Handles:
            │  • REST API          │     - Job submission
            │  • WebSocket proxy   │     - Container lifecycle
            │  • Container mgmt    │     - Terminal multiplexing
            └──────────┬───────────┘
                       │
                       │ Docker API
                       ▼
         ┌─────────────────────────────┐
         │      Docker Host            │
         │  (Containers on-demand)     │
         │                             │
         │  ┌──────────┐ ┌──────────┐ │
         │  │ Elide    │ │ Standard │ │◄─── Spawned per job
         │  │ Builder  │ │ Builder  │ │     Destroyed after
         │  │          │ │          │ │
         │  │ Claude   │ │ Claude   │ │
         │  │ Code     │ │ Code     │ │
         │  └──────────┘ └──────────┘ │
         └─────────────────────────────┘
```

## Components

### 1. Frontend (Vite + React)

**Purpose**: Static web application served via CDN

**Tech Stack**:
- Vite (dev server + build tool)
- React 18
- xterm.js (terminal emulation)
- TailwindCSS

**Deployment**:
- Built to static files: `npm run build`
- Served from CDN (Cloudflare, Vercel, Netlify, etc.)
- No runtime needed - pure static files

**Configuration**:
```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:3001',  // Dev mode only
    '/ws': { target: 'ws://localhost:3001', ws: true }
  }
}
```

### 2. Glue Server (Always Hot)

**Purpose**: Lightweight orchestration layer that manages container lifecycle

**Characteristics**:
- Always running (24/7)
- Minimal resource usage (~50MB RAM, <1% CPU when idle)
- Stateless (job state in memory, ephemeral)
- Single instance handles all requests
- Can run on cheap hardware ($5/month VPS)

**Responsibilities**:
1. **API Endpoints**:
   - `POST /api/jobs` - Create build job, spawn containers
   - `GET /api/jobs/:id` - Get job status
   - `POST /api/jobs/:id/cancel` - Stop and cleanup containers

2. **WebSocket Server**:
   - Multiplexes terminal streams from multiple containers
   - Routes client input to appropriate container
   - Broadcasts events (bell, completion, errors)

3. **Container Management**:
   - Spawns Docker containers on-demand
   - Monitors container health
   - Cleans up completed/failed containers
   - Enforces resource limits and timeouts

**Tech Stack**:
- Node.js + TypeScript
- Express (REST API)
- ws (WebSocket)
- Dockerode (Docker API client)

**File**: `backend/src/index.ts`

### 3. Build Containers (On-Demand)

**Purpose**: Isolated build environments with Claude Code agents

**Characteristics**:
- Spawned per build job (2 containers per job)
- Short-lived (typically 2-10 minutes)
- Pre-built images stored on Docker host
- Resource-limited (4GB RAM, 2 CPU cores each)
- Full internet access

**Lifecycle**:
```
User submits job
    ↓
Glue server receives request
    ↓
Spawns 2 containers (Elide + Standard)
    ↓
Containers run Claude Code agents
    ↓
Agents build, test, ring bell
    ↓
Containers exit
    ↓
Glue server cleanup & metrics
```

**Images**:
- `elide-builder:latest` (~2GB)
- `standard-builder:latest` (~2GB)

Pre-built and cached on Docker host

## Hot-Standby Benefits

### Why Keep Glue Server Hot?

1. **Instant Response**: No cold start delay
2. **WebSocket Persistence**: Can maintain long-lived connections
3. **Simple Deployment**: Single process to manage
4. **Low Cost**: Minimal resources when idle
5. **Connection Pooling**: Reuse Docker API connections

### Why Containers On-Demand?

1. **Cost Effective**: Only pay for compute when building
2. **Isolation**: Each job gets clean environment
3. **Scalability**: Can run many jobs in parallel
4. **Security**: Containers destroyed after use
5. **Resource Efficiency**: No idle Claude Code instances

## Resource Requirements

### Development (Local)

```
Glue Server:      50MB RAM, <1% CPU
Frontend (Vite):  100MB RAM during dev
Docker Images:    4GB disk (cached)
Running Jobs:     8GB RAM per job (2 containers × 4GB)
```

**Total for 1 concurrent job**: ~8GB RAM

### Production

**Glue Server (Always On)**:
- 1 vCPU
- 1GB RAM
- 10GB disk (for Docker images)
- $5-10/month (Hetzner, DigitalOcean, Linode)

**Docker Host (For Builds)**:
- Can be same machine as glue server for demos
- Or separate powerful machine for production
- Need 8GB RAM per concurrent job
- Example: 32GB machine = 4 concurrent jobs

**Frontend (Static)**:
- Served from CDN
- Free tier on Cloudflare/Netlify
- Or $5/month for custom domain

**Total**: $5-20/month depending on scale

## Deployment Scenarios

### Scenario 1: All-In-One Demo (Recommended)

Single $10/month VPS runs everything:

```
Hetzner CPX21:
- 3 vCPUs
- 4GB RAM
- 80GB disk
- €4.51/month (~$5)

Runs:
- Glue server
- Docker daemon
- Nginx (reverse proxy)
- 1 concurrent build job
```

### Scenario 2: Separated (Production)

```
Frontend: Cloudflare Pages (Free)
    ↓ HTTPS
Glue Server: $5 VPS (API + WebSocket)
    ↓ Docker API
Build Host: $20 VPS (8GB RAM, runs containers)
```

### Scenario 3: Auto-Scaling (Future)

```
Frontend: CDN
    ↓
Glue Server: Cloud Run (serverless, scales to zero)
    ↓
Build Containers: Cloud Run jobs (on-demand)
```

## Data Flow

### Job Submission Flow

```
1. User clicks "Start Build"
   Frontend → POST /api/jobs { repositoryUrl }

2. Glue server receives request
   - Generates job ID
   - Spawns 2 Docker containers
   - Injects CLAUDE.md files
   - Starts PTY sessions
   - Returns job ID

3. Frontend connects WebSocket
   - Subscribes to job ID
   - Starts receiving terminal output

4. Claude Code agents work
   - Clone repo
   - Install dependencies
   - Build project
   - Run tests
   - Ring bell

5. Glue server streams output
   - Multiplexes 2 terminal streams
   - Detects bell characters
   - Broadcasts events to clients

6. Containers finish
   - Glue server collects metrics
   - Destroys containers
   - Returns final results
```

### Terminal Streaming Flow

```
Container 1 (Elide) → stdout → Glue Server → WebSocket → Browser
Container 2 (Standard) → stdout → Glue Server → WebSocket → Browser

Browser (input) → WebSocket → Glue Server → Container PTY → Claude Code
```

## Code Organization

```
tools/build-arena/
├── frontend-vite/          # Vite-based frontend (static build)
│   ├── src/
│   │   ├── main.tsx        # Entry point
│   │   ├── App.tsx         # Main app component
│   │   └── components/     # React components
│   ├── vite.config.ts      # Vite configuration
│   └── package.json
│
├── backend/                # Glue server (always hot)
│   ├── src/
│   │   ├── index.ts        # Express + WebSocket setup
│   │   ├── routes/api.ts   # REST endpoints
│   │   ├── services/
│   │   │   ├── job-manager.ts           # Job lifecycle
│   │   │   └── interactive-sandbox-runner.ts  # Container mgmt
│   │   └── websocket/server.ts  # WebSocket multiplexing
│   └── package.json
│
├── docker/                 # Pre-built container images
│   ├── elide-builder.Dockerfile
│   ├── standard-builder.Dockerfile
│   ├── CLAUDE-elide.md     # Agent instructions
│   └── CLAUDE-standard.md
│
└── shared/
    └── types.ts            # Shared TypeScript types
```

## API Specification

### REST API

**POST /api/jobs**
```json
Request:
{
  "repositoryUrl": "https://github.com/user/repo.git"
}

Response:
{
  "jobId": "uuid",
  "message": "Job created successfully"
}
```

**GET /api/jobs/:jobId**
```json
Response:
{
  "job": {
    "id": "uuid",
    "repositoryUrl": "...",
    "status": "running",
    "elideResult": { ... },
    "standardResult": { ... }
  }
}
```

### WebSocket API

**Connect**: `ws://server/ws`

**Subscribe**:
```json
{
  "type": "subscribe",
  "payload": { "jobId": "uuid" }
}
```

**Events Received**:
- `terminal_output` - Terminal data
- `build_bell` - Agent rang bell
- `build_completed` - Build finished
- `error` - Error occurred

## Performance Characteristics

### Glue Server (Idle)

```
RAM: ~50MB
CPU: <1%
Network: Minimal
Response time: <50ms
```

### Glue Server (Under Load, 5 concurrent jobs)

```
RAM: ~100MB (job state is lightweight)
CPU: 5-10% (mostly I/O waiting)
Network: ~1MB/s (terminal streams)
Response time: <100ms
```

### Container Spawn Time

```
First spawn: ~2-3 seconds (image pull if needed)
Subsequent: ~500ms (image cached)
```

### End-to-End Latency

```
Job submission → Container running: ~1-2 seconds
Terminal keystroke → Container: ~50ms
Container output → Browser: ~100ms
```

## Security Considerations

### Glue Server

- Rate limiting on API endpoints
- WebSocket connection limits
- Job timeout enforcement (default: 10 minutes)
- Container resource limits

### Containers

- No host filesystem access
- Network isolation (bridge mode)
- Resource limits (RAM, CPU, time)
- Read-only root filesystem (future)
- Non-root user (future)
- Seccomp profiles (future)

### API Keys

- Claude API key stored in glue server env only
- Never sent to frontend
- Injected into containers at spawn time
- Containers destroyed after use (no persistence)

## Monitoring & Observability

### Metrics to Track

**Glue Server**:
- Request rate (jobs/minute)
- Active WebSocket connections
- Container spawn/cleanup rate
- Memory usage
- API response times

**Containers**:
- Success/failure rate
- Average build time (per tool)
- Resource usage (RAM, CPU)
- Bell ring times
- Timeout rate

### Logging

**Glue Server**:
```
[2025-01-06 10:30:15] Job created: job-123
[2025-01-06 10:30:16] Spawned containers for job-123
[2025-01-06 10:30:20] Bell detected: job-123/elide
[2025-01-06 10:30:35] Job completed: job-123 (elide: 20s, standard: 35s)
```

**Container Logs**:
- Streamed to glue server
- Forwarded to WebSocket clients
- Optionally persisted (future feature)

## Scaling Strategy

### Horizontal Scaling (Future)

```
         Load Balancer
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
  Glue-1   Glue-2   Glue-3
    │         │         │
    └─────────┼─────────┘
              ▼
         Docker Swarm
         (Shared pool of build hosts)
```

**Challenges**:
- WebSocket sticky sessions
- Shared job state (Redis)
- Container placement

**When to scale**:
- \>100 concurrent jobs
- \>1000 requests/minute
- Geographic distribution needed

## Development Workflow

### Local Development

```bash
# Terminal 1: Start glue server
cd backend
npm run dev

# Terminal 2: Start Vite frontend
cd frontend-vite
npm run dev

# Terminal 3: Build Docker images (once)
cd docker
./build-images.sh
```

Access: `http://localhost:3000`

### Production Build

```bash
# Build frontend static files
cd frontend-vite
npm run build
# Output: dist/ folder

# Deploy to CDN
# (Cloudflare Pages, Netlify, Vercel, etc.)

# Deploy glue server
cd backend
npm run build
node dist/index.js
# Or use PM2, systemd, Docker, etc.
```

## Future Enhancements

1. **Container Pool**: Pre-warm containers for faster starts
2. **Build Cache**: Share dependencies between builds
3. **Replay System**: Record sessions for later viewing
4. **Analytics Dashboard**: Aggregate metrics and trends
5. **Multi-Region**: Deploy globally for low latency
6. **Auto-Scaling**: Scale containers based on demand
7. **Persistent State**: Store job history in database

## Summary

The hot-standby architecture provides:

✅ **Low cost**: $5-10/month for demo/small-scale
✅ **Fast response**: Glue server always ready
✅ **Scalable**: Add more resources as needed
✅ **Secure**: Containers isolated and ephemeral
✅ **Simple**: Single glue server process to manage
✅ **Flexible**: Easy to move/redeploy components

Perfect for a sales demo that needs to be:
- Always available
- Quick to respond
- Cheap to run
- Easy to maintain
