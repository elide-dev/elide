# Interactive AI Agents - Build Arena

Build Arena uses **Claude Code AI agents** in each sandbox to autonomously build, test, and validate Java projects in a head-to-head competition.

## Overview

Instead of pre-scripted builds, Build Arena runs **fully autonomous AI agents** with:

- ✓ **Full terminal access** - Interactive PTY sessions
- ✓ **Internet connectivity** - Can search docs, Stack Overflow, GitHub issues
- ✓ **Package management** - Can install any tools they need (apt-get, pip, npm, etc.)
- ✓ **Creative freedom** - Can optimize, parallelize, cache, or use any technique
- ✓ **Self-validation** - Agents decide how to verify build success

## How It Works

### 1. User Submits Repository

User provides a public Java repository URL (GitHub, GitLab, etc.) via the web interface.

### 2. Sandboxes Spin Up

Two Docker containers are created in parallel:

- **Elide Container**: Ubuntu + Java + Elide + Claude Code
- **Standard Container**: Ubuntu + Java + Maven + Gradle + Claude Code

Each container gets:
- 4GB RAM
- 2 CPU cores
- Full internet access
- Interactive terminal (PTY)

### 3. Claude Code Agents Start

Each container launches a Claude Code agent with a `CLAUDE.md` instruction file:

**Elide Agent** (`docker/CLAUDE-elide.md`):
```markdown
# Build Arena Challenge: Elide Team

You have FULL AUTONOMY to:
- Read documentation
- Search the internet
- Install programs
- Optimize configurations
- Use any trick in the book

**Goal**: Build with Elide, validate, then ring the bell (\a)
```

**Standard Agent** (`docker/CLAUDE-standard.md`):
```markdown
# Build Arena Challenge: Standard Toolchain Team

You have FULL AUTONOMY to:
- Read documentation
- Search the internet
- Install programs
- Optimize Gradle/Maven configs
- Use any trick in the book

**Goal**: Build with Maven/Gradle, validate, then ring the bell (\a)
```

### 4. Agents Work Autonomously

The agents can do **anything** they want:

**Research & Discovery:**
- Search Google/Stack Overflow for build errors
- Read project documentation (README, CONTRIBUTING, etc.)
- Check GitHub Issues for known problems
- Look up Gradle/Maven plugin documentation

**Installation & Setup:**
- `apt-get install` missing dependencies
- Install specific Java versions if needed
- Download and configure build tools
- Set up environment variables

**Optimization:**
- Enable Gradle build cache
- Configure Maven local repository
- Run builds in parallel
- Skip unnecessary tasks
- Tune JVM settings

**Validation:**
- Run unit tests
- Run integration tests
- Check compiled artifacts exist
- Execute JAR files to verify they work
- Validate checksums
- Whatever they deem necessary!

### 5. Bell Ringing

When an agent is confident the build succeeded, they ring the terminal bell:

```bash
echo -e "\a"  # ASCII bell character
```

This triggers:
- 🔔 Visual notification in the frontend
- WebSocket event broadcast
- Timestamp recorded for metrics
- Counter increment in UI

### 6. Spectators Watch Live

Users see both terminals side-by-side with:

- **Real-time output** - Every command, every log line
- **Bell notifications** - Visual indicators when milestones hit
- **Performance metrics** - Build time, memory, CPU usage
- **Interactive terminals** - Can view agent's progress live

## Architecture

