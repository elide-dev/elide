# Race Reconnection System

The Build Arena now supports robust reconnection to races that are in progress, even after backend restarts.

## Problem

When the backend restarts (due to code changes or crashes):
1. In-memory minder processes are lost
2. Docker containers continue running
3. Database still shows race as "running"
4. Frontend can't display progress
5. No one is auto-approving Claude Code prompts

## Solution

### 1. Race Recovery Service

**File**: `backend/src/services/race-recovery.ts`

Detects and reconnects to "orphaned" races:
- Queries database for races marked as "running"
- Checks if containers are actually running via Docker
- Identifies orphaned races (containers running, no minders)
- Recreates minder processes for orphaned containers

### 2. Recovery Status API

**GET /api/status/recovery**
- Shows detected orphaned races
- Returns container IDs and job IDs
- Can be polled to detect issues

**POST /api/status/recovery/reconnect**
- Manually triggers race reconnection
- Recreates minders for all orphaned races
- Returns count of reconnected races

### 3. Status API Enhancements

**GET /api/status/minders**
- Shows all active minders
- Displays connection status, uptime, last activity
- Helps diagnose stuck races

**GET /api/status/docker**
- Lists all race containers
- Shows container status and uptime

## How to Use

### Manual Reconnection

```bash
# 1. Check for orphaned races
curl http://localhost:3001/api/status/recovery | jq .

# Output:
{
  "orphanedRacesDetected": 1,
  "races": [
    {
      "jobId": "904553ec-0c60-45f3-a682-1b719b6c324c",
      "repositoryUrl": "https://github.com/google/gson",
      "elideContainer": "4a9bdedda17e",
      "standardContainer": "56fcdaab8a0e"
    }
  ]
}

# 2. Trigger reconnection
curl -X POST http://localhost:3001/api/status/recovery/reconnect | jq .

# Output:
{
  "success": true,
  "reconnected": 1,
  "message": "Reconnected 1 orphaned race(s)"
}

# 3. Verify minders are active
curl http://localhost:3001/api/status/minders | jq '{total, connected}'

# Output:
{
  "total": 2,
  "connected": 2
}
```

### Frontend Reconnection

The frontend automatically handles reconnection:

1. **Race Page Load**: 
   - Fetches race status from `/api/races/:jobId`
   - Gets container IDs from database
   - Waits for `ready: true` before connecting WebSockets

2. **WebSocket Connection**:
   - Connects to `/ws/terminal/:containerId?interactive=false` (view-only)
   - Terminal server joins existing session if it exists
   - Receives all output from that point forward

3. **Timer Display**:
   - Currently shows duration from database (0 until completion)
   - TODO: Stream real-time duration from minders

## Diagnostic Workflow

When a race isn't working:

```bash
# Step 1: Check overall system health
curl http://localhost:3001/api/status | jq '{services}'

# Step 2: Check for orphaned races
curl http://localhost:3001/api/status/recovery | jq .

# Step 3: Check minder status
curl http://localhost:3001/api/status/minders | jq '.minders[] | {buildType, connected, lastActivity}'

# Step 4: Check containers
curl http://localhost:3001/api/status/docker | jq '.containers'

# Step 5: Trigger reconnection if needed
curl -X POST http://localhost:3001/api/status/recovery/reconnect
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│ Frontend (Browser)                                      │
│ - Fetches race status from API                         │
│ - Connects WebSocket in view-only mode                 │
│ - Displays terminal output and timers                  │
└──────────────────┬──────────────────────────────────────┘
                   │ HTTP + WebSocket
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Backend (Node.js)                                       │
│                                                          │
│ ┌──────────────────┐    ┌───────────────────────────┐ │
│ │ Race Recovery    │───▶│ Minder Registry (Map)     │ │
│ │ - findOrphaned() │    │ - Track active minders    │ │
│ │ - reconnect()    │    │ - getStatus()             │ │
│ └──────────────────┘    └───────────────────────────┘ │
│                                                          │
│ ┌──────────────────┐    ┌───────────────────────────┐ │
│ │ Status API       │───▶│ Database (SQLite)         │ │
│ │ /status/recovery │    │ - jobs table              │ │
│ │ /status/minders  │    │ - buildResults table      │ │
│ └──────────────────┘    └───────────────────────────┘ │
│                                                          │
│ ┌──────────────────────────────────────────────────┐   │
│ │ RaceMinder (per container)                       │   │
│ │ - Connects to terminal WebSocket (interactive)   │   │
│ │ - Auto-approves Claude Code prompts              │   │
│ │ - Detects completion signals                     │   │
│ └──────────────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────────────┘
                   │ WebSocket + Docker API
                   ▼
┌─────────────────────────────────────────────────────────┐
│ Docker Containers                                       │
│ ┌─────────────────┐              ┌──────────────────┐  │
│ │ Elide Runner    │              │ Standard Runner  │  │
│ │ - Claude Code   │              │ - Claude Code    │  │
│ │ - PTY session   │              │ - PTY session    │  │
│ └─────────────────┘              └──────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Key Features

### 1. Stateless Reconnection
- No need to track historical state
- Minders reconnect to existing PTY sessions
- Frontend joins existing WebSocket sessions
- Everything picks up from current point

### 2. Graceful Degradation
- If minders die, containers keep running
- Frontend can still view terminal output
- Can manually reconnect minders later
- No data loss (terminal history preserved)

### 3. Debug Visibility
- Status API shows exactly what's happening
- Can diagnose issues quickly
- Manual recovery when needed
- Helps understand system state

## Future Enhancements

1. **Automatic Recovery on Startup**
   - Backend automatically reconnects on startup
   - No manual intervention needed

2. **Persistent Minder State**
   - Save minder progress to database
   - Resume from last known state
   - Track approval history

3. **Frontend Recovery UI**
   - Show "Reconnecting..." indicator
   - Button to trigger manual recovery
   - Display minder status in UI

4. **Real-Time Duration Streaming**
   - Minders broadcast current duration
   - Frontend updates timer in real-time
   - More accurate progress tracking

## Testing

Test the reconnection flow:

```bash
# 1. Start a race
curl -X POST http://localhost:3001/api/races/start \
  -H 'Content-Type: application/json' \
  -d '{"repositoryUrl": "https://github.com/google/gson"}' | jq .jobId

