import { EventEmitter } from 'events';
import { v4 as uuidv4 } from 'uuid';
import { eq, desc } from 'drizzle-orm';
import { db, jobs, buildResults, type Job, type NewJob, type NewBuildResult } from '../db/index.js';
import type { BuildJob, JobStatus, BuildResult } from '../../../shared/types.js';
import { InteractiveSandboxRunner } from './interactive-sandbox-runner.js';

/**
 * Database-backed job manager with persistent storage
 */
export class DbJobManager extends EventEmitter {
  private static instance: DbJobManager;
  public sandboxRunner: InteractiveSandboxRunner;

  private constructor() {
    super();
    this.sandboxRunner = new InteractiveSandboxRunner();
    this.initializeLocalWorker();
  }

  static getInstance(): DbJobManager {
    if (!DbJobManager.instance) {
      DbJobManager.instance = new DbJobManager();
    }
    return DbJobManager.instance;
  }

  /**
   * Initialize local worker (localhost)
   * In a multi-worker setup, this would register with a separate worker registry
   */
  private async initializeLocalWorker(): Promise<void> {
    // For now, we run jobs locally
    // Future: register with worker pool management
    console.log('Local worker initialized');
  }

  /**
   * Create a new build job
   */
  async createJob(repositoryUrl: string): Promise<BuildJob> {
    const jobId = uuidv4();
    const repositoryName = this.extractRepositoryName(repositoryUrl);

    // Insert into database
    const newJob: NewJob = {
      id: jobId,
      repositoryUrl,
      repositoryName,
      status: 'queued',
      createdAt: new Date(),
    };

    await db.insert(jobs).values(newJob);

    // Start build asynchronously
    this.executeBuild(jobId).catch((error) => {
      console.error(`Error executing build for job ${jobId}:`, error);
      this.updateJobStatus(jobId, 'failed');
    });

    return this.dbJobToBuildJob(newJob);
  }

  /**
   * Get a job by ID
   */
  async getJob(jobId: string): Promise<BuildJob | null> {
    const [job] = await db.select().from(jobs).where(eq(jobs.id, jobId));
    if (!job) return null;

    // Fetch build results
    const results = await db.select().from(buildResults).where(eq(buildResults.jobId, jobId));

    const elideResult = results.find(r => r.buildType === 'elide');
    const standardResult = results.find(r => r.buildType === 'standard');

    return this.dbJobToBuildJob(job, elideResult, standardResult);
  }

  /**
   * List all jobs
   */
  async listJobs(): Promise<BuildJob[]> {
    const allJobs = await db.select().from(jobs).orderBy(desc(jobs.createdAt));

    // Fetch all results for these jobs
    const jobIds = allJobs.map(j => j.id);
    const allResults = await db.select().from(buildResults);

    return allJobs.map(job => {
      const elideResult = allResults.find(r => r.jobId === job.id && r.buildType === 'elide');
      const standardResult = allResults.find(r => r.jobId === job.id && r.buildType === 'standard');
      return this.dbJobToBuildJob(job, elideResult, standardResult);
    });
  }

  /**
   * Get recent completed jobs with results
   */
  async getRecentResults(limit: number = 20): Promise<BuildJob[]> {
    const completedJobs = await db
      .select()
      .from(jobs)
      .where(eq(jobs.status, 'completed'))
      .orderBy(desc(jobs.createdAt))
      .limit(limit);

    // Fetch results for these jobs
    const jobIds = completedJobs.map(j => j.id);
    const allResults = await db.select().from(buildResults);

    return completedJobs
      .map(job => {
        const elideResult = allResults.find(r => r.jobId === job.id && r.buildType === 'elide');
        const standardResult = allResults.find(r => r.jobId === job.id && r.buildType === 'standard');
        return this.dbJobToBuildJob(job, elideResult, standardResult);
      })
      .filter(job => job.elideResult && job.standardResult);
  }

