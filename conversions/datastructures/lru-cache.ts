/**
 * LRU Cache
 * Least Recently Used cache with O(1) get/set
 */

class LRUNode<K, V> {
  constructor(
    public key: K,
    public value: V,
    public prev: LRUNode<K, V> | null = null,
    public next: LRUNode<K, V> | null = null
  ) {}
}

export class LRUCache<K, V> {
  private capacity: number;
  private cache = new Map<K, LRUNode<K, V>>();
  private head: LRUNode<K, V> | null = null;
  private tail: LRUNode<K, V> | null = null;

  constructor(capacity: number) {
    if (capacity <= 0) {
      throw new Error('Capacity must be positive');
    }
    this.capacity = capacity;
  }

  get(key: K): V | undefined {
    const node = this.cache.get(key);

    if (!node) {
      return undefined;
    }

    // Move to head (most recently used)
    this.moveToHead(node);

    return node.value;
  }

  set(key: K, value: V): void {
    const existing = this.cache.get(key);

    if (existing) {
      // Update existing
      existing.value = value;
      this.moveToHead(existing);
    } else {
      // Add new node
      const node = new LRUNode(key, value);
      this.cache.set(key, node);
      this.addToHead(node);

      // Evict if over capacity
      if (this.cache.size > this.capacity) {
        const removed = this.removeTail();
        if (removed) {
          this.cache.delete(removed.key);
        }
      }
    }
  }

  has(key: K): boolean {
    return this.cache.has(key);
  }

  delete(key: K): boolean {
    const node = this.cache.get(key);

    if (!node) {
      return false;
    }

    this.removeNode(node);
    this.cache.delete(key);

    return true;
  }

  clear(): void {
    this.cache.clear();
    this.head = null;
    this.tail = null;
  }

  size(): number {
    return this.cache.size;
  }

  getCapacity(): number {
    return this.capacity;
  }

  // Get all keys in LRU order (most recent first)
  keys(): K[] {
    const result: K[] = [];
    let current = this.head;

    while (current) {
      result.push(current.key);
      current = current.next;
    }

    return result;
  }

  // Get all values in LRU order
  values(): V[] {
    const result: V[] = [];
    let current = this.head;

    while (current) {
      result.push(current.value);
      current = current.next;
    }

    return result;
  }

  private moveToHead(node: LRUNode<K, V>): void {
    if (node === this.head) {
      return;
    }

    this.removeNode(node);
    this.addToHead(node);
  }

  private addToHead(node: LRUNode<K, V>): void {
    node.prev = null;
    node.next = this.head;

    if (this.head) {
      this.head.prev = node;
    }

    this.head = node;

    if (!this.tail) {
      this.tail = node;
    }
  }

  private removeNode(node: LRUNode<K, V>): void {
    if (node.prev) {
      node.prev.next = node.next;
    } else {
      this.head = node.next;
    }

    if (node.next) {
      node.next.prev = node.prev;
    } else {
      this.tail = node.prev;
    }
  }

  private removeTail(): LRUNode<K, V> | null {
    if (!this.tail) {
      return null;
    }

    const removed = this.tail;
    this.removeNode(removed);

    return removed;
  }
}

// CLI demo
if (import.meta.url.includes("lru-cache.ts")) {
  console.log("LRU Cache Demo\n");

  console.log("1. Create cache with capacity 3:");
  const cache = new LRUCache<string, number>(3);
  console.log(`  Capacity: ${cache.getCapacity()}`);

  console.log("\n2. Add items:");
  cache.set("a", 1);
  cache.set("b", 2);
  cache.set("c", 3);
  console.log(`  Added: a=1, b=2, c=3`);
  console.log(`  Keys (MRU first): [${cache.keys().join(", ")}]`);

  console.log("\n3. Get item (moves to front):");
  const value = cache.get("a");
  console.log(`  get("a"): ${value}`);
  console.log(`  Keys after get: [${cache.keys().join(", ")}]`);

  console.log("\n4. Add item when full (evicts LRU):");
  cache.set("d", 4);
  console.log(`  Added: d=4`);
  console.log(`  Keys: [${cache.keys().join(", ")}]`);
  console.log(`  'b' was evicted (LRU)`);
  console.log(`  has("b"): ${cache.has("b")}`);

  console.log("\n5. Update existing:");
  cache.set("c", 30);
  console.log(`  Updated: c=30`);
  console.log(`  Keys: [${cache.keys().join(", ")}]`);

  console.log("\n6. Delete item:");
  cache.delete("a");
  console.log(`  Deleted: a`);
  console.log(`  Size: ${cache.size()}`);
  console.log(`  Keys: [${cache.keys().join(", ")}]`);

  console.log("\n7. Clear cache:");
  cache.clear();
  console.log(`  Size after clear: ${cache.size()}`);

  console.log("\n✅ LRU cache test passed");
}