# 2. Kill backend (simulate crash)
pkill -f "pnpm.*dev"

# 3. Restart backend
pnpm dev

# 4. Check for orphaned races
curl http://localhost:3001/api/status/recovery | jq .

# 5. Reconnect
curl -X POST http://localhost:3001/api/status/recovery/reconnect | jq .

# 6. Verify frontend works
# Open http://localhost:3000/race/:jobId
```

## Minder Replay Mode (TODO)

When a minder reconnects to an existing PTY session, it needs to:

### 1. Process Historical Output (Replay Mode)
- Request terminal history from WebSocket recorder
- Process all historical output to determine current state
- **DO NOT** send any input during replay
- Detect:
  - Has Claude Code started?
  - Which prompts have been approved?
  - Is it currently thinking/working?
  - Has the build completed?

### 2. Transition to Live Mode
- Once replay is complete, switch to live processing
- Begin sending input for new prompts
- Continue normal minder behavior

### Implementation

```typescript
class RaceMinder {
  private mode: 'replay' | 'live' = 'replay';  // Start in replay mode
  
  private handleMessage(data: Buffer) {
    const message = JSON.parse(data.toString());
    
    if (message.type === 'replay_start') {
      console.log('[Minder] Starting replay of terminal history');
      this.mode = 'replay';
    }
    
    if (message.type === 'replay_end') {
      console.log('[Minder] Replay complete, switching to live mode');
      this.mode = 'live';
      
      // Now that we've caught up, decide what to do next
      if (!this.claudeStarted) {
        // Claude hasn't started yet, send the initial command
        this.sendClaudeCommand();
      }
    }
    
    if (message.type === 'output') {
      // Process output to update state
      this.processOutput(message.data);
      
      // Only send input in live mode
      if (this.mode === 'live') {
        this.handlePromptApproval(message.data);
      }
    }
  }
}
```

### WebSocket Protocol Enhancement

The terminal WebSocket server should support replay:

```typescript
// When minder connects with reconnect=true
ws://host:port/ws/terminal/:containerId?record=true&reconnect=true

// Server response:
{type: 'replay_start'}
{type: 'output', data: '...'} // Historical output
{type: 'output', data: '...'} // Historical output
...
{type: 'replay_end', duration: 45000} // milliseconds since start
{type: 'output', data: '...'} // Live output from here on
```

### Benefits

1. **State Recovery**: Minder knows exactly where things are
2. **No Duplicate Work**: Won't re-send commands that already executed
3. **Accurate Progress**: Can resume from current point
4. **Idempotent Reconnection**: Safe to reconnect multiple times

### Example Scenario

```
Container starts at T=0
Minder1 connects at T=0
  - Sends: claude "clone repo..."
  - Approves: theme selection
  - Approves: API key
  - Approves: workspace trust
  
Backend crashes at T=30s

Minder2 reconnects at T=35s
  - Receives REPLAY: all output from T=0 to T=35
  - Detects: claudeStarted=true, apiKeyHandled=true, workspaceTrustHandled=true
  - Detects: Currently cloning repository
  - Switches to LIVE mode
  - Continues approving new prompts as they appear
```

### Status Tracking

The Status API should show replay status:

```bash
curl http://localhost:3001/api/status/minders | jq '.minders[] | {buildType, mode, replayComplete}'

# Output:
{
  "buildType": "elide",
  "mode": "replay",
  "replayComplete": false
}
```
