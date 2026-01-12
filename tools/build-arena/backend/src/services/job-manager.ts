import { v4 as uuidv4 } from 'uuid';
import type { BuildJob, JobStatus } from '../../../shared/types.js';
import { InteractiveSandboxRunner } from './interactive-sandbox-runner.js';
import { EventEmitter } from 'events';

/**
 * Manages build job lifecycle and execution
 */
export class JobManager extends EventEmitter {
  private static instance: JobManager;
  private jobs: Map<string, BuildJob> = new Map();
  public sandboxRunner: InteractiveSandboxRunner; // Changed to public for WebSocket access

  private constructor() {
    super();
    this.sandboxRunner = new InteractiveSandboxRunner();
  }

  static getInstance(): JobManager {
    if (!JobManager.instance) {
      JobManager.instance = new JobManager();
    }
    return JobManager.instance;
  }

  /**
   * Create a new build job
   */
  async createJob(repositoryUrl: string): Promise<BuildJob> {
    const jobId = uuidv4();
    const repositoryName = this.extractRepositoryName(repositoryUrl);

    const job: BuildJob = {
      id: jobId,
      repositoryUrl,
      repositoryName,
      createdAt: new Date().toISOString(),
      status: 'queued',
    };

    this.jobs.set(jobId, job);

    // Start the build process asynchronously
    this.executeBuild(jobId).catch((error) => {
      console.error(`Error executing build for job ${jobId}:`, error);
      this.updateJobStatus(jobId, 'failed');
    });

    return job;
  }

  /**
   * Get a job by ID
   */
  async getJob(jobId: string): Promise<BuildJob | null> {
    return this.jobs.get(jobId) || null;
  }

  /**
   * List all jobs
   */
  async listJobs(): Promise<BuildJob[]> {
    return Array.from(this.jobs.values()).sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }

  /**
   * Get recent completed jobs with results (for results table)
   */
  async getRecentResults(limit: number = 20): Promise<BuildJob[]> {
    return Array.from(this.jobs.values())
      .filter(job => job.status === 'completed' && job.elideResult && job.standardResult)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, limit);
  }

  /**
   * Cancel a running job
   */
  async cancelJob(jobId: string): Promise<boolean> {
    const job = this.jobs.get(jobId);
    if (!job || job.status === 'completed' || job.status === 'failed') {
      return false;
    }

    this.updateJobStatus(jobId, 'cancelled');
    this.emit('job:cancelled', jobId);
    await this.sandboxRunner.stopBuild(jobId);

    return true;
  }

  /**
   * Execute the build comparison
   */
  private async executeBuild(jobId: string): Promise<void> {
    this.updateJobStatus(jobId, 'running');
    this.emit('job:started', jobId);

    const job = this.jobs.get(jobId);
    if (!job) {
      throw new Error(`Job ${jobId} not found`);
    }

    try {
      // Run both builds in parallel with interactive Claude Code agents
      const [elideResult, standardResult] = await Promise.all([
        this.sandboxRunner.runInteractiveBuild(jobId, job.repositoryUrl, 'elide'),
        this.sandboxRunner.runInteractiveBuild(jobId, job.repositoryUrl, 'standard'),
      ]);

      // Update job with results
      job.elideResult = elideResult;
      job.standardResult = standardResult;

      // Determine overall status
      const hasFailures = elideResult.status === 'failure' || standardResult.status === 'failure';
      this.updateJobStatus(jobId, hasFailures ? 'failed' : 'completed');

      this.emit('job:completed', jobId);
    } catch (error) {
      console.error(`Build execution failed for job ${jobId}:`, error);
      this.updateJobStatus(jobId, 'failed');
      this.emit('job:failed', jobId, error);
    }
  }

  /**
   * Update job status
   */
  private updateJobStatus(jobId: string, status: JobStatus): void {
    const job = this.jobs.get(jobId);
    if (job) {
      job.status = status;
      this.emit('job:status_changed', jobId, status);
    }
  }

  /**
   * Extract repository name from URL
   */
  private extractRepositoryName(url: string): string {
    // Extract repository name from URL
    // e.g., https://github.com/user/repo.git -> user/repo
    const match = url.match(/github\.com\/([^/]+\/[^/]+?)(\.git)?$/);
    if (match) {
      return match[1];
    }

    // Fallback: try to extract last two path segments
    const parts = url.replace('.git', '').split('/').filter(Boolean);
    return parts.slice(-2).join('/');
  }
}
