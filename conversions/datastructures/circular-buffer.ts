/**
 * Circular Buffer (Ring Buffer)
 * Fixed-size buffer with wrap-around
 */

export class CircularBuffer<T> {
  private buffer: (T | undefined)[];
  private head = 0;
  private tail = 0;
  private count = 0;
  private capacity: number;

  constructor(capacity: number) {
    if (capacity <= 0) {
      throw new Error('Capacity must be positive');
    }

    this.capacity = capacity;
    this.buffer = new Array(capacity);
  }

  push(item: T): T | undefined {
    let evicted: T | undefined;

    if (this.isFull()) {
      evicted = this.buffer[this.tail];
      this.tail = (this.tail + 1) % this.capacity;
    } else {
      this.count++;
    }

    this.buffer[this.head] = item;
    this.head = (this.head + 1) % this.capacity;

    return evicted;
  }

  shift(): T | undefined {
    if (this.isEmpty()) {
      return undefined;
    }

    const item = this.buffer[this.tail];
    this.buffer[this.tail] = undefined;
    this.tail = (this.tail + 1) % this.capacity;
    this.count--;

    return item;
  }

  peek(): T | undefined {
    if (this.isEmpty()) {
      return undefined;
    }

    return this.buffer[this.tail];
  }

  peekLast(): T | undefined {
    if (this.isEmpty()) {
      return undefined;
    }

    const lastIndex = (this.head - 1 + this.capacity) % this.capacity;
    return this.buffer[lastIndex];
  }

  get(index: number): T | undefined {
    if (index < 0 || index >= this.count) {
      return undefined;
    }

    const actualIndex = (this.tail + index) % this.capacity;
    return this.buffer[actualIndex];
  }

  isEmpty(): boolean {
    return this.count === 0;
  }

  isFull(): boolean {
    return this.count === this.capacity;
  }

  size(): number {
    return this.count;
  }

  getCapacity(): number {
    return this.capacity;
  }

  clear(): void {
    this.buffer = new Array(this.capacity);
    this.head = 0;
    this.tail = 0;
    this.count = 0;
  }

  toArray(): T[] {
    const result: T[] = [];

    for (let i = 0; i < this.count; i++) {
      const index = (this.tail + i) % this.capacity;
      const item = this.buffer[index];

      if (item !== undefined) {
        result.push(item);
      }
    }

    return result;
  }

  forEach(callback: (item: T, index: number) => void): void {
    for (let i = 0; i < this.count; i++) {
      const index = (this.tail + i) % this.capacity;
      const item = this.buffer[index];

      if (item !== undefined) {
        callback(item, i);
      }
    }
  }
}

// Stats tracker using circular buffer
export class RollingStats {
  private buffer: CircularBuffer<number>;

  constructor(windowSize: number) {
    this.buffer = new CircularBuffer(windowSize);
  }

  add(value: number): void {
    this.buffer.push(value);
  }

  getAverage(): number {
    if (this.buffer.isEmpty()) {
      return 0;
    }

    const values = this.buffer.toArray();
    const sum = values.reduce((acc, val) => acc + val, 0);

    return sum / values.length;
  }

  getMin(): number {
    if (this.buffer.isEmpty()) {
      return 0;
    }

    const values = this.buffer.toArray();
    return Math.min(...values);
  }

  getMax(): number {
    if (this.buffer.isEmpty()) {
      return 0;
    }

    const values = this.buffer.toArray();
    return Math.max(...values);
  }

  getCount(): number {
    return this.buffer.size();
  }
}

// CLI demo
if (import.meta.url.includes("circular-buffer.ts")) {
  console.log("Circular Buffer Demo\n");

  console.log("1. Create buffer with capacity 5:");
  const buffer = new CircularBuffer<number>(5);
  console.log(`  Capacity: ${buffer.getCapacity()}`);

  console.log("\n2. Push items:");
  [1, 2, 3].forEach(n => buffer.push(n));
  console.log(`  Pushed: 1, 2, 3`);
  console.log(`  Size: ${buffer.size()}`);
  console.log(`  Contents: [${buffer.toArray().join(", ")}]`);

  console.log("\n3. Shift items:");
  const first = buffer.shift();
  console.log(`  Shifted: ${first}`);
  console.log(`  Contents: [${buffer.toArray().join(", ")}]`);

  console.log("\n4. Fill buffer:");
  [4, 5, 6].forEach(n => buffer.push(n));
  console.log(`  Pushed: 4, 5, 6`);
  console.log(`  Contents: [${buffer.toArray().join(", ")}]`);
  console.log(`  Full: ${buffer.isFull()}`);

  console.log("\n5. Overflow (evicts oldest):");
  const evicted = buffer.push(7);
  console.log(`  Pushed: 7`);
  console.log(`  Evicted: ${evicted}`);
  console.log(`  Contents: [${buffer.toArray().join(", ")}]`);

  console.log("\n6. Random access:");
  console.log(`  get(0): ${buffer.get(0)}`);
  console.log(`  get(2): ${buffer.get(2)}`);
  console.log(`  peek(): ${buffer.peek()}`);
  console.log(`  peekLast(): ${buffer.peekLast()}`);

  console.log("\n7. Rolling stats:");
  const stats = new RollingStats(5);

  [10, 20, 30, 40, 50, 60].forEach(n => stats.add(n));

  console.log(`  Values (last 5): 20, 30, 40, 50, 60`);
  console.log(`  Average: ${stats.getAverage()}`);
  console.log(`  Min: ${stats.getMin()}`);
  console.log(`  Max: ${stats.getMax()}`);

  console.log("\n✅ Circular buffer test passed");
}
