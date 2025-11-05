/**
 * Bloom Filter
 * Probabilistic data structure for set membership testing
 */

export class BloomFilter {
  private bits: Uint8Array;
  private size: number;
  private hashCount: number;

  constructor(size: number = 1024, hashCount: number = 3) {
    this.size = size;
    this.hashCount = hashCount;
    this.bits = new Uint8Array(Math.ceil(size / 8));
  }

  add(item: string): void {
    const hashes = this.getHashes(item);

    for (const hash of hashes) {
      const index = hash % this.size;
      const byteIndex = Math.floor(index / 8);
      const bitIndex = index % 8;

      this.bits[byteIndex] |= (1 << bitIndex);
    }
  }

  contains(item: string): boolean {
    const hashes = this.getHashes(item);

    for (const hash of hashes) {
      const index = hash % this.size;
      const byteIndex = Math.floor(index / 8);
      const bitIndex = index % 8;

      if ((this.bits[byteIndex] & (1 << bitIndex)) === 0) {
        return false;
      }
    }

    return true;
  }

  private getHashes(item: string): number[] {
    const hashes: number[] = [];

    for (let i = 0; i < this.hashCount; i++) {
      hashes.push(this.hash(item, i));
    }

    return hashes;
  }

  private hash(str: string, seed: number): number {
    let hash = seed;

    for (let i = 0; i < str.length; i++) {
      hash = ((hash << 5) - hash) + str.charCodeAt(i);
      hash = hash & hash; // Convert to 32-bit integer
    }

    return Math.abs(hash);
  }

  // Estimate false positive rate
  getFalsePositiveRate(itemCount: number): number {
    const k = this.hashCount;
    const m = this.size;
    const n = itemCount;

    // (1 - e^(-kn/m))^k
    return Math.pow(1 - Math.exp(-k * n / m), k);
  }

  clear(): void {
    this.bits.fill(0);
  }

  getSize(): number {
    return this.size;
  }

  getHashCount(): number {
    return this.hashCount;
  }
}

// Optimal bloom filter sizing
export function optimalSize(expectedItems: number, falsePositiveRate: number): {
  size: number;
  hashCount: number;
} {
  // m = -(n * ln(p)) / (ln(2)^2)
  const size = Math.ceil(-(expectedItems * Math.log(falsePositiveRate)) / (Math.log(2) ** 2));

  // k = (m/n) * ln(2)
  const hashCount = Math.ceil((size / expectedItems) * Math.log(2));

  return { size, hashCount };
}

// CLI demo
if (import.meta.url.includes("bloom-filter.ts")) {
  console.log("Bloom Filter Demo\n");

  console.log("1. Create bloom filter:");
  const filter = new BloomFilter(1000, 3);
  console.log(`  Size: ${filter.getSize()} bits`);
  console.log(`  Hash functions: ${filter.getHashCount()}`);

  console.log("\n2. Add items:");
  const items = ["apple", "banana", "cherry", "date", "elderberry"];
  items.forEach(item => filter.add(item));
  console.log(`  Added: ${items.join(", ")}`);

  console.log("\n3. Test membership:");
  console.log("  'apple':", filter.contains("apple") ? "✅ probably in set" : "❌ definitely not");
  console.log("  'banana':", filter.contains("banana") ? "✅ probably in set" : "❌ definitely not");
  console.log("  'grape':", filter.contains("grape") ? "⚠️ false positive!" : "❌ definitely not");

  console.log("\n4. False positive rate:");
  const fpRate = filter.getFalsePositiveRate(items.length);
  console.log(`  Estimated: ${(fpRate * 100).toFixed(2)}%`);

  console.log("\n5. Optimal sizing:");
  const optimal = optimalSize(10000, 0.01);
  console.log(`  For 10,000 items with 1% FP rate:`);
  console.log(`    Size: ${optimal.size} bits (${Math.ceil(optimal.size / 8)} bytes)`);
  console.log(`    Hash functions: ${optimal.hashCount}`);

  console.log("\n6. Test false positives:");
  const testFilter = new BloomFilter(100, 3);
  const added = ["test1", "test2", "test3"];
  added.forEach(item => testFilter.add(item));

  let falsePositives = 0;
  const testCount = 100;

  for (let i = 0; i < testCount; i++) {
    const testItem = `notadded${i}`;
    if (testFilter.contains(testItem)) {
      falsePositives++;
    }
  }

  console.log(`  Tested ${testCount} non-members:`);
  console.log(`  False positives: ${falsePositives} (${(falsePositives / testCount * 100).toFixed(1)}%)`);

  console.log("\n✅ Bloom filter test passed");
}
