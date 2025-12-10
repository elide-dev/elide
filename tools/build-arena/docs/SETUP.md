# Build Arena Setup Guide

Complete setup instructions for the Build Arena development and deployment.

## Prerequisites

- Node.js 20+ and npm
- Docker and Docker Compose
- Git

## Quick Start

### 1. Build Docker Images

First, build the Docker images for the build environments:

```bash
cd tools/build-arena/docker
./build-images.sh
```

This will create:
- `elide-builder:latest` - Container with Elide installed
- `standard-builder:latest` - Container with Maven and Gradle

### 2. Setup Backend

```bash
cd tools/build-arena/backend

# Install dependencies
npm install

# Copy environment file
cp ../.env.example .env

# Start development server
npm run dev
```

The backend API will be available at `http://localhost:3001`

### 3. Setup Frontend

```bash
cd tools/build-arena/frontend

# Install dependencies
npm install

# Create environment file
cat > .env.local << EOF
NEXT_PUBLIC_API_URL=http://localhost:3001
NEXT_PUBLIC_WS_URL=ws://localhost:3001/ws
EOF

# Start development server
npm run dev
```

The frontend will be available at `http://localhost:3000`

## Architecture Overview

```
┌─────────────┐         ┌─────────────┐
│   Browser   │◄───────►│   Frontend  │
│             │  HTTP   │  (Next.js)  │
└─────────────┘         └─────────────┘
       │                       │
       │ WebSocket             │ HTTP
       │                       │
       ▼                       ▼
┌─────────────────────────────────────┐
│          Backend (Node.js)          │
│  ┌───────────┐    ┌──────────────┐ │
│  │ REST API  │    │ WebSocket    │ │
│  └───────────┘    │ Server       │ │
│  ┌───────────┐    └──────────────┘ │
│  │    Job    │                      │
│  │  Manager  │                      │
│  └───────────┘                      │
│       │                              │
│       ▼                              │
│  ┌───────────────┐                  │
│  │   Sandbox     │                  │
│  │   Runner      │                  │
│  └───────────────┘                  │
└──────────┬──────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │    Docker    │
    │  ┌────────┐  │
    │  │ Elide  │  │
    │  │Builder │  │
    │  └────────┘  │
    │  ┌────────┐  │
    │  │Standard│  │
    │  │Builder │  │
    │  └────────┘  │
    └──────────────┘
```

## Project Structure

```
tools/build-arena/
├── backend/              # Node.js/TypeScript backend
│   ├── src/
│   │   ├── index.ts             # Entry point
│   │   ├── routes/
│   │   │   └── api.ts           # REST API routes
│   │   ├── services/
│   │   │   ├── job-manager.ts   # Job lifecycle management
│   │   │   └── sandbox-runner.ts # Docker execution
│   │   └── websocket/
│   │       └── server.ts        # WebSocket handling
│   ├── package.json
│   └── tsconfig.json
│
├── frontend/             # Next.js React frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── page.tsx         # Home page
│   │   │   └── layout.tsx       # Layout wrapper
│   │   └── components/
│   │       ├── RepositoryForm.tsx   # Job submission form
│   │       ├── BuildArena.tsx       # Main arena view
│   │       ├── Terminal.tsx         # XTerm.js terminal
│   │       └── BuildMetrics.tsx     # Metrics display
│   ├── package.json
│   └── next.config.js
│
├── shared/               # Shared TypeScript types
│   └── types.ts
│
├── docker/               # Docker build environments
│   ├── elide-builder.Dockerfile
│   ├── standard-builder.Dockerfile
│   └── build-images.sh
│
└── README.md            # Documentation
```

## API Reference

### REST API Endpoints

#### `POST /api/jobs`
Submit a new build job

**Request:**
```json
{
  "repositoryUrl": "https://github.com/user/repo.git"
}
```

**Response:**
```json
{
  "jobId": "uuid",
  "message": "Job created successfully"
}
```

#### `GET /api/jobs/:jobId`
Get job status and results

**Response:**
```json
{
  "job": {
    "id": "uuid",
    "repositoryUrl": "https://github.com/user/repo.git",
    "repositoryName": "user/repo",
    "status": "running",
    "createdAt": "2025-01-06T...",
    "elideResult": { ... },
    "standardResult": { ... }
  }
}
```

