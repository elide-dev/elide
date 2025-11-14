# Terminal Test Page - Testing Guide

This guide helps Claude Code and other AI agents test the terminal functionality using headless browsers.

## Quick Start

```bash
# Run all terminal tests in headless mode
./test-terminal.sh headless

# Run tests with visible browser windows
./test-terminal.sh headed

# Open interactive browser for manual testing
./test-terminal.sh interactive

# Run tests in debug mode with Playwright inspector
./test-terminal.sh debug
```

## Prerequisites

The test script automatically checks for:
- ✅ Docker installed
- ✅ pnpm installed
- ✅ `elide-builder` Docker image built
- ✅ Backend running on port 3001
- ✅ Frontend running on port 3000

If services aren't running, the script will start them automatically.

## Test Coverage

The terminal tests cover:

### Basic Functionality
- ✅ Page loads correctly
- ✅ Control buttons are visible (Start/Stop/Clear)
- ✅ Status indicators show correct state
- ✅ Test instructions are displayed

### Container Management
- ✅ Start Docker container
- ✅ WebSocket connection established
- ✅ Container ID displayed
- ✅ Stop container and cleanup
- ✅ Status changes reflected

### Terminal Interaction
- ✅ Execute basic commands (`echo`, `ls`, `pwd`)
- ✅ Check Java installation (`java -version`)
- ✅ Check Elide installation (`elide --version`)
- ✅ Clear terminal output
- ✅ Terminal input/output works bidirectionally

### Error Handling
- ✅ Graceful handling of container start failures
- ✅ WebSocket disconnect handling
- ✅ Error messages displayed in terminal

### Navigation
- ✅ Link from homepage to terminal test page
- ✅ Routing works correctly

## Manual Testing Commands

If you want to test manually without the script:

### 1. Start Services
```bash
# Start both frontend and backend
pnpm dev

# Or start separately
pnpm --filter @elide/build-arena-backend dev &
pnpm --filter @elide/build-arena-frontend dev &
```

### 2. Build Docker Images (if needed)
```bash
cd docker && ./build-images.sh && cd ..
```

### 3. Run Playwright Tests
```bash
# Headless mode
pnpm test tests/terminal-test.spec.ts

# Headed mode (see browsers)
pnpm test:headed tests/terminal-test.spec.ts

# Debug mode with inspector
pnpm exec playwright test tests/terminal-test.spec.ts --debug

# Single test
pnpm exec playwright test tests/terminal-test.spec.ts -g "should execute commands"
```

## Test Scenarios

### Scenario 1: Basic Terminal Connection

```typescript
// Test automatically:
// 1. Navigates to http://localhost:3000/test/terminal
// 2. Clicks "Start Container"
// 3. Waits for WebSocket connection
// 4. Verifies "Connected" status
// 5. Checks for container ID
```

### Scenario 2: Command Execution

```typescript
// Test automatically:
// 1. Starts container and connects
// 2. Types: echo "Hello from terminal test"
// 3. Waits for output
// 4. Verifies output contains expected text
```

### Scenario 3: Java Environment Check

```typescript
// Test automatically:
// 1. Starts container
// 2. Executes: java -version
// 3. Waits for output
// 4. Verifies output contains "openjdk" or "java version"
```

### Scenario 4: Cleanup

```typescript
// Test automatically:
// 1. Starts container
// 2. Clicks "Stop Container"
// 3. Verifies "Disconnected" status
// 4. Checks status shows "Stopped"
```

## Debugging Tips

### Check Backend Logs
```bash
tail -f backend.log
```

### Check Frontend Console
```bash
# In headed mode, open browser DevTools
# Or check Playwright trace:
pnpm exec playwright show-trace test-results/.../trace.zip
```

### Check Docker Containers
```bash
# List all containers (including stopped)
docker ps -a | grep elide-builder

# Check container logs
docker logs <container-id>

# Cleanup test containers manually
docker ps -a | grep elide-builder | awk '{print $1}' | xargs docker rm -f
```

### WebSocket Debugging
```bash
# Test WebSocket connection manually
wscat -c ws://localhost:3001/ws/terminal/<container-id>

# Or use curl to start a container
curl -X POST http://localhost:3001/api/test/start-container
```

## Claude Code Usage

If you're Claude Code testing this locally:

### Quick Test
```bash
# Just run this command:
./test-terminal.sh headless
```

