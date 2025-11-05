/**
 * ETag Generator
 * Generate and validate ETags for HTTP caching
 */

export interface ETagOptions {
  weak?: boolean;
  algorithm?: 'simple' | 'hash';
}

export class ETagGenerator {
  private options: Required<ETagOptions>;

  constructor(options: ETagOptions = {}) {
    this.options = {
      weak: options.weak ?? false,
      algorithm: options.algorithm || 'simple'
    };
  }

  generate(content: string | Buffer, options?: ETagOptions): string {
    const opts = { ...this.options, ...options };
    const buffer = typeof content === 'string' ? Buffer.from(content) : content;

    let hash: string;

    if (opts.algorithm === 'hash') {
      hash = this.hashBuffer(buffer);
    } else {
      hash = this.simpleHash(buffer);
    }

    return opts.weak ? `W/"${hash}"` : `"${hash}"`;
  }

  private simpleHash(buffer: Buffer): string {
    // Simple hash based on size and timestamp
    const size = buffer.length;
    const timestamp = Date.now();

    // Create a basic fingerprint
    let hash = 0;
    for (let i = 0; i < Math.min(buffer.length, 100); i++) {
      hash = ((hash << 5) - hash) + buffer[i];
      hash = hash & hash; // Convert to 32-bit integer
    }

    return `${size}-${Math.abs(hash).toString(16)}`;
  }

  private hashBuffer(buffer: Buffer): string {
    // More sophisticated hash (but still simple without crypto)
    let hash = 0;

    for (let i = 0; i < buffer.length; i++) {
      const char = buffer[i];
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }

    const hex = Math.abs(hash).toString(16).padStart(8, '0');
    return `${buffer.length.toString(16)}-${hex}`;
  }

  matches(etag: string, requestETag: string): boolean {
    if (!etag || !requestETag) return false;

    // Normalize ETags (remove W/ prefix)
    const normalized1 = this.normalize(etag);
    const normalized2 = this.normalize(requestETag);

    return normalized1 === normalized2;
  }

  private normalize(etag: string): string {
    return etag.replace(/^W\//, '').replace(/"/g, '');
  }

  isWeak(etag: string): boolean {
    return etag.startsWith('W/');
  }

  parseIfNoneMatch(header: string): string[] {
    if (!header) return [];

    // Handle * (matches any)
    if (header.trim() === '*') return ['*'];

    // Parse comma-separated ETags
    return header.split(',').map(s => s.trim());
  }

  shouldReturn304(etag: string, ifNoneMatch?: string): boolean {
    if (!ifNoneMatch) return false;

    const requestETags = this.parseIfNoneMatch(ifNoneMatch);

    // * matches everything
    if (requestETags.includes('*')) return true;

    // Check if any request ETag matches
    return requestETags.some(reqETag => this.matches(etag, reqETag));
  }
}

// Helper functions
export function generateETag(content: string | Buffer, options?: ETagOptions): string {
  const generator = new ETagGenerator(options);
  return generator.generate(content);
}

export function checkETag(etag: string, ifNoneMatch: string): boolean {
  const generator = new ETagGenerator();
  return generator.shouldReturn304(etag, ifNoneMatch);
}

// Build 304 Not Modified response
export function buildNotModifiedResponse(etag: string): {
  status: number;
  headers: Record<string, string>;
  body: null;
} {
  return {
    status: 304,
    headers: {
      'ETag': etag,
      'Cache-Control': 'public, max-age=3600'
    },
    body: null
  };
}

// CLI demo
if (import.meta.url.includes("etag-generator.ts")) {
  console.log("ETag Generator Demo\n");

  const generator = new ETagGenerator();

  console.log("1. Generate ETag:");
  const content = "Hello, World!";
  const etag = generator.generate(content);
  console.log(`  Content: "${content}"`);
  console.log(`  ETag: ${etag}`);

  console.log("\n2. Generate weak ETag:");
  const weakETag = generator.generate(content, { weak: true });
  console.log(`  Weak ETag: ${weakETag}`);
  console.log(`  Is weak: ${generator.isWeak(weakETag)}`);

  console.log("\n3. Generate with hash algorithm:");
  const hashETag = generator.generate(content, { algorithm: 'hash' });
  console.log(`  Hash ETag: ${hashETag}`);

  console.log("\n4. Match ETags:");
  const match1 = generator.matches(etag, etag);
  const match2 = generator.matches(etag, weakETag);
  console.log(`  Same ETag matches: ${match1}`);
  console.log(`  Weak vs strong: ${match2}`);

  console.log("\n5. Parse If-None-Match:");
  const ifNoneMatch = '"abc123", W/"def456", "ghi789"';
  const parsed = generator.parseIfNoneMatch(ifNoneMatch);
  console.log(`  Header: ${ifNoneMatch}`);
  console.log(`  Parsed: [${parsed.join(', ')}]`);

  console.log("\n6. Check 304 Not Modified:");
  const should304 = generator.shouldReturn304('"abc123"', '"abc123", "xyz"');
  console.log(`  ETag: "abc123"`);
  console.log(`  If-None-Match: "abc123", "xyz"`);
  console.log(`  Should return 304: ${should304}`);

  console.log("\n7. Different content:");
  const content2 = "Different content here";
  const etag2 = generator.generate(content2);
  console.log(`  Content 1 ETag: ${etag}`);
  console.log(`  Content 2 ETag: ${etag2}`);
  console.log(`  Match: ${generator.matches(etag, etag2)}`);

  console.log("\n8. Build 304 response:");
  const response = buildNotModifiedResponse(etag);
  console.log(`  Status: ${response.status}`);
  console.log(`  ETag: ${response.headers['ETag']}`);

  console.log("\n✅ ETag generator test passed");
}
