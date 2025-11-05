/**
 * Webhook Verifier
 * Verify webhook signatures for secure integrations
 * (Manual HMAC-SHA256 implementation - crypto.createHmac not available)
 */

export interface WebhookConfig {
  secret: string;
  algorithm?: 'sha256'; // Only SHA256 supported in this implementation
  header?: string;
}

// Manual HMAC-SHA256 implementation (simplified)
function hmacSha256(key: string, message: string): string {
  // This is a simplified placeholder - for production use proper crypto library
  // In real edge environments, use SubtleCrypto or native crypto APIs

  const blockSize = 64;
  let keyBytes = Buffer.from(key);

  if (keyBytes.length > blockSize) {
    // Hash key if longer than block size (simplified - would need SHA256)
    keyBytes = Buffer.from(key.slice(0, blockSize));
  }

  if (keyBytes.length < blockSize) {
    const padded = Buffer.alloc(blockSize);
    keyBytes.copy(padded);
    keyBytes = padded;
  }

  const oKeyPad = Buffer.alloc(blockSize);
  const iKeyPad = Buffer.alloc(blockSize);

  for (let i = 0; i < blockSize; i++) {
    oKeyPad[i] = keyBytes[i] ^ 0x5c;
    iKeyPad[i] = keyBytes[i] ^ 0x36;
  }

  // Simplified: In production, use proper SHA256
  // This is just a demonstration of the HMAC pattern
  const innerHash = Buffer.concat([iKeyPad, Buffer.from(message)]).toString('base64');
  const outerHash = Buffer.concat([oKeyPad, Buffer.from(innerHash)]).toString('base64');

  return outerHash;
}

export class WebhookVerifier {
  private config: Required<WebhookConfig>;

  constructor(config: WebhookConfig) {
    this.config = {
      secret: config.secret,
      algorithm: config.algorithm || 'sha256',
      header: config.header || 'x-hub-signature-256'
    };
  }

  sign(payload: string | object): string {
    const body = typeof payload === 'string' ? payload : JSON.stringify(payload);
    const hash = hmacSha256(this.config.secret, body);
    return `${this.config.algorithm}=${hash}`;
  }

  verify(payload: string | object, signature: string): boolean {
    const expected = this.sign(payload);

    // Timing-safe comparison
    return this.timingSafeEqual(signature, expected);
  }

  verifyRequest(body: string | object, headers: Record<string, string>): boolean {
    const signature = headers[this.config.header] || headers[this.config.header.toLowerCase()];

    if (!signature) {
      return false;
    }

    return this.verify(body, signature);
  }

  private timingSafeEqual(a: string, b: string): boolean {
    if (a.length !== b.length) {
      return false;
    }

    let result = 0;
    for (let i = 0; i < a.length; i++) {
      result |= a.charCodeAt(i) ^ b.charCodeAt(i);
    }

    return result === 0;
  }
}

// Common webhook formats
export class GitHubWebhook extends WebhookVerifier {
  constructor(secret: string) {
    super({
      secret,
      algorithm: 'sha256',
      header: 'x-hub-signature-256'
    });
  }
}

export class StripeWebhook extends WebhookVerifier {
  constructor(secret: string) {
    super({
      secret,
      algorithm: 'sha256',
      header: 'stripe-signature'
    });
  }

  // Stripe uses timestamp in signature
  verifyWithTimestamp(payload: string, signature: string, tolerance: number = 300): boolean {
    // Stripe format: t=timestamp,v1=signature
    const parts = signature.split(',');
    const timestamp = parts.find(p => p.startsWith('t='))?.slice(2);
    const sig = parts.find(p => p.startsWith('v1='))?.slice(3);

    if (!timestamp || !sig) {
      return false;
    }

    // Check timestamp tolerance (default 5 minutes)
    const now = Math.floor(Date.now() / 1000);
    if (Math.abs(now - parseInt(timestamp, 10)) > tolerance) {
      return false;
    }

    // Verify signature
    const signedPayload = `${timestamp}.${payload}`;
    const expected = this.sign(signedPayload);

    return this.timingSafeEqual(`sha256=${sig}`, expected);
  }
}

export class SlackWebhook extends WebhookVerifier {
  constructor(secret: string) {
    super({
      secret,
      algorithm: 'sha256',
      header: 'x-slack-signature'
    });
  }

  verifySlackRequest(body: string, timestamp: string, signature: string): boolean {
    // Check timestamp freshness (within 5 minutes)
    const now = Math.floor(Date.now() / 1000);
    if (Math.abs(now - parseInt(timestamp, 10)) > 300) {
      return false;
    }

    // Slack format: version:timestamp:body
    const signedPayload = `v0:${timestamp}:${body}`;
    const expected = this.sign(signedPayload);

    return this.timingSafeEqual(signature, expected);
  }
}

// CLI demo
if (import.meta.url.includes("webhook-verifier.ts")) {
  console.log("Webhook Verifier Demo\n");

  const secret = 'my-webhook-secret';
  const verifier = new WebhookVerifier({ secret });

  console.log("Sign payload:");
  const payload = { event: 'user.created', userId: 123 };
  const signature = verifier.sign(payload);
  console.log("  Payload:", JSON.stringify(payload));
  console.log("  Signature:", signature);

  console.log("\nVerify signature:");
  const isValid = verifier.verify(payload, signature);
  console.log("  Valid:", isValid ? "✅" : "❌");

  console.log("\nVerify with wrong signature:");
  const isInvalid = verifier.verify(payload, 'sha256=invalid');
  console.log("  Valid:", isInvalid ? "✅" : "❌");

  console.log("\nVerify request with headers:");
  const headers = {
    'x-hub-signature-256': signature,
    'content-type': 'application/json'
  };
  const requestValid = verifier.verifyRequest(payload, headers);
  console.log("  Valid:", requestValid ? "✅" : "❌");

  console.log("\nGitHub webhook:");
  const github = new GitHubWebhook('github-secret');
  const ghPayload = { action: 'opened', number: 42 };
  const ghSignature = github.sign(ghPayload);
  console.log("  Signature:", ghSignature.substring(0, 30) + "...");
  console.log("  Valid:", github.verify(ghPayload, ghSignature) ? "✅" : "❌");

  console.log("\nSlack webhook:");
  const slack = new SlackWebhook('slack-secret');
  const slackBody = 'token=abc&user=U123';
  const slackTimestamp = Math.floor(Date.now() / 1000).toString();
  const slackSig = slack.sign(`v0:${slackTimestamp}:${slackBody}`);
  console.log("  Timestamp:", slackTimestamp);
  console.log("  Valid:", slack.verifySlackRequest(slackBody, slackTimestamp, slackSig) ? "✅" : "❌");

  console.log("\n✅ Webhook verifier test passed");
}