#### `GET /api/jobs`
List all jobs

#### `POST /api/jobs/:jobId/cancel`
Cancel a running job

### WebSocket API

Connect to `ws://localhost:3001/ws`

**Subscribe to job updates:**
```json
{
  "type": "subscribe",
  "payload": {
    "jobId": "uuid"
  }
}
```

**Message types received:**
- `terminal_output` - Real-time terminal output
- `build_started` - Build started notification
- `build_completed` - Build completed with results
- `error` - Error messages

## Development

### Backend Development

```bash
cd backend

# Run in development mode with hot reload
npm run dev

# Build for production
npm run build

# Run production build
npm start

# Run tests
npm test

# Lint code
npm run lint
```

### Frontend Development

```bash
cd frontend

# Run development server
npm run dev

# Build for production
npm run build

# Run production build
npm start

# Lint code
npm run lint
```

## Docker Images

### Building Images

The Docker images need to be built before running the application:

```bash
cd docker
./build-images.sh
```

### Customizing Build Environments

Edit the Dockerfiles to:
- Add additional build tools
- Change Java versions
- Install project-specific dependencies
- Configure build optimizations

### Image Details

**elide-builder:latest**
- Base: Ubuntu 22.04
- Java: OpenJDK 17
- Includes: Elide runtime
- Purpose: Build projects with Elide toolchain

**standard-builder:latest**
- Base: Ubuntu 22.04
- Java: OpenJDK 17
- Includes: Maven, Gradle 8.5
- Purpose: Build projects with standard tools

## Troubleshooting

### Docker Issues

**Error: Image not found**
```bash
cd docker
./build-images.sh
```

**Error: Permission denied**
```bash
# Ensure Docker daemon is running
docker ps

# Check Docker permissions
sudo usermod -aG docker $USER
```

### Backend Issues

**Error: Cannot connect to Docker**
- Ensure Docker daemon is running
- Check DOCKER_HOST environment variable
- Verify Docker socket permissions

**Error: Port already in use**
```bash
# Change PORT in .env file
PORT=3002
```

### Frontend Issues

**Error: Cannot connect to backend**
- Ensure backend is running on port 3001
- Check NEXT_PUBLIC_API_URL in .env.local
- Verify CORS is enabled in backend

**Error: WebSocket connection failed**
- Ensure backend WebSocket server is running
- Check NEXT_PUBLIC_WS_URL in .env.local
- Check browser console for errors

## Production Deployment

### Docker Compose (Recommended)

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "3001:3001"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - NODE_ENV=production

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_URL=http://backend:3001
      - NEXT_PUBLIC_WS_URL=ws://backend:3001/ws
    depends_on:
      - backend
```

### Environment Variables

**Production backend (.env):**
```bash
PORT=3001
NODE_ENV=production
```

**Production frontend (.env.local):**
```bash
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
NEXT_PUBLIC_WS_URL=wss://api.yourdomain.com/ws
```

### Security Considerations

1. **Rate limiting**: Add rate limiting to prevent abuse
2. **Repository validation**: Validate repository URLs strictly
3. **Resource limits**: Configure Docker resource limits
4. **Timeout enforcement**: Set timeouts for long-running builds
5. **Access control**: Add authentication for production deployment

## Testing

### Testing the Backend

```bash
cd backend

# Run unit tests
npm test

# Test API endpoint
curl -X POST http://localhost:3001/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"repositoryUrl":"https://github.com/spring-projects/spring-petclinic.git"}'
```

### Testing the Frontend

```bash
cd frontend
npm run dev

# Open browser to http://localhost:3000
# Submit a test repository
```

### End-to-End Testing

1. Start backend: `cd backend && npm run dev`
2. Start frontend: `cd frontend && npm run dev`
3. Open `http://localhost:3000`
4. Submit test repository (e.g., Spring PetClinic)
5. Watch live terminal output in both panels
6. Verify metrics are displayed correctly

## Contributing

When making changes:

1. Update types in `shared/types.ts` if API changes
2. Test both frontend and backend together
3. Ensure Docker images build successfully
4. Update documentation for new features

## License

MIT
