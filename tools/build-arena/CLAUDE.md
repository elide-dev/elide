# Claude Code Instructions - Build Arena

## Quick Start for Testing

If you're Claude Code and want to test the Build Arena terminal functionality locally:

### One-Command Test
```bash
cd /Users/rwalters/GitHub/elide/tools/build-arena
./test-terminal.sh headless
```

This will:
1. ✅ Check Docker & pnpm are installed
2. ✅ Build Docker images if needed
3. ✅ Start frontend & backend if not running
4. ✅ Run comprehensive terminal tests
5. ✅ Report results

### Manual Testing Steps

If you want to test manually step-by-step:

```bash
# 1. Navigate to project
cd /Users/rwalters/GitHub/elide/tools/build-arena

# 2. Check prerequisites
docker --version  # Should show Docker version
pnpm --version    # Should show pnpm version

# 3. Start services (if not already running)
pnpm dev &
sleep 10  # Wait for services to start

# 4. Verify services are up
curl http://localhost:3001/health  # Should return {"status":"ok"}
curl http://localhost:3000         # Should return HTML

# 5. Run terminal tests
pnpm test tests/terminal-test.spec.ts

# 6. View results
# Tests will show pass/fail for each scenario
```

### Interactive Testing

To manually test in a browser:

```bash
# 1. Start services
cd /Users/rwalters/GitHub/elide/tools/build-arena
pnpm dev &

# 2. Wait for startup
sleep 10

# 3. Visit in browser (or use Playwright)
# http://localhost:3000/test/terminal

# 4. Click "Start Container"
# 5. Type commands: ls, pwd, java -version, elide --version
# 6. Click "Stop Container" when done
```

## Project Structure

```
build-arena/
├── frontend-vite/          # React + Vite frontend
│   ├── src/
│   │   ├── pages/
│   │   │   ├── HomePage.tsx      # Main page
│   │   │   └── TerminalTest.tsx  # Terminal test page
│   │   └── components/           # React components
│   └── package.json
├── backend/                # Node.js backend
│   ├── src/
│   │   ├── routes/
│   │   │   ├── api.ts           # Main API routes
│   │   │   └── test-api.ts      # Terminal test endpoints
│   │   ├── websocket/
│   │   │   ├── server.ts        # Job WebSocket
│   │   │   └── terminal-server.ts # Terminal WebSocket
│   │   └── db/                  # SQLite database
│   └── package.json
├── tests/                  # Playwright tests
│   ├── terminal-test.spec.ts    # Terminal tests
│   ├── homepage.spec.ts
│   └── ...
├── docker/                 # Docker images
│   ├── elide-builder.Dockerfile
│   ├── standard-builder.Dockerfile
│   └── build-images.sh
├── test-terminal.sh        # Test helper script
└── TERMINAL_TESTING.md     # Full testing guide
```

## Key Files

### Terminal Test Page
- **Frontend**: `frontend-vite/src/pages/TerminalTest.tsx`
- **Backend API**: `backend/src/routes/test-api.ts`
- **WebSocket**: `backend/src/websocket/terminal-server.ts`
- **Tests**: `tests/terminal-test.spec.ts`

### What Each Does

**TerminalTest.tsx**:
- React page with xterm.js terminal
- Buttons to start/stop containers
- WebSocket connection to container terminal

**test-api.ts**:
- `/api/test/start-container` - Creates Docker container
- `/api/test/stop-container/:id` - Stops and removes container
- `/api/test/cleanup` - Removes all test containers

**terminal-server.ts**:
- WebSocket at `/ws/terminal/:containerId`
- Bidirectional terminal I/O
- Executes `/bin/bash` in container

## Common Commands

### Development
```bash
# Start all services
pnpm dev

# Start backend only
pnpm --filter @elide/build-arena-backend dev

# Start frontend only
pnpm --filter @elide/build-arena-frontend dev

# Build Docker images
cd docker && ./build-images.sh

# Seed mock data
curl -X POST http://localhost:3001/api/dev/seed-mock-data
```

### Testing
```bash
# Run all tests
pnpm test

# Run specific test file
pnpm test tests/terminal-test.spec.ts

# Run with visible browsers
pnpm test:headed tests/terminal-test.spec.ts

# Debug mode (Playwright inspector)
pnpm exec playwright test tests/terminal-test.spec.ts --debug

# Run single test
pnpm exec playwright test -g "should execute commands"
```

### Database
```bash
# Push schema to database
pnpm --filter @elide/build-arena-backend db:push

# Open database GUI
pnpm --filter @elide/build-arena-backend db:studio

# Reset database
rm backend/build-arena.db
pnpm --filter @elide/build-arena-backend db:push
```

