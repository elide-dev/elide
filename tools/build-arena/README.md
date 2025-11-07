# Build Arena

An interactive AI-driven demonstration platform showcasing Elide's build performance against standard toolchains.

## Overview

Build Arena is a **live, AI-powered build competition** where autonomous Claude Code agents race to build and validate Java projects. Users submit public repositories and watch in real-time as two AI agents compete:

- **Elide Team** - Claude Code agent using the optimized Elide polyruntime toolchain
- **Standard Team** - Claude Code agent using traditional Maven/Gradle tools

Each agent has **full autonomy**: they can search the internet, install packages, optimize configurations, and use any strategy to build faster. When they're confident the build succeeded, they "ring the bell" 🔔

## Architecture

```
tools/build-arena/
├── frontend/          # React/Next.js web application
├── backend/           # TypeScript/Node.js API server
├── shared/            # Shared types and utilities
├── docker/            # Docker configurations for sandboxes
└── README.md
```

### Components

1. **Frontend** - Web UI with dual terminal displays, repository submission form
2. **Backend API** - Job management, WebSocket server, sandbox orchestration
3. **Sandbox Runner** - Docker-based isolated build environments
4. **WebSocket Server** - Real-time terminal output streaming

## Technology Stack

- **Frontend**: React/Next.js, xterm.js for terminal emulation
- **Backend**: TypeScript, Node.js, Express, ws (WebSocket)
- **Sandbox**: Docker containers with resource limits
- **Streaming**: WebSocket for bi-directional real-time communication

## Quick Start

**Prerequisites**: Node.js 20+, pnpm, Docker

```bash
# First-time setup (installs dependencies, builds Docker images)
./dev-pnpm.sh setup

# Start development (backend + frontend in parallel)
./dev-pnpm.sh dev

# Run tests
pnpm test
```

The application will be available at:
- **Frontend (Vite)**: http://localhost:5173
- **Glue Server API**: http://localhost:3001
- **WebSocket**: ws://localhost:3001/ws

### Available Commands

```bash
./dev-pnpm.sh dev        # Start both servers
./dev-pnpm.sh backend    # Start backend only
./dev-pnpm.sh frontend   # Start frontend only
./dev-pnpm.sh lint       # Run Biome linter
./dev-pnpm.sh format     # Format code with Biome
./dev-pnpm.sh test       # Run Playwright tests
./dev-pnpm.sh test:ui    # Open Playwright UI
```

### Architecture

Build Arena uses a **hot-standby architecture**:
- **Glue Server**: Lightweight Node.js server (always running) that spawns containers on-demand
- **Frontend**: Vite-based React app (can be built to static files for CDN deployment)
- **Build Containers**: Docker containers with Claude Code agents (spawned per job, destroyed after)

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed information.

## Technology Stack

**Frontend**:
- Vite - Lightning-fast dev server and build tool
- React 18 - UI framework
- xterm.js - Terminal emulation
- TailwindCSS - Styling

**Backend**:
- Node.js + TypeScript
- Express - REST API
- ws - WebSocket server
- Dockerode - Docker orchestration

**Quality Tools**:
- **pnpm** - Fast, efficient package manager
- **Biome** - Lightning-fast linter & formatter (Rust-based)
- **Playwright** - E2E testing with multiple browsers

**Infrastructure**:
- Docker - Container isolation
- Claude Code - AI agents

## Key Features

- 🤖 **Autonomous AI Agents** - Claude Code agents with full terminal access
- 🌐 **Internet Access** - Agents can search docs, Stack Overflow, install packages
- 🔔 **Bell System** - Agents ring bell when build succeeds
- 📊 **Live Metrics** - Real-time performance comparison (time, memory, CPU)
- 💻 **Interactive Terminals** - Watch agents work with xterm.js
- 🛡️ **Sandboxed** - Docker containers with resource limits
- ⚡ **Parallel Execution** - Both agents run simultaneously

## How It Works

1. **Submit Repository** - User enters a public Java repository URL
2. **Agents Launch** - Two Docker containers spin up with Claude Code
3. **Autonomous Building** - Agents independently:
   - Clone the repository
   - Detect build system (Maven/Gradle)
   - Install missing dependencies
   - Search for solutions to errors
   - Optimize build configurations
   - Run tests
4. **Ring the Bell** - First agent to validate the build rings the bell 🔔
5. **Winner Declared** - Fastest build time wins!

## Documentation

See these guides for more information:

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Hot-standby design, deployment scenarios, scaling
- **[SETUP.md](./SETUP.md)** - Installation, configuration, and deployment
- **[INTERACTIVE_AGENTS.md](./INTERACTIVE_AGENTS.md)** - How AI agents work, autonomy, and customization
- **[TESTING.md](./TESTING.md)** - Comprehensive Playwright testing guide
- **[Docker files](./docker/)** - Container configurations and CLAUDE.md instructions

### Quick Links

- Hot-standby architecture → [ARCHITECTURE.md](./ARCHITECTURE.md)
- Testing guide → [TESTING.md](./TESTING.md)
- Deployment scenarios → [ARCHITECTURE.md#deployment-scenarios](./ARCHITECTURE.md)
- API reference → [SETUP.md#api-reference](./SETUP.md)
- Agent instructions → [INTERACTIVE_AGENTS.md#agent-instructions](./INTERACTIVE_AGENTS.md)

### Cost & Requirements

**Development**: Just your local machine (8GB RAM recommended)

**Production (Demo/Small Scale)**:
- **$5-10/month VPS** (Hetzner, DigitalOcean, Linode)
- Runs glue server + Docker containers
- Handles ~5-10 concurrent builds
- Frontend deployed to free CDN (Cloudflare/Netlify)

See [ARCHITECTURE.md#deployment-scenarios](./ARCHITECTURE.md#deployment-scenarios) for details.
