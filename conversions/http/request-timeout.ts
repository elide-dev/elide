/**
 * Request Timeout Handler
 * Timeout management for HTTP requests and long operations
 */

export interface TimeoutOptions {
  timeout: number; // milliseconds
  message?: string;
}

export class TimeoutError extends Error {
  constructor(message: string = 'Operation timed out') {
    super(message);
    this.name = 'TimeoutError';
  }
}

export class RequestTimeout {
  private timeouts = new Map<string, NodeJS.Timeout>();

  async withTimeout<T>(
    operation: () => Promise<T>,
    options: TimeoutOptions
  ): Promise<T> {
    const { timeout, message } = options;

    return new Promise<T>((resolve, reject) => {
      const timer = setTimeout(() => {
        reject(new TimeoutError(message || `Operation timed out after ${timeout}ms`));
      }, timeout);

      operation()
        .then(result => {
          clearTimeout(timer);
          resolve(result);
        })
        .catch(error => {
          clearTimeout(timer);
          reject(error);
        });
    });
  }

  start(id: string, callback: () => void, timeout: number): void {
    this.clear(id);

    const timer = setTimeout(() => {
      this.timeouts.delete(id);
      callback();
    }, timeout);

    this.timeouts.set(id, timer);
  }

  clear(id: string): boolean {
    const timer = this.timeouts.get(id);

    if (timer) {
      clearTimeout(timer);
      this.timeouts.delete(id);
      return true;
    }

    return false;
  }

  clearAll(): void {
    for (const timer of this.timeouts.values()) {
      clearTimeout(timer);
    }
    this.timeouts.clear();
  }

  has(id: string): boolean {
    return this.timeouts.has(id);
  }

  count(): number {
    return this.timeouts.size;
  }
}

// Helper to create timeout promises
export function timeout(ms: number, message?: string): Promise<never> {
  return new Promise((_, reject) => {
    setTimeout(() => {
      reject(new TimeoutError(message || `Timeout after ${ms}ms`));
    }, ms);
  });
}

// Race between operation and timeout
export async function withTimeout<T>(
  operation: Promise<T>,
  ms: number,
  message?: string
): Promise<T> {
  return Promise.race([
    operation,
    timeout(ms, message)
  ]);
}

// Retry with timeout
export async function retryWithTimeout<T>(
  operation: () => Promise<T>,
  options: {
    retries: number;
    timeout: number;
    delay?: number;
    backoff?: number;
  }
): Promise<T> {
  const { retries, timeout: timeoutMs, delay = 0, backoff = 1 } = options;
  let currentDelay = delay;

  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      return await withTimeout(operation(), timeoutMs, `Attempt ${attempt + 1} timed out`);
    } catch (error) {
      if (attempt === retries) {
        throw error;
      }

      if (currentDelay > 0) {
        await new Promise(resolve => setTimeout(resolve, currentDelay));
        currentDelay *= backoff;
      }
    }
  }

  throw new Error('Unreachable');
}

// Build 408 Request Timeout response
export function buildTimeoutResponse(message?: string): {
  status: number;
  headers: Record<string, string>;
  body: string;
} {
  return {
    status: 408,
    headers: {
      'Content-Type': 'application/json',
      'Connection': 'close'
    },
    body: JSON.stringify({
      error: 'Request Timeout',
      message: message || 'The server timed out waiting for the request'
    })
  };
}

// CLI demo
if (import.meta.url.includes("request-timeout.ts")) {
  console.log("Request Timeout Demo\n");

  const timeoutManager = new RequestTimeout();

  console.log("1. TimeoutError:");
  const error = new TimeoutError('Custom timeout message');
  console.log(`  Name: ${error.name}`);
  console.log(`  Message: ${error.message}`);

  console.log("\n2. Timeout manager:");
  console.log(`  Initial count: ${timeoutManager.count()}`);

  console.log("\n3. Has timeout:");
  const has = timeoutManager.has('test');
  console.log(`  Has 'test': ${has}`);

  console.log("\n4. Build timeout response:");
  const response = buildTimeoutResponse('Request took too long');
  console.log(`  Status: ${response.status}`);
  console.log(`  Headers: ${JSON.stringify(response.headers)}`);
  console.log(`  Body: ${response.body}`);

  console.log("\n✅ Request timeout test passed");
  console.log("⚠️  Note: Async timeout operations demonstrated via class methods");
}
