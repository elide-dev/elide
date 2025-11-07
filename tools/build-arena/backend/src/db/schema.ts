import { sqliteTable, text, integer } from 'drizzle-orm/sqlite-core';

/**
 * Jobs table - stores all build comparison jobs
 */
export const jobs = sqliteTable('jobs', {
  id: text('id').primaryKey(),
  repositoryUrl: text('repository_url').notNull(),
  repositoryName: text('repository_name').notNull(),
  status: text('status').notNull(), // 'queued' | 'running' | 'completed' | 'failed' | 'cancelled'
  createdAt: integer('created_at', { mode: 'timestamp' }).notNull(),
  startedAt: integer('started_at', { mode: 'timestamp' }),
  completedAt: integer('completed_at', { mode: 'timestamp' }),
  workerId: text('worker_id'), // which worker is/was handling this job
});

/**
 * Build results table - stores results for each build type (elide/standard)
 */
export const buildResults = sqliteTable('build_results', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  jobId: text('job_id').notNull().references(() => jobs.id, { onDelete: 'cascade' }),
  buildType: text('build_type').notNull(), // 'elide' | 'standard'
  status: text('status').notNull(), // 'success' | 'failure'
  duration: integer('duration').notNull(), // in seconds
  output: text('output'), // build output/logs
  bellRung: integer('bell_rung', { mode: 'boolean' }).default(false),
  createdAt: integer('created_at', { mode: 'timestamp' }).notNull(),
});

/**
 * Workers table - tracks available worker nodes
 */
export const workers = sqliteTable('workers', {
  id: text('id').primaryKey(),
  hostname: text('hostname').notNull(),
  status: text('status').notNull(), // 'active' | 'inactive' | 'maintenance'
  capacity: integer('capacity').notNull().default(2), // max concurrent builds
  currentLoad: integer('current_load').notNull().default(0), // current active builds
  lastHeartbeat: integer('last_heartbeat', { mode: 'timestamp' }),
  createdAt: integer('created_at', { mode: 'timestamp' }).notNull(),
  updatedAt: integer('updated_at', { mode: 'timestamp' }).notNull(),
});

/**
 * Worker assignments table - tracks which jobs are on which workers
 */
export const workerAssignments = sqliteTable('worker_assignments', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  jobId: text('job_id').notNull().references(() => jobs.id, { onDelete: 'cascade' }),
  workerId: text('worker_id').notNull().references(() => workers.id, { onDelete: 'cascade' }),
  assignedAt: integer('assigned_at', { mode: 'timestamp' }).notNull(),
  completedAt: integer('completed_at', { mode: 'timestamp' }),
});

/**
 * Build cache table - stores metadata about cached recordings
 */
export const buildCache = sqliteTable('build_cache', {
  cacheKey: text('cache_key').primaryKey(),
  jobId: text('job_id').notNull().references(() => jobs.id),
  repositoryUrl: text('repository_url').notNull(),
  commitHash: text('commit_hash'),
  buildTool: text('build_tool').notNull(), // 'elide' | 'standard'

  recordingPath: text('recording_path').notNull(),
  fileSizeBytes: integer('file_size_bytes').notNull(),
  durationMs: integer('duration_ms').notNull(),
  messageCount: integer('message_count').notNull(),

  claudeVersion: text('claude_version').notNull(),
  dockerImage: text('docker_image').notNull(),

  createdAt: integer('created_at', { mode: 'timestamp' }).notNull(),
  accessCount: integer('access_count').notNull().default(0),
  lastAccessedAt: integer('last_accessed_at', { mode: 'timestamp' }),
});

export type Job = typeof jobs.$inferSelect;
export type NewJob = typeof jobs.$inferInsert;
export type BuildResult = typeof buildResults.$inferSelect;
export type NewBuildResult = typeof buildResults.$inferInsert;
export type Worker = typeof workers.$inferSelect;
export type NewWorker = typeof workers.$inferInsert;
export type WorkerAssignment = typeof workerAssignments.$inferSelect;
export type NewWorkerAssignment = typeof workerAssignments.$inferInsert;
export type BuildCache = typeof buildCache.$inferSelect;
export type NewBuildCache = typeof buildCache.$inferInsert;