```
┌─────────────────────────────────────────┐
│          User Browser                   │
│  ┌─────────────┐   ┌─────────────┐    │
│  │ Elide Term  │   │Standard Term│    │
│  │  (xterm.js) │   │  (xterm.js) │    │
│  └─────┬───────┘   └───────┬─────┘    │
│        │WebSocket          │           │
└────────┼───────────────────┼───────────┘
         │                   │
         ▼                   ▼
┌─────────────────────────────────────────┐
│        Backend (Node.js)                │
│   InteractiveSandboxRunner              │
│        │               │                 │
│        ▼               ▼                 │
│  ┌──────────┐   ┌──────────┐           │
│  │Docker PTY│   │Docker PTY│           │
│  └────┬─────┘   └─────┬────┘           │
└───────┼───────────────┼─────────────────┘
        │               │
        ▼               ▼
  ┌──────────────┐ ┌──────────────┐
  │Elide Builder │ │Standard      │
  │Container     │ │Builder       │
  │              │ │Container     │
  │┌───────────┐ │ │┌───────────┐│
  ││Claude Code│ │ ││Claude Code││
  ││Agent      │ │ ││Agent      ││
  │└───────────┘ │ │└───────────┘│
  │              │ │              │
  │• Elide      │ │• Maven      ││
  │• Java 17    │ │• Gradle     ││
  │• Git        │ │• Java 17    ││
  │• Internet   │ │• Git        ││
  │• apt-get    │ │• Internet   ││
  └──────────────┘ └──────────────┘
```

## Code Flow

### Backend: Starting Interactive Session

```typescript
// backend/src/services/interactive-sandbox-runner.ts
async runInteractiveBuild(jobId, repositoryUrl, tool) {
  // 1. Create Docker container with PTY
  const container = await this.createInteractiveContainer(...)

  // 2. Start container
  await container.start()

  // 3. Launch Claude Code agent with CLAUDE.md instructions
  await this.startClaudeCodeAgent(container, ...)

  // 4. Stream output to WebSocket clients
  stream.on('data', (chunk) => {
    this.emit('terminal:output', { jobId, tool, data: chunk })

    // Detect bell character
    if (data.includes('\u0007')) {
      this.emit('build:bell', { jobId, tool, timestamp })
    }
  })

  // 5. Wait for completion
  await this.waitForCompletion(container)
}
```

### Frontend: Terminal Component

```typescript
// frontend/src/components/Terminal.tsx
export function Terminal({ jobId, tool }) {
  // Initialize xterm.js terminal
  const term = new XTerm({ ... })

  // Handle keyboard input
  term.onData((data) => {
    ws.send(JSON.stringify({
      type: 'terminal_input',
      payload: { jobId, tool, data }
    }))
  })

  // Handle WebSocket messages
  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data)

    if (msg.type === 'terminal_output') {
      term.write(msg.payload.data)
    }
    else if (msg.type === 'build_bell') {
      setBellCount(prev => prev + 1)
      term.writeln('🔔 BELL RUNG!')
    }
  }
}
```

## Agent Instructions

### What Agents Get

**Environment Variables:**
```bash
REPO_URL=https://github.com/user/project.git
BUILD_TOOL=elide|standard
ANTHROPIC_API_KEY=sk-ant-...
TERM=xterm-256color
```

**Instruction File** (`/workspace/CLAUDE.md`):
- Mission description
- Full autonomy permissions
- Suggested workflow (optional)
- Bell ringing instructions
- Success criteria

**Pre-installed Tools:**
- `curl`, `wget` - Download files
- `git` - Clone repositories
- `apt-get` - Install packages
- `python3`, `pip` - Scripting
- `jq` - JSON processing
- `vim`, `nano` - Text editing
- Java 17 JDK
- Node.js + npm
- Claude Code CLI

### Agent Autonomy Examples

**Example 1: Handling Missing Dependencies**

```bash
# Agent discovers missing dependency
./gradlew build
# Error: Could not find protobuf compiler

# Agent searches the internet
curl -s "https://www.google.com/search?q=gradle+protobuf+missing"

# Agent installs it
apt-get update && apt-get install -y protobuf-compiler

# Agent retries
./gradlew build  # Success!
echo -e "\a"  # Ring the bell
```

**Example 2: Optimization**

```bash
# Standard agent optimizes Gradle
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << EOF
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.workers.max=4
EOF

# Run optimized build
./gradlew build --build-cache --parallel
```

**Example 3: Creative Validation**

```bash
# Elide agent validates by running tests
elide gradle build

# Then validates JAR is executable
java -jar build/libs/*.jar --version

# If successful, ring bell
if [ $? -eq 0 ]; then
  echo "✓ Binary validated and executable!"
  echo -e "\a\a"  # Double bell!
fi
```

## Bell System

The "bell" is the universal signal that a build milestone is reached.

