import { Router, type Router as ExpressRouter } from 'express';
import { z } from 'zod';
import { DbJobManager } from '../services/db-job-manager.js';
import type { SubmitJobResponse, JobStatusResponse } from '../../../shared/types.js';

export const apiRouter: ExpressRouter = Router();
const jobManager = DbJobManager.getInstance();

// Validation schema for repository URL
const submitJobSchema = z.object({
  repositoryUrl: z.string().url().refine(
    (url) => {
      // Must be a GitHub, GitLab, or other public git repository
      return url.startsWith('https://github.com/') ||
             url.startsWith('https://gitlab.com/') ||
             url.endsWith('.git');
    },
    { message: 'Must be a valid public Git repository URL' }
  ),
});

// Submit a new build job
apiRouter.post('/jobs', async (req, res) => {
  try {
    const validatedData = submitJobSchema.parse(req.body);

    const job = await jobManager.createJob(validatedData.repositoryUrl);

    const response: SubmitJobResponse = {
      jobId: job.id,
      message: 'Job created successfully',
    };

    res.status(201).json(response);
  } catch (error) {
    if (error instanceof z.ZodError) {
      res.status(400).json({
        error: 'Invalid request',
        details: error.errors,
      });
    } else {
      console.error('Error creating job:', error);
      res.status(500).json({
        error: 'Failed to create job',
      });
    }
  }
});

// Get job status
apiRouter.get('/jobs/:jobId', async (req, res) => {
  try {
    const job = await jobManager.getJob(req.params.jobId);

    if (!job) {
      res.status(404).json({
        error: 'Job not found',
      });
      return;
    }

    const response: JobStatusResponse = {
      job,
    };

    res.json(response);
  } catch (error) {
    console.error('Error fetching job:', error);
    res.status(500).json({
      error: 'Failed to fetch job',
    });
  }
});

// Get recent completed jobs with results (for homepage results table)
apiRouter.get('/jobs/recent/results', async (req, res) => {
  try {
    const limit = req.query.limit ? Number.parseInt(req.query.limit as string, 10) : 20;
    const jobs = await jobManager.getRecentResults(limit);
    res.json({ jobs });
  } catch (error) {
    console.error('Error fetching recent results:', error);
    res.status(500).json({
      error: 'Failed to fetch recent results',
    });
  }
});

// List all jobs (for admin/debugging)
apiRouter.get('/jobs', async (_req, res) => {
  try {
    const jobs = await jobManager.listJobs();
    res.json({ jobs });
  } catch (error) {
    console.error('Error listing jobs:', error);
    res.status(500).json({
      error: 'Failed to list jobs',
    });
  }
});

// Cancel a job
apiRouter.post('/jobs/:jobId/cancel', async (req, res) => {
  try {
    const success = await jobManager.cancelJob(req.params.jobId);

    if (!success) {
      res.status(404).json({
        error: 'Job not found or already completed',
      });
      return;
    }

    res.json({
      message: 'Job cancelled successfully',
    });
  } catch (error) {
    console.error('Error cancelling job:', error);
    res.status(500).json({
      error: 'Failed to cancel job',
    });
  }
});

// Seed mock data (development only)
if (process.env.NODE_ENV !== 'production') {
  apiRouter.post('/dev/seed-mock-data', async (_req, res) => {
    try {
      const { db, jobs: jobsTable, buildResults: buildResultsTable } = await import('../db/index.js');

      const mockRepos = [
        { name: 'spring-projects/spring-petclinic', url: 'https://github.com/spring-projects/spring-petclinic.git', elideTime: 45, standardTime: 78 },
        { name: 'apache/commons-lang', url: 'https://github.com/apache/commons-lang.git', elideTime: 32, standardTime: 55 },
        { name: 'google/guava', url: 'https://github.com/google/guava.git', elideTime: 89, standardTime: 142 },
        { name: 'square/okhttp', url: 'https://github.com/square/okhttp.git', elideTime: 41, standardTime: 63 },
        { name: 'junit-team/junit5', url: 'https://github.com/junit-team/junit5.git', elideTime: 67, standardTime: 98 },
      ];

      const createdJobs = [];
      for (let i = 0; i < mockRepos.length; i++) {
        const repo = mockRepos[i];
        const jobId = `mock-job-${i}-${Date.now()}`;
        const createdAt = new Date(Date.now() - (i * 3600000));

        // Insert job
        await db.insert(jobsTable).values({
          id: jobId,
          repositoryUrl: repo.url,
          repositoryName: repo.name,
          status: 'completed',
          createdAt,
          startedAt: createdAt,
          completedAt: new Date(createdAt.getTime() + Math.max(repo.elideTime, repo.standardTime) * 1000),
        });

        // Insert Elide result
        await db.insert(buildResultsTable).values({
          jobId,
          buildType: 'elide',
          status: 'success',
          duration: repo.elideTime,
          output: 'Build successful with Elide',
          bellRung: true,
          createdAt,
        });

        // Insert Standard result
        await db.insert(buildResultsTable).values({
          jobId,
          buildType: 'standard',
          status: 'success',
          duration: repo.standardTime,
          output: 'Build successful with standard toolchain',
          bellRung: true,
          createdAt,
        });

        createdJobs.push({ id: jobId, name: repo.name });
      }

      res.json({
        message: `Seeded ${createdJobs.length} mock jobs`,
        jobs: createdJobs,
      });
    } catch (error) {
      console.error('Error seeding mock data:', error);
      res.status(500).json({
        error: 'Failed to seed mock data',
        details: error instanceof Error ? error.message : 'Unknown error',
      });
    }
  });
}
