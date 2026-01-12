# Build Arena Observability Guide

This guide documents the closed-loop observability infrastructure for monitoring and debugging build races.

## Overview

The Build Arena includes several tools for real-time monitoring and debugging of race containers:

1. **Terminal Output Dumper** - Capture complete WebSocket streams from containers
2. **Backend Log Filtering** - Monitor minder detection events and approvals
3. **Container Status Checks** - Verify container health and state
4. **Minder Status API** - Inspect minder internal state

## Quick Start

### 1. Start a Race

```bash
cd /Users/rwalters/GitHub/elide/tools/build-arena
curl -X POST http://localhost:3001/api/races/start \
  -H 'Content-Type: application/json' \
  -d '{"repositoryUrl": "https://github.com/google/gson"}'
```

Save the `jobId` and container IDs from the response.

### 2. Monitor with Terminal Dumper

The terminal dumper connects to a container's WebSocket in read-only mode and captures all output:

```bash
cd backend
pnpm exec tsx ../scripts/dump-terminal-output.ts <containerId>
```

**Example:**
```bash
pnpm exec tsx ../scripts/dump-terminal-output.ts bbfeee6988ff
```

**Output:**
```
Connecting to ws://localhost:3001/ws/terminal/bbfeee6988ff?interactive=false...
Connected! Listening for output...

================================================================================
=== Elide Build Arena Container ===
Claude Code: 2.0.30 (Claude Code)
Java: openjdk version "17.0.17" 2025-10-21
Working Directory: /workspace
Instructions: cat /workspace/CLAUDE.md
================================================================================

Timeout after 10 seconds. Received 4 messages.
Total output length: 412 characters
```

**What it tells you:**
- Current terminal state (bash prompt, Claude running, etc.)
- Message count and total output size
- Real-time snapshot of container activity

### 3. Filter Backend Logs

Use `BashOutput` filtering to monitor specific events from the running backend:

```bash
# From Claude Code, filter the backend process logs
```

**Common filters:**

#### Monitor All Minder Activity
```regex
Minder:
```
Shows: Starting, connecting, sending commands, approvals

#### Monitor Approvals Only
```regex
Auto-approving:|Bell rung|approved
```
Shows: API key, workspace trust, Bash commands, completion signals

#### Monitor Errors
```regex
Error|error|failed|Failed|API Error
```
Shows: WebSocket errors, API errors, permission failures

#### Monitor Specific Commands
```regex
git clone|maven|gradle|elide
```
Shows: Build tool command executions

**Example output:**
```
[Minder:elide] Starting for container bbfeee6988ff
[Minder:elide] Connected to WebSocket
[Minder:elide] Sending Claude command
[Minder:elide] ✅ API KEY PROMPT DETECTED! Auto-approving...
[Minder:elide] API key approved - auth successful
[Minder:elide] Workspace trust prompt detected - auto-approving
[Minder:elide] Workspace trust approved
[Minder:elide] Claude Code started
[Minder:elide] Auto-approving: git clone https://github.com/google/gson
[Minder:elide] Bell rung! Pattern: /🔔/
```

### 4. Check Container Status

Verify containers are running:

```bash
docker ps --filter "name=race-" --format "table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.RunningFor}}"
```

**Example output:**
```
CONTAINER ID   NAMES                                                STATUS         CREATED
ff6496c07c68   race-e82b1bf0-e623-41b4-8957-87c239207e3c-standard   Up 7 minutes   7 minutes ago
bbfeee6988ff   race-e82b1bf0-e623-41b4-8957-87c239207e3c-elide      Up 7 minutes   7 minutes ago
```

### 5. Check Minder Status (Future)

Query minder internal state via API endpoint:

```bash
curl http://localhost:3001/api/minders/status/<containerId> | jq
```

**Example response:**
```json
{
  "containerId": "bbfeee6988ff",
  "buildType": "elide",
  "repoUrl": "https://github.com/google/gson",
  "connected": true,
  "uptime": 120,
  "lastActivity": "Approving: git clone",
  "approvalCount": 3,
  "state": {
    "themeHandled": true,
    "apiKeyHandled": true,
    "trustHandled": true,
    "workspaceTrustHandled": true,
    "claudeStarted": true,
    "bellRung": false
  },
  "lastOutputSnippet": "Cloning into 'gson'..."
}
```

## Debugging Workflows

### Workflow 1: Race Stuck at Workspace Trust

**Symptom:** Containers started but no progress

**Steps:**
1. Run terminal dumper to see current state:
   ```bash
   pnpm exec tsx ../scripts/dump-terminal-output.ts <containerId>
   ```

2. Look for workspace trust prompt text in output

3. Filter backend logs for workspace trust:
   ```regex
   Workspace trust|Ready to code|Do you trust
   ```

4. If detected but not approved, check race-minder.ts:287-289 for prompt patterns

**Expected output when working:**
```
[Minder:elide] Workspace trust prompt detected - auto-approving
[Minder:elide] Workspace trust approved
```

### Workflow 2: Claude Not Starting

**Symptom:** Minder logs show "Claude Code started" but container is at bash prompt

**Steps:**
1. Dump terminal to verify state:
   ```bash
   pnpm exec tsx ../scripts/dump-terminal-output.ts <containerId>
   ```

2. Filter logs for errors:
   ```regex
   Error|error|API Error
   ```

3. Check if Claude exited due to:
   - API timeout (thinking too long)
   - API error (500/429/etc)
   - No valid response

**Fix:** Look for API error retry logic in race-minder.ts:184-198

