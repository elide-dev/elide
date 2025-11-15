# Build Arena - Getting Started Guide

Welcome! This guide will help you get the Build Arena race demo running locally.

## What is Build Arena?

Build Arena is a side-by-side comparison tool that races **Elide** against **Maven/Gradle** on real Java projects. You can watch both build tools compete in real-time with live terminal output, or replay previous races.

## Prerequisites

Make sure you have these installed:

- **Docker Desktop** (running)
- **Node.js** 18+ and **pnpm**
- **Git**
- **Anthropic API Key** (for Claude Code)

## Quick Start (5 minutes)

### 1. Checkout the Branch

```bash
cd /path/to/elide
git checkout robb/1106-nomad
cd tools/build-arena
```

### 2. Set Up Environment

Create a `.env` file in the `backend/` directory:

```bash
cd backend
cat > .env << 'EOF'
ANTHROPIC_API_KEY=your_api_key_here
EOF
cd ..
```

**Important**: Replace `your_api_key_here` with your actual Anthropic API key.

### 3. Install Dependencies

```bash
pnpm install
```

### 4. Build Docker Images

This takes ~5-10 minutes the first time:

```bash
cd docker
./build-images.sh
cd ..
```

This builds two Docker images:
- `elide-builder:latest` - Container with Elide installed
- `standard-builder:latest` - Container with Maven/Gradle

### 5. Initialize Database

```bash
pnpm --filter @elide/build-arena-backend db:push
```

### 6. Start the Servers

```bash
pnpm dev
```

This starts:
- **Backend** (port 3001) - API and WebSocket servers
- **Frontend** (port 3000) - React app with Vite

Wait for both servers to show "ready" messages.

### 7. Open the Race Page

```bash
open http://localhost:3000/race
```

Or visit http://localhost:3000/race in your browser.

## Running Your First Race

### Option 1: Use a Suggested Repository

1. The race page shows 5 suggested repositories
2. Click any suggestion (e.g., "Google Gson")
3. Click "Start Race"
4. Watch Elide and Maven/Gradle compete side-by-side!

### Option 2: Use a Custom Repository

1. Enter any public Java repository URL:
   - `https://github.com/google/gson`
   - `https://github.com/apache/commons-lang`
   - `https://github.com/spring-projects/spring-petclinic`
2. Click "Start Race"

### What to Expect

- **Live Race**: Two terminals appear showing real-time build output
- **Build Time**: Timer shows elapsed time for each runner
- **Winner**: Automatically detected when both complete
- **Replay**: Run the same repo again to see the cached replay

## Features

### 🏁 Side-by-Side Terminals
Watch both build tools execute in real-time with full terminal output.

### 📊 Win Rate Statistics
- Tracks wins/losses per repository
- Shows average build times
- Displays overall Elide win rate

### 📼 Automatic Recording & Replay
- First run: Live streaming
- Subsequent runs: Instant replay from recording
- All WebSocket messages saved to `backend/recordings/`

### 🎯 Suggested Repositories
18 popular Java projects categorized by size:
- **Small** (<1 min): Gson, Commons Lang, Joda Time
- **Medium** (1-5 min): Guava, OkHttp, JUnit 5
- **Large** (5-15 min): Spring Boot, Kafka, Elasticsearch

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  Frontend (React)               │
│              http://localhost:3000              │
│                                                 │
│  ┌──────────────┐      ┌──────────────┐       │
│  │  Elide       │      │  Maven/      │       │
│  │  Terminal    │      │  Gradle      │       │
│  │  (xterm.js)  │      │  Terminal    │       │
│  └──────┬───────┘      └──────┬───────┘       │
└─────────┼──────────────────────┼───────────────┘
          │                      │
      WebSocket               WebSocket
          │                      │
