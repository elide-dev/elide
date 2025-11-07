import { promises as fs } from 'fs';
import { createGzip } from 'zlib';
import { promisify } from 'util';
import { pipeline } from 'stream';
import * as path from 'path';
import * as crypto from 'crypto';

const pipelineAsync = promisify(pipeline);

interface RecordedMessage {
  ts: number; // Timestamp offset from start (milliseconds)
  msg: any;   // The WebSocket message
}

interface Recording {
  version: number;
  duration: number;
  messages: RecordedMessage[];
  metadata: {
    jobId: string;
    tool: string;
    repositoryUrl: string;
    commitHash?: string;
    claudeVersion: string;
    dockerImage: string;
    timestamp: string;
  };
}

export interface CacheKeyParams {
  repositoryUrl: string;
  commitHash?: string;
  tool: string;
  claudeVersion: string;
  dockerImage: string;
}

/**
 * Records WebSocket messages during live builds for later replay
 */
export class WebSocketRecorder {
  private messages: RecordedMessage[] = [];
  private startTime: number;
  private jobId: string;
  private tool: string;
  private metadata: Recording['metadata'];
  private recording: boolean = false;

  constructor(jobId: string, tool: string, metadata: Omit<Recording['metadata'], 'timestamp'>) {
    this.jobId = jobId;
    this.tool = tool;
    this.startTime = Date.now();
    this.metadata = {
      ...metadata,
      timestamp: new Date().toISOString()
    };
  }

  /**
   * Start recording messages
   */
  start(): void {
    this.recording = true;
    this.startTime = Date.now();
    this.messages = [];
    console.log(`[Recorder] Started recording for job ${this.jobId} (${this.tool})`);
  }

  /**
   * Record a WebSocket message with timestamp
   */
  record(message: any): void {
    if (!this.recording) {
      return;
    }

    const ts = Date.now() - this.startTime;
    this.messages.push({ ts, msg: message });
  }

  /**
   * Stop recording
   */
  stop(): void {
    this.recording = false;
    console.log(`[Recorder] Stopped recording for job ${this.jobId} (${this.tool}) - ${this.messages.length} messages`);
  }

  /**
   * Save recording to filesystem with gzip compression
   */
  async save(cacheKey: string, recordingsDir: string = './recordings'): Promise<string> {
    if (this.messages.length === 0) {
      throw new Error('No messages to save');
    }

    // Ensure recordings directory exists
    await fs.mkdir(recordingsDir, { recursive: true });

    const recording: Recording = {
      version: 1,
      duration: Date.now() - this.startTime,
      messages: this.messages,
      metadata: this.metadata
    };

    const json = JSON.stringify(recording, null, 2);
    const filePath = path.join(recordingsDir, `${cacheKey}.json.gz`);

    // Write gzipped JSON
    const gzip = createGzip({ level: 9 }); // Maximum compression
    await pipelineAsync(
      async function* () {
        yield Buffer.from(json, 'utf8');
      }(),
      gzip,
      fs.createWriteStream(filePath)
    );

    const stats = await fs.stat(filePath);
    console.log(`[Recorder] Saved recording to ${filePath} (${stats.size} bytes, ${this.messages.length} messages, ${recording.duration}ms duration)`);

    return filePath;
  }

  /**
   * Get message count
   */
  getMessageCount(): number {
    return this.messages.length;
  }

  /**
   * Get recording duration
   */
  getDuration(): number {
    return Date.now() - this.startTime;
  }

  /**
   * Check if currently recording
   */
  isRecording(): boolean {
    return this.recording;
  }
}

/**
 * Generate cache key from build parameters
 */
export function generateCacheKey(params: CacheKeyParams): string {
  // Normalize repository URL (remove .git suffix, convert to lowercase)
  const normalizedRepo = params.repositoryUrl
    .toLowerCase()
    .replace(/\.git$/, '')
    .replace(/^https?:\/\//, '')
    .replace(/\/$/, '');

  const input = JSON.stringify({
    repo: normalizedRepo,
    commit: params.commitHash || 'HEAD',
    tool: params.tool,
    claudeVersion: params.claudeVersion,
    dockerImage: params.dockerImage
  });

  return crypto.createHash('sha256').update(input).digest('hex');
}

/**
 * Check if a cache key exists and return the recording path
 */
export async function findCachedRecording(cacheKey: string, recordingsDir: string = './recordings'): Promise<string | null> {
  const filePath = path.join(recordingsDir, `${cacheKey}.json.gz`);

  try {
    await fs.access(filePath);
    return filePath;
  } catch {
    return null;
  }
}

/**
 * Load a recording from disk (uncompressed)
 */
export async function loadRecording(filePath: string): Promise<Recording> {
  const gunzip = promisify(require('zlib').gunzip);
  const compressed = await fs.readFile(filePath);
  const json = await gunzip(compressed);
  return JSON.parse(json.toString('utf8'));
}
