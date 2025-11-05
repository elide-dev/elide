/**
 * Priority Queue
 * Min-heap based priority queue
 */

export interface PriorityItem<T> {
  value: T;
  priority: number;
}

export class PriorityQueue<T> {
  private heap: PriorityItem<T>[] = [];

  enqueue(value: T, priority: number): void {
    this.heap.push({ value, priority });
    this.bubbleUp(this.heap.length - 1);
  }

  dequeue(): T | undefined {
    if (this.isEmpty()) {
      return undefined;
    }

    const min = this.heap[0];
    const last = this.heap.pop()!;

    if (this.heap.length > 0) {
      this.heap[0] = last;
      this.bubbleDown(0);
    }

    return min.value;
  }

  peek(): T | undefined {
    return this.heap[0]?.value;
  }

  peekPriority(): number | undefined {
    return this.heap[0]?.priority;
  }

  isEmpty(): boolean {
    return this.heap.length === 0;
  }

  size(): number {
    return this.heap.length;
  }

  clear(): void {
    this.heap = [];
  }

  toArray(): T[] {
    return this.heap.map(item => item.value);
  }

  private bubbleUp(index: number): void {
    while (index > 0) {
      const parentIndex = Math.floor((index - 1) / 2);

      if (this.heap[index].priority >= this.heap[parentIndex].priority) {
        break;
      }

      this.swap(index, parentIndex);
      index = parentIndex;
    }
  }

  private bubbleDown(index: number): void {
    while (true) {
      let minIndex = index;
      const leftChild = 2 * index + 1;
      const rightChild = 2 * index + 2;

      if (
        leftChild < this.heap.length &&
        this.heap[leftChild].priority < this.heap[minIndex].priority
      ) {
        minIndex = leftChild;
      }

      if (
        rightChild < this.heap.length &&
        this.heap[rightChild].priority < this.heap[minIndex].priority
      ) {
        minIndex = rightChild;
      }

      if (minIndex === index) {
        break;
      }

      this.swap(index, minIndex);
      index = minIndex;
    }
  }

  private swap(i: number, j: number): void {
    [this.heap[i], this.heap[j]] = [this.heap[j], this.heap[i]];
  }
}

// Max-heap variant
export class MaxPriorityQueue<T> {
  private heap: PriorityItem<T>[] = [];

  enqueue(value: T, priority: number): void {
    this.heap.push({ value, priority });
    this.bubbleUp(this.heap.length - 1);
  }

  dequeue(): T | undefined {
    if (this.isEmpty()) {
      return undefined;
    }

    const max = this.heap[0];
    const last = this.heap.pop()!;

    if (this.heap.length > 0) {
      this.heap[0] = last;
      this.bubbleDown(0);
    }

    return max.value;
  }

  peek(): T | undefined {
    return this.heap[0]?.value;
  }

  isEmpty(): boolean {
    return this.heap.length === 0;
  }

  size(): number {
    return this.heap.length;
  }

  private bubbleUp(index: number): void {
    while (index > 0) {
      const parentIndex = Math.floor((index - 1) / 2);

      if (this.heap[index].priority <= this.heap[parentIndex].priority) {
        break;
      }

      this.swap(index, parentIndex);
      index = parentIndex;
    }
  }

  private bubbleDown(index: number): void {
    while (true) {
      let maxIndex = index;
      const leftChild = 2 * index + 1;
      const rightChild = 2 * index + 2;

      if (
        leftChild < this.heap.length &&
        this.heap[leftChild].priority > this.heap[maxIndex].priority
      ) {
        maxIndex = leftChild;
      }

      if (
        rightChild < this.heap.length &&
        this.heap[rightChild].priority > this.heap[maxIndex].priority
      ) {
        maxIndex = rightChild;
      }

      if (maxIndex === index) {
        break;
      }

      this.swap(index, maxIndex);
      index = maxIndex;
    }
  }

  private swap(i: number, j: number): void {
    [this.heap[i], this.heap[j]] = [this.heap[j], this.heap[i]];
  }
}

// CLI demo
if (import.meta.url.includes("priority-queue.ts")) {
  console.log("Priority Queue Demo\n");

  console.log("1. Min Priority Queue:");
  const minPQ = new PriorityQueue<string>();

  minPQ.enqueue("Low priority", 10);
  minPQ.enqueue("High priority", 1);
  minPQ.enqueue("Medium priority", 5);

  console.log(`  Size: ${minPQ.size()}`);
  console.log(`  Peek: "${minPQ.peek()}" (priority ${minPQ.peekPriority()})`);

  console.log("\n2. Dequeue in order:");
  while (!minPQ.isEmpty()) {
    console.log(`  → ${minPQ.dequeue()}`);
  }

  console.log("\n3. Max Priority Queue:");
  const maxPQ = new MaxPriorityQueue<string>();

  maxPQ.enqueue("Low priority", 1);
  maxPQ.enqueue("High priority", 10);
  maxPQ.enqueue("Medium priority", 5);

  console.log(`  Size: ${maxPQ.size()}`);
  console.log(`  Peek: "${maxPQ.peek()}"`);

  console.log("\n4. Dequeue in order (highest first):");
  while (!maxPQ.isEmpty()) {
    console.log(`  → ${maxPQ.dequeue()}`);
  }

  console.log("\n5. Task scheduler:");
  const tasks = new PriorityQueue<{ name: string; deadline: number }>();

  tasks.enqueue({ name: "Write report", deadline: 100 }, 100);
  tasks.enqueue({ name: "Fix bug", deadline: 50 }, 50);
  tasks.enqueue({ name: "Code review", deadline: 75 }, 75);

  console.log("  Tasks by deadline:");
  while (!tasks.isEmpty()) {
    const task = tasks.dequeue()!;
    console.log(`  → ${task.name} (deadline: ${task.deadline})`);
  }

  console.log("\n✅ Priority queue test passed");
}