┌─────────┴──────────────────────┴───────────────┐
│              Backend (Express)                 │
│              http://localhost:3001             │
│                                                │
│  ┌──────────────┐      ┌──────────────┐      │
│  │  Race API    │      │  WebSocket   │      │
│  │  Endpoints   │      │  Servers     │      │
│  └──────────────┘      └──────────────┘      │
│                                               │
│  ┌──────────────┐      ┌──────────────┐      │
│  │  Database    │      │  Recorder    │      │
│  │  (SQLite)    │      │  Service     │      │
│  └──────────────┘      └──────────────┘      │
└───────────────────┬───────────┬───────────────┘
                    │           │
              ┌─────┴───────────┴─────┐
              │     Docker Engine     │
              │                       │
              │  ┌────────────────┐  │
              │  │ elide-builder  │  │
              │  │   container    │  │
              │  └────────────────┘  │
              │                      │
              │  ┌────────────────┐  │
              │  │standard-builder│  │
              │  │   container    │  │
              │  └────────────────┘  │
              └──────────────────────┘
```

## Project Structure

```
build-arena/
├── frontend-vite/          # React frontend
│   └── src/
│       └── pages/
│           └── RacePage.tsx       # Main race UI
│
├── backend/                # Node.js backend
│   ├── src/
│   │   ├── routes/
│   │   │   └── race-api.ts        # Race API endpoints
│   │   ├── websocket/
│   │   │   └── race-server.ts     # Live race streaming
│   │   └── services/
│   │       └── websocket-recorder.ts  # Recording service
│   └── recordings/         # Cached race recordings
│
├── docker/                 # Docker images
│   ├── elide-builder.Dockerfile
│   ├── standard-builder.Dockerfile
│   ├── CLAUDE-ELIDE.md    # Instructions for Elide agent
│   └── CLAUDE-STANDARD.md # Instructions for Maven/Gradle agent
│
└── INSTRUCTIONS.md         # This file
```

## Key Files

- **Race Page**: `frontend-vite/src/pages/RacePage.tsx` - Side-by-side terminals UI
- **Race API**: `backend/src/routes/race-api.ts` - Start races, check cache, get stats
- **WebSocket**: `backend/src/websocket/race-server.ts` - Live streaming & recording
- **Database**: `backend/src/db/schema.ts` - Jobs, results, statistics
- **Instructions**: `docker/CLAUDE-ELIDE.md` & `CLAUDE-STANDARD.md` - Agent prompts

## Common Commands

### Development

```bash
# Start both servers
pnpm dev

# Start backend only
pnpm --filter @elide/build-arena-backend dev

# Start frontend only
pnpm --filter @elide/build-arena-frontend dev

# Rebuild Docker images
cd docker && ./build-images.sh
```

### Database

```bash
# Push schema changes
pnpm --filter @elide/build-arena-backend db:push

# Open database GUI
pnpm --filter @elide/build-arena-backend db:studio

# Reset database
rm backend/build-arena.db
pnpm --filter @elide/build-arena-backend db:push
```

### Testing

```bash
# Test API endpoints
node test-race-api.js

# Run Playwright tests
pnpm test

# Check running containers
docker ps | grep race-

# View container logs
docker logs <container-id>

# Stop all race containers
docker ps -a | grep race- | awk '{print $1}' | xargs docker rm -f
```

## Troubleshooting

### Docker Images Not Found

```bash
cd docker
./build-images.sh
```

### Ports Already in Use

```bash
# Kill processes on ports 3000 and 3001
lsof -ti:3000 | xargs kill -9
lsof -ti:3001 | xargs kill -9