### Ringing the Bell

**Bash:**
```bash
echo -e "\a"
printf '\a'
```

**Python:**
```python
print('\a')
```

**Java:**
```java
System.out.print('\u0007');
```

### Bell Detection

Backend detects ASCII bell character (`\u0007` or `\a`) in terminal output:

```typescript
if (data.includes('\u0007') || data.includes('\a')) {
  this.emit('build:bell', { jobId, tool, timestamp })
}
```

### Visual Notification

Frontend shows:
- Animated badge in terminal corner: `🔔 x2`
- Console message: `🔔 BELL RUNG! Build milestone reached!`
- Optional callback for parent component

## Security & Sandboxing

### Container Isolation

Each sandbox is isolated with:
- **Resource limits**: 4GB RAM, 2 CPU cores
- **Network isolation**: Bridge network (can access internet, not host)
- **Filesystem isolation**: No host filesystem access
- **Temporary**: Containers destroyed after build

### Safety Measures

- Containers run as non-root user (future enhancement)
- No access to host Docker socket
- Time limits enforced (10-minute max)
- Can be stopped/killed at any time
- Output sanitized for display

### API Key Protection

Claude API keys are:
- Stored as environment variables
- Never exposed to frontend
- Passed to containers at runtime only
- Containers destroyed after use (no persistence)

## Configuration

### Environment Variables

**Required:**
```bash
ANTHROPIC_API_KEY=sk-ant-...  # Your Claude API key
```

**Optional:**
```bash
PORT=3001                      # Backend port
DOCKER_HOST=unix:///var/run/docker.sock
```

### Resource Limits

Edit `backend/src/services/interactive-sandbox-runner.ts`:

```typescript
HostConfig: {
  Memory: 4 * 1024 * 1024 * 1024,  // 4GB
  MemorySwap: 4 * 1024 * 1024 * 1024,
  CpuQuota: 200000,  // 2 cores (100000 = 1 core)
  NetworkMode: 'bridge',
}
```

### Timeout

Edit `waitForCompletion()` for custom timeout:

```typescript
const timeout = setTimeout(() => {
  container.stop()
  resolve(-1)
}, 10 * 60 * 1000)  // 10 minutes
```

## Customizing Agent Instructions

Edit the `CLAUDE-*.md` files to change agent behavior:

**Make agents more aggressive:**
```markdown
## Performance is EVERYTHING
- Skip non-critical tests
- Download dependencies in parallel
- Use every CPU core available
```

**Make agents more careful:**
```markdown
## Validation is Critical
- Must run ALL tests
- Must check code coverage
- Must validate binary signatures
```

**Give specific hints:**
```markdown
## Known Issues
- This project requires protobuf-compiler
- Gradle wrapper might be broken, use system Gradle
```

## Metrics & Analytics

Build Arena tracks:

**Performance:**
- Total build time (ms)
- Peak memory usage (bytes)
- Average CPU usage (%)
- First bell time (how fast to declare success)

**Behavior:**
- Number of bells rung
- Commands executed (future)
- Packages installed (future)
- Internet requests made (future)

## Future Enhancements

Potential improvements:

- **Recording**: Save full terminal sessions for replay
- **Replay**: Watch builds from the past
- **Leaderboard**: Top performing repositories
- **Agent analytics**: What strategies work best
- **Collaborative mode**: Multiple agents per team
- **Tournament mode**: Multiple repos compete
- **Live commentary**: AI commentator narrates the competition

## Troubleshooting

**Agents not starting:**
- Check ANTHROPIC_API_KEY is set
- Verify Claude Code is installed in Docker image
- Check Docker logs: `docker logs <container-id>`

**No output in terminal:**
- Check WebSocket connection in browser console
- Verify container is running: `docker ps`
- Check backend logs for errors

**Bell not detected:**
- Verify agent is using correct escape sequence
- Check backend logs for bell events
- Try manual test: `docker exec <container> echo -e "\a"`

**Build timeout:**
- Increase timeout in `interactive-sandbox-runner.ts`
- Check container resources aren't exhausted
- Verify project can build normally

## License

MIT