  /**
   * Cancel a running job
   */
  async cancelJob(jobId: string): Promise<boolean> {
    const job = await this.getJob(jobId);
    if (!job || job.status === 'completed' || job.status === 'failed') {
      return false;
    }

    await this.updateJobStatus(jobId, 'cancelled');
    this.emit('job:cancelled', jobId);
    await this.sandboxRunner.stopBuild(jobId);

    return true;
  }

  /**
   * Execute the build comparison
   */
  private async executeBuild(jobId: string): Promise<void> {
    await this.updateJobStatus(jobId, 'running');
    await db.update(jobs).set({ startedAt: new Date() }).where(eq(jobs.id, jobId));
    this.emit('job:started', jobId);

    const job = await this.getJob(jobId);
    if (!job) {
      throw new Error(`Job ${jobId} not found`);
    }

    try {
      // Run both builds in parallel
      const [elideResult, standardResult] = await Promise.all([
        this.sandboxRunner.runInteractiveBuild(jobId, job.repositoryUrl, 'elide'),
        this.sandboxRunner.runInteractiveBuild(jobId, job.repositoryUrl, 'standard'),
      ]);

      // Save results to database
      await this.saveBuildResult(jobId, 'elide', elideResult);
      await this.saveBuildResult(jobId, 'standard', standardResult);

      // Update job status
      const hasFailures = elideResult.status === 'failure' || standardResult.status === 'failure';
      await this.updateJobStatus(jobId, hasFailures ? 'failed' : 'completed');
      await db.update(jobs).set({ completedAt: new Date() }).where(eq(jobs.id, jobId));

      this.emit('job:completed', jobId);
    } catch (error) {
      console.error(`Build execution failed for job ${jobId}:`, error);
      await this.updateJobStatus(jobId, 'failed');
      await db.update(jobs).set({ completedAt: new Date() }).where(eq(jobs.id, jobId));
      this.emit('job:failed', jobId, error);
    }
  }

  /**
   * Save build result to database
   */
  private async saveBuildResult(
    jobId: string,
    buildType: 'elide' | 'standard',
    result: BuildResult
  ): Promise<void> {
    const newResult: NewBuildResult = {
      jobId,
      buildType,
      status: result.status,
      duration: result.duration,
      output: result.output,
      bellRung: result.bellRung || false,
      createdAt: new Date(),
    };

    await db.insert(buildResults).values(newResult);
  }

  /**
   * Update job status
   */
  private async updateJobStatus(jobId: string, status: JobStatus): Promise<void> {
    await db.update(jobs).set({ status }).where(eq(jobs.id, jobId));
    this.emit('job:status_changed', jobId, status);
  }

  /**
   * Extract repository name from URL
   */
  private extractRepositoryName(url: string): string {
    const match = url.match(/github\.com\/([^/]+\/[^/]+?)(\.git)?$/);
    if (match) {
      return match[1];
    }

    const parts = url.replace('.git', '').split('/').filter(Boolean);
    return parts.slice(-2).join('/');
  }

  /**
   * Convert database job to BuildJob type
   */
  private dbJobToBuildJob(
    job: Job,
    elideResult?: typeof buildResults.$inferSelect,
    standardResult?: typeof buildResults.$inferSelect
  ): BuildJob {
    return {
      id: job.id,
      repositoryUrl: job.repositoryUrl,
      repositoryName: job.repositoryName,
      status: job.status as JobStatus,
      createdAt: job.createdAt.toISOString(),
      elideResult: elideResult ? {
        status: elideResult.status as 'success' | 'failure',
        duration: elideResult.duration,
        output: elideResult.output || '',
        bellRung: elideResult.bellRung || false,
      } : undefined,
      standardResult: standardResult ? {
        status: standardResult.status as 'success' | 'failure',
        duration: standardResult.duration,
        output: standardResult.output || '',
        bellRung: standardResult.bellRung || false,
      } : undefined,
    };
  }
}