# Restart servers
pnpm dev
```

### Database Issues

```bash
# Reset database
rm backend/build-arena.db
pnpm --filter @elide/build-arena-backend db:push
```

### Environment Variables Not Loaded

Make sure `.env` file exists in `backend/` directory:

```bash
cat backend/.env
# Should show: ANTHROPIC_API_KEY=sk-...
```

### Containers Exit Immediately

Check container logs:

```bash
docker ps -a | grep race-
docker logs <container-id>
```

Common issues:
- Missing ANTHROPIC_API_KEY
- Docker image not built
- Repository URL invalid

## API Endpoints

### Check if Race Exists

```bash
curl "http://localhost:3001/api/races/check?repo=https://github.com/google/gson"
```

### Start a New Race

```bash
curl -X POST http://localhost:3001/api/races/start \
  -H "Content-Type: application/json" \
  -d '{"repositoryUrl":"https://github.com/google/gson"}'
```

### Get Statistics

```bash
curl "http://localhost:3001/api/races/stats"
```

## How It Works

### New Race Flow

1. **User submits repository URL**
2. **Backend checks cache** - Has this repo been raced before?
3. **If cached**: Load recording and replay to terminals
4. **If new**:
   - Create job in database
   - Start two Docker containers (Elide + Maven/Gradle)
   - Stream container logs via WebSocket
   - Record all messages for future replay
   - Detect winner based on build time
   - Save results and update statistics

### Replay Flow

1. **User submits previously-raced repository**
2. **Backend finds cached recording**
3. **Frontend replays messages** with original timing
4. **Shows previous winner** and statistics

### Recording Format

Recordings are stored as gzipped JSON:

```json
{
  "version": 1,
  "duration": 45230,
  "messages": [
    { "ts": 0, "msg": { "type": "output", "data": "Starting build..." } },
    { "ts": 1234, "msg": { "type": "output", "data": "Downloaded dependencies..." } },
    { "ts": 45230, "msg": { "type": "complete", "status": "success", "duration": 45 } }
  ],
  "metadata": {
    "jobId": "abc-123",
    "tool": "elide",
    "repositoryUrl": "https://github.com/google/gson",
    "claudeVersion": "2.0.35",
    "dockerImage": "elide-builder:latest"
  }
}
```

## Performance Tips

- **First build**: Takes longer (downloads dependencies)
- **Cached builds**: Much faster (uses Docker layer cache)
- **Small repos** (Gson): ~30-60 seconds
- **Large repos** (Spring Boot): 5-15 minutes

## Next Steps

### Try Different Repositories

Start with small repos to test quickly:
- ✅ **Google Gson** - JSON library (~30 sec)
- ✅ **Apache Commons Lang** - Utilities (~1 min)
- ✅ **Joda Time** - Date/time library (~1 min)

Then try larger projects:
- ⚡ **Google Guava** - Core libraries (~3 min)
- ⚡ **Spring Boot** - Full framework (~10 min)

### Explore Statistics

After running multiple races, check:
- Win rates per repository
- Average build times
- Overall Elide performance

### Modify Agent Instructions

Edit the Claude Code prompts:
- `docker/CLAUDE-ELIDE.md` - Elide agent instructions
- `docker/CLAUDE-STANDARD.md` - Maven/Gradle agent instructions

These files are mounted into containers and shown to Claude Code.

## Resources

- **Full Testing Guide**: `TERMINAL_TESTING.md`
- **Database Guide**: `DATABASE.md`
- **Architecture**: `ARCHITECTURE.md`
- **Popular Repos**: `POPULAR_JAVA_REPOS.md`
- **Agent Instructions**: `docker/CLAUDE.md`

## Support

If you run into issues:

1. Check this troubleshooting section
2. Review backend logs (`pnpm dev` output)
3. Check Docker container logs (`docker logs <id>`)
4. Verify environment variables (`cat backend/.env`)
5. Rebuild Docker images (`cd docker && ./build-images.sh`)

## Summary

You're now ready to:
- ✅ Run side-by-side races between Elide and Maven/Gradle
- ✅ Watch live terminal output from both build tools
- ✅ Replay previous races instantly
- ✅ Track win rates and statistics
- ✅ Test on 18+ popular Java repositories

**Enjoy watching the races!** 🏁