### Step-by-Step Test
```bash
# 1. Check prerequisites
docker --version
pnpm --version

# 2. Start services
pnpm dev

# 3. Verify services
curl http://localhost:3001/health
curl http://localhost:3000

# 4. Run tests
pnpm test tests/terminal-test.spec.ts

# 5. Check results
echo "Tests passed!" && echo "View report: pnpm exec playwright show-report"
```

### Interactive Test (Manual Verification)
```bash
# 1. Start services
pnpm dev

# 2. Open terminal test page in browser
open http://localhost:3000/test/terminal
# Or manually visit the URL

# 3. Click "Start Container"
# 4. Type commands: ls, pwd, java -version, elide --version
# 5. Click "Stop Container"
```

## Test Output Examples

### Successful Test Run
```
✓ should load terminal test page (234ms)
✓ should have control buttons (123ms)
✓ should show disconnected status initially (89ms)
✓ should start container and connect terminal (5432ms)
✓ should execute commands in terminal (3210ms)
✓ should show Java version (4567ms)
✓ should stop container and disconnect (2345ms)
✓ should clear terminal (1890ms)

8 passed (23.4s)
```

### Failed Test Example
```
✗ should start container and connect terminal
  Error: Timeout 30000ms exceeded while waiting for selector "text=Connected"

  This usually means:
  - Backend is not running
  - Docker is not running
  - elide-builder image doesn't exist
  - WebSocket connection failed
```

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Terminal Tests
  run: |
    cd tools/build-arena
    ./test-terminal.sh headless
```

### GitLab CI Example
```yaml
test-terminal:
  script:
    - cd tools/build-arena
    - ./test-terminal.sh headless
  artifacts:
    when: always
    paths:
      - tools/build-arena/test-results/
      - tools/build-arena/playwright-report/
```

## Troubleshooting

### Problem: Tests timeout waiting for connection

**Solution:**
1. Check if backend is running: `curl http://localhost:3001/health`
2. Check if Docker is running: `docker ps`
3. Check if image exists: `docker images | grep elide-builder`
4. Check backend logs: `tail -f backend.log`

### Problem: Container starts but terminal doesn't connect

**Solution:**
1. Check WebSocket endpoint: `wscat -c ws://localhost:3001/ws/terminal/test`
2. Check browser console for WebSocket errors
3. Verify backend WebSocket server is set up correctly
4. Check CORS settings

### Problem: Commands don't execute in terminal

**Solution:**
1. Verify terminal has focus (Playwright clicks on it)
2. Check if stdin is properly connected to container
3. Verify exec instance is created correctly
4. Check Docker container logs: `docker logs <container-id>`

### Problem: Docker image not found

**Solution:**
```bash
cd docker
./build-images.sh
```

## Performance Notes

- Container startup: ~5-10 seconds
- WebSocket connection: ~1-2 seconds
- Command execution: ~100-500ms per command
- Total test suite: ~20-30 seconds

## Advanced Usage

### Run Specific Test
```bash
pnpm exec playwright test tests/terminal-test.spec.ts -g "should execute commands"
```

### Run with specific browser
```bash
pnpm exec playwright test tests/terminal-test.spec.ts --project=chromium
pnpm exec playwright test tests/terminal-test.spec.ts --project=firefox
pnpm exec playwright test tests/terminal-test.spec.ts --project=webkit
```

### Generate Test Report
```bash
pnpm test tests/terminal-test.spec.ts
pnpm exec playwright show-report
```

### Record Test Video
```bash
# Videos are automatically recorded on test failure
# Find them in: test-results/*/video.webm
```

### Trace Viewer
```bash
# Traces are recorded on test failure
pnpm exec playwright show-trace test-results/*/trace.zip
```

## API Endpoints for Testing

### Start Container
```bash
curl -X POST http://localhost:3001/api/test/start-container \
  -H "Content-Type: application/json" \
  -d '{"image":"elide-builder:latest"}'
```

### Stop Container
```bash
curl -X POST http://localhost:3001/api/test/stop-container/<container-id>
```

### List Test Containers
```bash
curl http://localhost:3001/api/test/containers
```

### Cleanup All Test Containers
```bash
curl -X POST http://localhost:3001/api/test/cleanup
```

## Next Steps

After terminal testing is complete, you can:
1. Test the full Build Arena flow with AI agents
2. Test WebSocket multiplexing with multiple terminals
3. Test concurrent builds in different containers
4. Load test with multiple simultaneous connections

## Resources

- [Playwright Documentation](https://playwright.dev)
- [xterm.js Documentation](https://xtermjs.org)
- [Docker SDK Documentation](https://github.com/apocas/dockerode)
- [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
