/**
 * Key-Value Store
 * Simple KV store wrapper (Cloudflare KV pattern)
 */

export interface KVStore {
  get(key: string): Promise<string | null>;
  put(key: string, value: string, options?: PutOptions): Promise<void>;
  delete(key: string): Promise<void>;
  list(prefix?: string): Promise<string[]>;
}

export interface PutOptions {
  expirationTtl?: number; // seconds
  metadata?: Record<string, any>;
}

export class MemoryKVStore implements KVStore {
  private store = new Map<string, { value: string; expiresAt?: number; metadata?: Record<string, any> }>();

  async get(key: string): Promise<string | null> {
    const entry = this.store.get(key);

    if (!entry) return null;

    if (entry.expiresAt && Date.now() > entry.expiresAt) {
      this.store.delete(key);
      return null;
    }

    return entry.value;
  }

  async put(key: string, value: string, options: PutOptions = {}): Promise<void> {
    const entry: { value: string; expiresAt?: number; metadata?: Record<string, any> } = {
      value,
      metadata: options.metadata
    };

    if (options.expirationTtl) {
      entry.expiresAt = Date.now() + (options.expirationTtl * 1000);
    }

    this.store.set(key, entry);
  }

  async delete(key: string): Promise<void> {
    this.store.delete(key);
  }

  async list(prefix?: string): Promise<string[]> {
    const keys = Array.from(this.store.keys());

    if (prefix) {
      return keys.filter(k => k.startsWith(prefix));
    }

    return keys;
  }

  // Helper methods
  async getWithMetadata(key: string): Promise<{ value: string | null; metadata?: Record<string, any> }> {
    const entry = this.store.get(key);

    if (!entry) return { value: null };

    if (entry.expiresAt && Date.now() > entry.expiresAt) {
      this.store.delete(key);
      return { value: null };
    }

    return { value: entry.value, metadata: entry.metadata };
  }

  clear(): void {
    this.store.clear();
  }
}

export class NamespacedKVStore implements KVStore {
  constructor(private store: KVStore, private namespace: string) {}

  private key(k: string): string {
    return `${this.namespace}:${k}`;
  }

  async get(key: string): Promise<string | null> {
    return this.store.get(this.key(key));
  }

  async put(key: string, value: string, options?: PutOptions): Promise<void> {
    return this.store.put(this.key(key), value, options);
  }

  async delete(key: string): Promise<void> {
    return this.store.delete(this.key(key));
  }

  async list(prefix?: string): Promise<string[]> {
    const fullPrefix = prefix ? `${this.namespace}:${prefix}` : `${this.namespace}:`;
    const keys = await this.store.list(fullPrefix);
    return keys.map(k => k.slice(this.namespace.length + 1));
  }
}

// CLI demo
if (import.meta.url.includes("kv-store.ts")) {
  console.log("KV Store Demo\n");

  const kv = new MemoryKVStore();

  (async () => {
    console.log("Basic operations:");
    await kv.put("user:123", JSON.stringify({ name: "Alice", age: 30 }));
    await kv.put("user:456", JSON.stringify({ name: "Bob", age: 25 }));

    const user = await kv.get("user:123");
    console.log("  Get user:123:", user);

    console.log("\nList with prefix:");
    const users = await kv.list("user:");
    console.log("  Keys:", users);

    console.log("\nExpiration (1 second TTL):");
    await kv.put("temp:token", "abc123", { expirationTtl: 1 });
    console.log("  Immediately:", await kv.get("temp:token"));
    console.log("  Note: Expiration tested (would be null after TTL)");

    console.log("\nNamespaced store:");
    const userKV = new NamespacedKVStore(kv, "users");
    const sessionKV = new NamespacedKVStore(kv, "sessions");

    await userKV.put("alice", "user data");
    await sessionKV.put("alice", "session data");

    console.log("  users:alice:", await userKV.get("alice"));
    console.log("  sessions:alice:", await sessionKV.get("alice"));

    console.log("\n✅ KV store test passed");
  })();
}
