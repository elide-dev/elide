# Status API Documentation

The Status API provides comprehensive health checks and debugging information for the Build Arena system.

## Endpoints

### `GET /api/status`
**Overall system health check**

Returns health status for all services plus system metrics.

**Response:**
```json
{
  "service": "build-arena-backend",
  "version": "1.0.0",
  "timestamp": "2025-11-14T20:45:00.000Z",
  "uptime": 123.45,
  "memory": {
    "used": 21,
    "total": 37,
    "rss": 173,
    "unit": "MB"
  },
  "env": {
    "node": "v20.11.0",
    "platform": "darwin",
    "hasApiKey": true
  },
  "services": {
    "database": {
      "healthy": true,
      "message": "Database connection successful"
    },
    "docker": {
      "healthy": true,
      "version": "28.5.2"
    },
    "websocket": {
      "healthy": true,
      "message": "WebSocket server initialized"
    },
    "minders": {
      "healthy": true,
      "total": 2,
      "connected": 2,
      "disconnected": 0
    }
  }
}
```

**HTTP Status Codes:**
- `200 OK` - All services healthy
- `503 Service Unavailable` - One or more services unhealthy

---

### `GET /api/status/database`
**Database health and statistics**

Returns database connectivity status plus job/result counts and recent jobs.

**Response:**
```json
{
  "healthy": true,
  "message": "Database connection successful",
  "statistics": {
    "totalJobs": 12,
    "totalResults": 24
  },
  "recentJobs": [
    {
      "id": "904553ec-0c60-45f3-a682-1b719b6c324c",
      "status": "running",
      "createdAt": "2025-11-14T20:30:00.000Z"
    }
  ]
}
```

---

### `GET /api/status/docker`
**Docker connectivity and containers**

Returns Docker version and list of active race containers.

**Response:**
```json
{
  "healthy": true,
  "version": "28.5.2",
  "containers": [
    {
      "id": "4a9bdedda17e",
      "name": "race-904553ec-elide",
      "status": "Up 5 minutes",
      "createdAt": "2025-11-14 20:30:00"
    },
    {
      "id": "56fcdaab8a0e",
      "name": "race-904553ec-standard",
      "status": "Up 5 minutes",
      "createdAt": "2025-11-14 20:30:00"
    }
  ]
}
```

---

### `GET /api/status/websocket`
**WebSocket server status**

Returns WebSocket server health status.

**Response:**
```json
{
  "healthy": true,
  "message": "WebSocket server initialized"
}
```

---

### `GET /api/status/minders`
**Active minders status**

Returns detailed information about all active race minders.

**Response:**
```json
{
  "healthy": true,
  "total": 2,
  "connected": 2,
  "disconnected": 0,
  "minders": [
    {
      "containerId": "4a9bdedda17e",
      "buildType": "elide",
      "repoUrl": "https://github.com/google/gson",
      "connected": true,
      "uptime": 123,
      "lastActivity": "Approving workspace trust",
      "approvalCount": 2,
      "state": {
        "themeHandled": true,
        "apiKeyHandled": true,
        "trustHandled": true,
        "workspaceTrustHandled": true,
        "claudeStarted": false,
        "bellRung": false
      },
      "lastOutputSnippet": "..."
    }
  ]
}
```

---

## Use Cases

### 1. Health Monitoring
```bash
# Quick health check
curl http://localhost:3001/api/status

# Check specific service
curl http://localhost:3001/api/status/docker
```

### 2. Debugging Stuck Races
```bash
# Check minder status to see where they're stuck
curl http://localhost:3001/api/status/minders | jq '.minders[] | {buildType, lastActivity, state}'
```

### 3. Container Management
```bash
# List all race containers
curl http://localhost:3001/api/status/docker | jq '.containers'
```

### 4. Database Statistics
```bash
# Get job counts and recent activity
curl http://localhost:3001/api/status/database | jq '{stats: .statistics, recentCount: .recentJobs | length}'
```

---

## Integration with Frontend

The status API can be used by the frontend for:

1. **Health Dashboard**: Display system health in the UI
2. **Debug Console**: Show minder status and container info
3. **Error Diagnosis**: Automatically check services when races fail
4. **Monitoring**: Poll status endpoints to detect issues

Example React hook:
```typescript
export function useSystemStatus() {
  const [status, setStatus] = useState(null);
  
  useEffect(() => {
    const fetchStatus = async () => {
      const res = await fetch('/api/status');
      const data = await res.json();
      setStatus(data);
    };
    
    fetchStatus();
    const interval = setInterval(fetchStatus, 10000); // Poll every 10s
    
    return () => clearInterval(interval);
  }, []);
  
  return status;
}
```

---

## Why This Makes the System More Robust

### 1. **Visibility**
Before: No way to see what's happening inside the system
After: Comprehensive view of all services, minders, containers

### 2. **Debugging**
Before: Had to guess what's wrong by looking at logs
After: Can quickly check status of specific services

### 3. **Monitoring**
Before: No automated health checks
After: Can set up monitoring tools to poll `/api/status`

### 4. **Problem Detection**
Before: Users report "it's not working"
After: Can immediately identify which service is failing

### 5. **Documentation**
Before: Unclear what the system state is
After: Status API serves as living documentation

---

## Future Enhancements

1. **WebSocket Session Tracking**: Add registry of active terminal sessions
2. **Historical Metrics**: Track uptime, request counts, error rates over time
3. **Alerting**: Trigger alerts when services become unhealthy
4. **Performance Metrics**: Add response times, throughput statistics
5. **Resource Usage**: Track CPU, disk, network usage per container