### Workflow 3: Commands Not Being Approved

**Symptom:** Claude requests permission but minder doesn't approve

**Steps:**
1. Dump terminal to see the prompt:
   ```bash
   pnpm exec tsx ../scripts/dump-terminal-output.ts <containerId>
   ```

2. Look for the exact prompt text (copy it)

3. Check race-minder.ts:327-334 for detection patterns:
   ```typescript
   const hasPromptQuestion = output.includes('Do you want to proceed?') ||
                             output.includes('proceed?') ||
                             output.includes('Enter to confirm');

   const hasOptionChoices = /[❯\s]*1\.\s*(Yes|Allow|Approve)/i.test(output);
   ```

4. Add missing pattern if needed

5. Verify 2-second debounce isn't blocking (line 338)

**Expected output when working:**
```
[Minder:elide] Auto-approving: git clone https://github.com/google/gson
```

### Workflow 4: Bell Not Detected

**Symptom:** Build completes but race doesn't finish

**Steps:**
1. Dump terminal output to see completion message

2. Check completion patterns in race-minder.ts:388-402:
   ```typescript
   const completionPatterns = [
     /🔔/,
     /BUILD COMPLETE/i,
     /Build succeeded/i,
     /Total time:/i,
     // etc...
   ];
   ```

3. Add missing pattern if build tool uses different format

**Expected output when working:**
```
[Minder:elide] Bell rung! Pattern: /🔔/
```

## Testing Individual Components

### Test 1: WebSocket Terminal Dumper

**Purpose:** Verify real-time terminal output capture

```bash
# Start a test container manually
docker run -it --rm --name test-dump elide-builder:latest bash

# In another terminal, get container ID
docker ps --filter "name=test-dump" --format "{{.ID}}"

# Dump output
cd backend
pnpm exec tsx ../scripts/dump-terminal-output.ts <containerId>
```

**Expected:** Should show bash prompt and any commands you type

### Test 2: Minder Detection Logic

**Purpose:** Verify prompt detection without full race

```bash
# Read the test cases in race-minder.ts comments
grep -A 5 "Check if this is the workspace trust" backend/src/services/race-minder.ts
```

**Patterns to verify:**
- `"Do you trust the files"` (old Claude Code)
- `"Ready to code here?"` (Claude Code 2.0.30 standard)
- `"Is this a project you created or one you trust"` (Claude Code 2.0.30 elide)
- `"Quick safety check"` (fallback)

### Test 3: Backend Log Filtering

**Purpose:** Verify log output is reaching backend

```bash
# Tail backend logs with basic filter
# Look for "Container ... output" messages
```

**Expected:** Should see every terminal output message logged

## Common Issues

### Issue 1: Dump Script Shows Stale Output

**Cause:** WebSocket replay buffer only shows buffered messages

**Solution:** The dump script captures a 10-second snapshot. For historical output, check the WebSocket recorder buffer (future feature).

### Issue 2: Minder Logs Not Appearing

**Cause:** Backend not running or crashed

**Solution:**
```bash
lsof -ti:3001  # Check if backend is running
pnpm dev       # Restart if needed
```

### Issue 3: Container Exited Unexpectedly

**Cause:** Docker resource limits or OOM

**Solution:**
```bash
docker logs <containerId>  # Check container logs
docker inspect <containerId> | jq '.[0].State'  # Check exit code
```

## File Reference

### Key Files

- **`scripts/dump-terminal-output.ts`** - WebSocket terminal dumper
- **`backend/src/services/race-minder.ts`** - Auto-approval logic
  - Lines 284-289: Workspace trust detection
  - Lines 327-381: Generic permission detection
  - Lines 388-402: Completion signal detection
- **`backend/src/services/websocket-recorder.ts`** - Message recording
- **`backend/src/websocket/terminal-server.ts`** - WebSocket server

### Configuration

- **Timeout:** Race timeout is 10 minutes (race-minder.ts:139)
- **Debounce:** Permission approval debounce is 2 seconds (race-minder.ts:338)
- **Dump duration:** Terminal dumper runs for 10 seconds (dump-terminal-output.ts:65)

## Advanced Debugging

### Inspect WebSocket Messages

To see raw WebSocket messages, modify terminal-server.ts to log all messages:

```typescript
// Add after line 150 in terminal-server.ts
console.log(`[WS Debug] ${interactive ? 'Interactive' : 'View-only'} message:`,
            JSON.stringify(message).substring(0, 200));
```

### Monitor Network Traffic

Use `tcpdump` to capture WebSocket traffic:

```bash
sudo tcpdump -i lo0 -A 'tcp port 3001' | grep -A 5 "ws/terminal"
```

### Analyze Recorder Buffer

The WebSocket recorder stores all messages. To dump the buffer:

```typescript
// In backend console or new endpoint
import { getRecorder } from './services/websocket-recorder';
const recorder = getRecorder(containerId);
console.log(recorder.getMessages());
```

## Next Steps

### Planned Enhancements

1. **Minder Status API** - Real-time minder state inspection
2. **Recorder Replay API** - Full WebSocket message history
3. **Auto-restart on Exit** - Restart Claude Code if it exits prematurely
4. **Approval Statistics** - Track approval counts and timings
5. **Race Dashboard** - Real-time UI for monitoring races

### Contributing

When adding new detection patterns or observability features:

1. Update race-minder.ts with clear comments
2. Add test cases in this document
3. Update CLAUDE.md instructions if prompt text changes
4. Test with both elide-builder and standard-builder images
