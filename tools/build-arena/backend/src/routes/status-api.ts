/**
 * Status API - Health checks and debugging information
 * 
 * Provides comprehensive status information about all services:
 * - WebSocket server health
 * - Active terminal sessions
 * - Active minders
 * - Database connectivity
 * - Docker connectivity
 * - System resources
 */

import { Router, type Router as ExpressRouter } from 'express';
import { db } from '../db/index.js';
import { jobs, buildResults } from '../db/schema.js';
import { sql } from 'drizzle-orm';
import { spawn } from 'child_process';
import { getActiveMinders } from '../services/race-minder.js';
import { getRecoveryStatus, reconnectOrphanedRaces } from '../services/race-recovery.js';

export const statusApiRouter: ExpressRouter = Router();

/**
 * GET /api/status - Overall system health
 */
statusApiRouter.get('/', async (req, res) => {
  const status = {
    service: 'build-arena-backend',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memory: {
      used: Math.round(process.memoryUsage().heapUsed / 1024 / 1024),
      total: Math.round(process.memoryUsage().heapTotal / 1024 / 1024),
      rss: Math.round(process.memoryUsage().rss / 1024 / 1024),
      unit: 'MB'
    },
    env: {
      node: process.version,
      platform: process.platform,
      hasApiKey: !!process.env.ANTHROPIC_API_KEY,
    },
    services: {
      database: await checkDatabase(),
      docker: await checkDocker(),
      websocket: checkWebSocket(),
      minders: checkMinders(),
    }
  };

  const healthy = Object.values(status.services).every(s => s.healthy);
  res.status(healthy ? 200 : 503).json(status);
});

/**
 * GET /api/status/database - Database health and statistics
 */
statusApiRouter.get('/database', async (req, res) => {
  try {
    const dbStatus = await checkDatabase();
    
    // Get counts
    const [jobCount] = await db.select({ count: sql<number>`count(*)` }).from(jobs);
    const [resultCount] = await db.select({ count: sql<number>`count(*)` }).from(buildResults);
    
    // Get recent jobs
    const recentJobs = await db
      .select({ 
        id: jobs.id, 
        status: jobs.status, 
        createdAt: jobs.createdAt 
      })
      .from(jobs)
      .orderBy(sql`${jobs.createdAt} DESC`)
      .limit(5);

    res.json({
      ...dbStatus,
      statistics: {
        totalJobs: jobCount.count,
        totalResults: resultCount.count,
      },
      recentJobs,
    });
  } catch (error: any) {
    res.status(500).json({
      healthy: false,
      error: error.message,
    });
  }
});

/**
 * GET /api/status/docker - Docker connectivity and containers
 */
statusApiRouter.get('/docker', async (req, res) => {
  try {
    const dockerStatus = await checkDocker();
    
    // Get race containers
    const containers = await new Promise<any[]>((resolve, reject) => {
      const docker = spawn('docker', ['ps', '-a', '--filter', 'name=race-', '--format', '{{json .}}']);
      let output = '';
      
      docker.stdout.on('data', (data) => {
        output += data.toString();
      });
      
      docker.on('close', (code) => {
        if (code === 0) {
          const lines = output.trim().split('\n').filter(Boolean);
          const containers = lines.map(line => JSON.parse(line));
          resolve(containers);
        } else {
          reject(new Error(`Docker ps failed with code ${code}`));
        }
      });
      
      docker.on('error', reject);
    });

    res.json({
      ...dockerStatus,
      containers: containers.map(c => ({
        id: c.ID,
        name: c.Names,
        status: c.Status,
        createdAt: c.CreatedAt,
      })),
    });
  } catch (error: any) {
    res.status(500).json({
      healthy: false,
      error: error.message,
    });
  }
});

/**
 * GET /api/status/websocket - WebSocket server status
 */
statusApiRouter.get('/websocket', async (req, res) => {
  const wsStatus = checkWebSocket();
  
  // TODO: Add active sessions info when we have a sessions registry
  res.json(wsStatus);
});

/**
 * GET /api/status/minders - Active minders status
 */
statusApiRouter.get('/minders', async (req, res) => {
  const minderStatus = checkMinders();
  const minders = getActiveMinders();

  const minderDetails = Array.from(minders.values()).map(minder => minder.getStatus());

  res.json({
    ...minderStatus,
    minders: minderDetails,
  });
});

/**
 * GET /api/status/recovery - Race recovery status
 */
statusApiRouter.get('/recovery', async (req, res) => {
  try {
    const recoveryStatus = await getRecoveryStatus();
    res.json(recoveryStatus);
  } catch (error: any) {
    res.status(500).json({
      error: error.message,
    });
  }
});

/**
 * POST /api/status/recovery/reconnect - Manually trigger race reconnection
 */
statusApiRouter.post('/recovery/reconnect', async (req, res) => {
  try {
    const count = await reconnectOrphanedRaces();
    res.json({
      success: true,
      reconnected: count,
      message: count > 0
        ? `Reconnected ${count} orphaned race(s)`
        : 'No orphaned races found',
    });
  } catch (error: any) {
    res.status(500).json({
      error: error.message,
    });
  }
});

// Helper functions

async function checkDatabase() {
  try {
    await db.select({ count: sql<number>`1` }).from(jobs).limit(1);
    return {
      healthy: true,
      message: 'Database connection successful',
    };
  } catch (error: any) {
    return {
      healthy: false,
      error: error.message,
    };
  }
}

async function checkDocker() {
  return new Promise<{ healthy: boolean; version?: string; error?: string }>((resolve) => {
    const docker = spawn('docker', ['version', '--format', '{{.Server.Version}}']);
    let version = '';
    let error = '';
    
    docker.stdout.on('data', (data) => {
      version += data.toString().trim();
    });
    
    docker.stderr.on('data', (data) => {
      error += data.toString();
    });
    
    docker.on('close', (code) => {
      if (code === 0 && version) {
        resolve({
          healthy: true,
          version,
        });
      } else {
        resolve({
          healthy: false,
          error: error || 'Docker not responding',
        });
      }
    });
    
    docker.on('error', (err) => {
      resolve({
        healthy: false,
        error: err.message,
      });
    });
  });
}

function checkWebSocket() {
  // TODO: Check if WebSocket server is actually listening
  // For now, just return a basic status
  return {
    healthy: true,
    message: 'WebSocket server initialized',
  };
}

function checkMinders() {
  const minders = getActiveMinders();
  const count = minders.size;
  const connected = Array.from(minders.values()).filter(m => m.getStatus().connected).length;
  
  return {
    healthy: true,
    total: count,
    connected,
    disconnected: count - connected,
  };
}
