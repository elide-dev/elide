# Build Arena Database

## Overview

Build Arena uses **SQLite + Drizzle ORM** for persistent job storage and worker pool management.

## Stack

- **Database**: SQLite (file-based, zero config)
- **ORM**: Drizzle (TypeScript-first, lightweight)
- **Driver**: @libsql/client (pure JavaScript, no native dependencies)

## Database Location

- **Development**: `backend/build-arena.db`
- **Production**: Set `DATABASE_PATH` environment variable

## Schema

### Tables

#### `jobs`
Stores all build comparison jobs.

```sql
CREATE TABLE jobs (
  id TEXT PRIMARY KEY,
  repository_url TEXT NOT NULL,
  repository_name TEXT NOT NULL,
  status TEXT NOT NULL,  -- 'queued' | 'running' | 'completed' | 'failed' | 'cancelled'
  created_at INTEGER NOT NULL,
  started_at INTEGER,
  completed_at INTEGER,
  worker_id TEXT
);
```

#### `build_results`
Stores individual build results (Elide and Standard).

```sql
CREATE TABLE build_results (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  job_id TEXT NOT NULL,
  build_type TEXT NOT NULL,  -- 'elide' | 'standard'
  status TEXT NOT NULL,       -- 'success' | 'failure'
  duration INTEGER NOT NULL,  -- in seconds
  output TEXT,
  bell_rung INTEGER DEFAULT 0,  -- boolean
  created_at INTEGER NOT NULL,
  FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
```

#### `workers` (Future)
Tracks available worker nodes for distributed builds.

```sql
CREATE TABLE workers (
  id TEXT PRIMARY KEY,
  hostname TEXT NOT NULL,
  status TEXT NOT NULL,           -- 'active' | 'inactive' | 'maintenance'
  capacity INTEGER NOT NULL DEFAULT 2,     -- max concurrent builds
  current_load INTEGER NOT NULL DEFAULT 0, -- current active builds
  last_heartbeat INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

#### `worker_assignments` (Future)
Maps jobs to workers.

```sql
CREATE TABLE worker_assignments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  job_id TEXT NOT NULL,
  worker_id TEXT NOT NULL,
  assigned_at INTEGER NOT NULL,
  completed_at INTEGER,
  FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
  FOREIGN KEY (worker_id) REFERENCES workers(id) ON DELETE CASCADE
);
```

## Drizzle Commands

### Push Schema to Database
```bash
pnpm --filter @elide/build-arena-backend db:push
```

### Generate Migrations
```bash
pnpm --filter @elide/build-arena-backend db:generate
```

### Run Migrations
```bash
pnpm --filter @elide/build-arena-backend db:migrate
```

### Open Drizzle Studio (Database GUI)
```bash
pnpm --filter @elide/build-arena-backend db:studio
```

## Usage

### DbJobManager

The `DbJobManager` class provides database-backed job persistence:

```typescript
import { DbJobManager } from './services/db-job-manager';

const jobManager = DbJobManager.getInstance();

// Create a job (automatically persisted)
const job = await jobManager.createJob('https://github.com/user/repo.git');

// Get a job (from database)
const job = await jobManager.getJob(jobId);

// List all jobs (from database)
const jobs = await jobManager.listJobs();

// Get recent completed jobs with results
const results = await jobManager.getRecentResults(20);
```

### Direct Database Access

```typescript
import { db, jobs, buildResults } from './db';
import { eq, desc } from 'drizzle-orm';

// Query jobs
const allJobs = await db.select().from(jobs).orderBy(desc(jobs.createdAt));

// Query with filter
const completedJobs = await db
  .select()
  .from(jobs)
  .where(eq(jobs.status, 'completed'));

// Insert a job
await db.insert(jobs).values({
  id: 'job-123',
  repositoryUrl: 'https://github.com/user/repo.git',
  repositoryName: 'user/repo',
  status: 'queued',
  createdAt: new Date(),
});

// Update a job
await db.update(jobs)
  .set({ status: 'running' })
  .where(eq(jobs.id, 'job-123'));
```

## Development

### Seed Mock Data

For testing, seed the database with mock build results:

```bash
curl -X POST http://localhost:3001/api/dev/seed-mock-data
```

This creates 5 sample jobs showing Elide's performance benefits.

### Reset Database

To start fresh:

```bash
rm backend/build-arena.db
pnpm --filter @elide/build-arena-backend db:push
```

## Scaling Considerations

### Current: Single-Server Setup
- SQLite file database
- All jobs run on localhost
- Perfect for demo and small deployments
- Handles thousands of jobs easily

### Future: Multi-Worker Setup
1. **Add Worker Registration**
   - Workers register themselves on startup
   - Heartbeat mechanism for health monitoring
   - Auto-cleanup of dead workers

2. **Load Balancing**
   - Assign jobs to least-loaded worker
   - Respect worker capacity limits
   - Queue overflow handling

3. **Database Migration**
   - SQLite → PostgreSQL when scaling beyond single server
   - Same Drizzle schema works with both
   - Just change database driver

4. **Worker Pool Management**
   ```typescript
   // Future API
   const worker = await workerPool.assignJob(jobId);
   await worker.executeBuild(jobId, repositoryUrl);
   ```

## Backup and Recovery

### Backup Database
```bash
# Simple file copy
cp backend/build-arena.db backend/build-arena.db.backup

# Or use SQLite backup command
sqlite3 backend/build-arena.db ".backup backend/build-arena.db.backup"
```

### Restore Database
```bash
cp backend/build-arena.db.backup backend/build-arena.db
```

### Automated Backups
For production, set up cron job:
```bash
0 0 * * * cp /path/to/build-arena.db /path/to/backups/build-arena-$(date +\%Y\%m\%d).db
```

## Monitoring

### Check Database Size
```bash
ls -lh backend/build-arena.db
```

### Query Database Stats
```bash
sqlite3 backend/build-arena.db "SELECT
  (SELECT COUNT(*) FROM jobs) as total_jobs,
  (SELECT COUNT(*) FROM jobs WHERE status='completed') as completed_jobs,
  (SELECT COUNT(*) FROM build_results) as total_results;"
```

### View Recent Jobs
```bash
sqlite3 backend/build-arena.db "SELECT
  id, repository_name, status, datetime(created_at, 'unixepoch')
  FROM jobs
  ORDER BY created_at DESC
  LIMIT 10;"
```

## Performance

### SQLite Optimizations

The database is configured with:
- WAL mode (write-ahead logging)
- Synchronous mode for durability
- Proper indexing on foreign keys

### Expected Performance

With a few thousand rows:
- **Insert**: <1ms
- **Select by ID**: <1ms
- **List recent 20**: <5ms
- **Database size**: ~5-10MB per 1000 jobs

This is more than sufficient for a demo/sales tool running on a $5/month VPS.
