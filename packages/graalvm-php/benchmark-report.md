# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 7.29 ms
- Median: 7.12 ms
- Std Dev: 1.81 ms
- Min: 4.77 ms
- Max: 12.28 ms
- Throughput: 137.25 ops/sec

**Memory:**
- Before: 23.34 MB
- After: 49.05 MB
- Delta: 25.71 MB

### array_sequential

**Performance:**
- Mean: 2.01 ms
- Median: 1.66 ms
- Std Dev: 1.01 ms
- Min: 1.14 ms
- Max: 5.39 ms
- Throughput: 496.57 ops/sec

**Memory:**
- Before: 23.39 MB
- After: 29.63 MB
- Delta: 6.24 MB

### property_access

**Performance:**
- Mean: 2.62 ms
- Median: 2.37 ms
- Std Dev: 889.69 μs
- Min: 1.53 ms
- Max: 4.94 ms
- Throughput: 381.70 ops/sec

**Memory:**
- Before: 23.17 MB
- After: 30.15 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 1.79 ms
- Median: 1.55 ms
- Std Dev: 782.21 μs
- Min: 873.53 μs
- Max: 4.02 ms
- Throughput: 557.22 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 36.67 MB
- Delta: 13.48 MB

### arithmetic

**Performance:**
- Mean: 2.65 ms
- Median: 2.41 ms
- Std Dev: 972.91 μs
- Min: 1.85 ms
- Max: 6.19 ms
- Throughput: 377.62 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 27.81 MB
- Delta: 4.62 MB

### method_calls

**Performance:**
- Mean: 2.18 ms
- Median: 1.95 ms
- Std Dev: 788.55 μs
- Min: 1.67 ms
- Max: 5.34 ms
- Throughput: 459.09 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 50.68 MB
- Delta: 27.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 7ms | 7ms | 2ms | 5ms | 12ms | 137.25 ops/s |
 array_sequential | 2ms | 2ms | 1ms | 1ms | 5ms | 496.57 ops/s |
 property_access | 3ms | 2ms | 890μs | 2ms | 5ms | 381.70 ops/s |
 string_concat | 2ms | 2ms | 782μs | 874μs | 4ms | 557.22 ops/s |
 arithmetic | 3ms | 2ms | 973μs | 2ms | 6ms | 377.62 ops/s |
 method_calls | 2ms | 2ms | 789μs | 2ms | 5ms | 459.09 ops/s |
