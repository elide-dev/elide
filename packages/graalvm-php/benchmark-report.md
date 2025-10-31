# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 7.21 ms
- Median: 6.71 ms
- Std Dev: 1.60 ms
- Min: 5.51 ms
- Max: 12.21 ms
- Throughput: 138.74 ops/sec

**Memory:**
- Before: 23.30 MB
- After: 51.06 MB
- Delta: 27.75 MB

### array_sequential

**Performance:**
- Mean: 2.11 ms
- Median: 1.88 ms
- Std Dev: 811.06 μs
- Min: 1.25 ms
- Max: 4.53 ms
- Throughput: 474.51 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 29.59 MB
- Delta: 6.41 MB

### property_access

**Performance:**
- Mean: 2.43 ms
- Median: 2.12 ms
- Std Dev: 886.95 μs
- Min: 1.26 ms
- Max: 4.67 ms
- Throughput: 410.72 ops/sec

**Memory:**
- Before: 23.13 MB
- After: 30.11 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 1.69 ms
- Median: 1.58 ms
- Std Dev: 828.04 μs
- Min: 856.71 μs
- Max: 4.10 ms
- Throughput: 591.75 ops/sec

**Memory:**
- Before: 23.15 MB
- After: 36.63 MB
- Delta: 13.48 MB

### arithmetic

**Performance:**
- Mean: 2.81 ms
- Median: 2.40 ms
- Std Dev: 1.28 ms
- Min: 1.79 ms
- Max: 7.50 ms
- Throughput: 355.90 ops/sec

**Memory:**
- Before: 23.16 MB
- After: 26.79 MB
- Delta: 3.63 MB

### method_calls

**Performance:**
- Mean: 2.08 ms
- Median: 1.81 ms
- Std Dev: 1.10 ms
- Min: 1.45 ms
- Max: 6.68 ms
- Throughput: 481.38 ops/sec

**Memory:**
- Before: 23.16 MB
- After: 49.65 MB
- Delta: 26.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 7ms | 7ms | 2ms | 6ms | 12ms | 138.74 ops/s |
 array_sequential | 2ms | 2ms | 811μs | 1ms | 5ms | 474.51 ops/s |
 property_access | 2ms | 2ms | 887μs | 1ms | 5ms | 410.72 ops/s |
 string_concat | 2ms | 2ms | 828μs | 857μs | 4ms | 591.75 ops/s |
 arithmetic | 3ms | 2ms | 1ms | 2ms | 7ms | 355.90 ops/s |
 method_calls | 2ms | 2ms | 1ms | 1ms | 7ms | 481.38 ops/s |
