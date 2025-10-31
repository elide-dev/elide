# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 7.62 ms
- Median: 7.65 ms
- Std Dev: 1.40 ms
- Min: 5.42 ms
- Max: 12.12 ms
- Throughput: 131.17 ops/sec

**Memory:**
- Before: 23.34 MB
- After: 47.21 MB
- Delta: 23.87 MB

### array_sequential

**Performance:**
- Mean: 2.03 ms
- Median: 1.85 ms
- Std Dev: 696.99 μs
- Min: 1.16 ms
- Max: 4.22 ms
- Throughput: 491.55 ops/sec

**Memory:**
- Before: 23.17 MB
- After: 29.64 MB
- Delta: 6.46 MB

### property_access

**Performance:**
- Mean: 2.24 ms
- Median: 2.03 ms
- Std Dev: 753.37 μs
- Min: 1.44 ms
- Max: 4.57 ms
- Throughput: 447.35 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 30.15 MB
- Delta: 6.97 MB

### string_concat

**Performance:**
- Mean: 2.13 ms
- Median: 1.86 ms
- Std Dev: 920.03 μs
- Min: 1.13 ms
- Max: 5.12 ms
- Throughput: 469.22 ops/sec

**Memory:**
- Before: 23.20 MB
- After: 37.17 MB
- Delta: 13.97 MB

### arithmetic

**Performance:**
- Mean: 2.61 ms
- Median: 2.35 ms
- Std Dev: 877.92 μs
- Min: 1.81 ms
- Max: 5.09 ms
- Throughput: 382.94 ops/sec

**Memory:**
- Before: 23.20 MB
- After: 27.87 MB
- Delta: 4.66 MB

### method_calls

**Performance:**
- Mean: 2.28 ms
- Median: 1.91 ms
- Std Dev: 962.28 μs
- Min: 1.50 ms
- Max: 5.63 ms
- Throughput: 438.82 ops/sec

**Memory:**
- Before: 23.20 MB
- After: 50.69 MB
- Delta: 27.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 8ms | 8ms | 1ms | 5ms | 12ms | 131.17 ops/s |
 array_sequential | 2ms | 2ms | 697μs | 1ms | 4ms | 491.55 ops/s |
 property_access | 2ms | 2ms | 753μs | 1ms | 5ms | 447.35 ops/s |
 string_concat | 2ms | 2ms | 920μs | 1ms | 5ms | 469.22 ops/s |
 arithmetic | 3ms | 2ms | 878μs | 2ms | 5ms | 382.94 ops/s |
 method_calls | 2ms | 2ms | 962μs | 1ms | 6ms | 438.82 ops/s |