### Docker
```bash
# List containers
docker ps -a

# Stop all test containers
docker ps -a | grep elide-builder | awk '{print $1}' | xargs docker rm -f

# View container logs
docker logs <container-id>

# Execute command in container
docker exec -it <container-id> bash
```

## Testing Checklist

When testing the terminal page, verify:

- [ ] Page loads at http://localhost:3000/test/terminal
- [ ] "Start Container" button works
- [ ] Status changes to "Connected"
- [ ] Container ID is displayed
- [ ] Terminal accepts input
- [ ] Commands execute and show output (`echo`, `ls`, `pwd`)
- [ ] Java is installed (`java -version`)
- [ ] Elide is installed (`elide --version` or check PATH)
- [ ] "Stop Container" button works
- [ ] Status changes to "Stopped"
- [ ] "Clear Terminal" button works

## Expected Test Output

### Successful Run
```
✓ should load terminal test page
✓ should have control buttons
✓ should show disconnected status initially
✓ should start container and connect terminal
✓ should execute commands in terminal
✓ should show Java version
✓ should check if Elide is installed
✓ should stop container and disconnect
✓ should clear terminal

9 passed (25.3s)
```

### If Tests Fail

**Common Issues:**

1. **Services not running**
   ```bash
   # Fix: Start services
   pnpm dev
   ```

2. **Docker not running**
   ```bash
   # Fix: Start Docker Desktop or Docker daemon
   ```

3. **Images not built**
   ```bash
   # Fix: Build images
   cd docker && ./build-images.sh
   ```

4. **Port already in use**
   ```bash
   # Fix: Kill processes on ports 3000/3001
   lsof -ti:3000 | xargs kill -9
   lsof -ti:3001 | xargs kill -9
   ```

## API Testing

### Start Container
```bash
curl -X POST http://localhost:3001/api/test/start-container \
  -H "Content-Type: application/json" \
  -d '{"image":"elide-builder:latest"}'

# Response:
# {"containerId":"abc123...","image":"elide-builder:latest","status":"running"}
```

### Connect to Terminal via WebSocket
```bash
# Use wscat if installed
wscat -c ws://localhost:3001/ws/terminal/abc123...

# Or test in browser console:
const ws = new WebSocket('ws://localhost:3001/ws/terminal/abc123...');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
ws.send(JSON.stringify({type: 'input', data: 'ls\n'}));
```

### Stop Container
```bash
curl -X POST http://localhost:3001/api/test/stop-container/abc123...

# Response:
# {"containerId":"abc123...","status":"stopped"}
```

## Troubleshooting

### Backend won't start
```bash
# Check logs
tail -f backend.log

# Check port
lsof -i:3001

# Kill process and restart
lsof -ti:3001 | xargs kill -9
pnpm --filter @elide/build-arena-backend dev
```

### Frontend won't start
```bash
# Check port
lsof -i:3000

# Clear Vite cache
rm -rf frontend-vite/node_modules/.vite

# Restart
pnpm --filter @elide/build-arena-frontend dev
```

### Container won't start
```bash
# Check Docker
docker ps

# Check image exists
docker images | grep elide-builder

# Build if missing
cd docker && ./build-images.sh

# Check backend logs
tail -f backend.log
```

### WebSocket won't connect
```bash
# Check backend WebSocket server
curl http://localhost:3001/health

# Check browser console for errors
# Check backend logs
tail -f backend.log | grep -i websocket

# Test WebSocket manually
wscat -c ws://localhost:3001/ws/terminal/test
```

## Next Steps

After testing the terminal:

1. **Test full build arena**
   - Submit a real repository
   - Watch AI agents compete
   - Verify results are saved

2. **Test with real projects**
   - Try spring-petclinic
   - Try apache/commons-lang
   - Try google/guava

3. **Test concurrent builds**
   - Submit multiple jobs
   - Verify they run in parallel
   - Check database persistence

## Resources

- **Full Testing Guide**: `TERMINAL_TESTING.md`
- **Database Guide**: `DATABASE.md`
- **Architecture**: `ARCHITECTURE.md`
- **Popular Repos**: `POPULAR_JAVA_REPOS.md`

## Quick Help

```bash
# One command to rule them all
./test-terminal.sh headless

# If that works, you're good to go!
```

---

**Remember**: Always run tests from the `build-arena` directory:
```bash
cd /Users/rwalters/GitHub/elide/tools/build-arena
```
