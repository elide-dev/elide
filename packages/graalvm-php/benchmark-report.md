# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 6.90 ms
- Median: 6.32 ms
- Std Dev: 1.92 ms
- Min: 5.03 ms
- Max: 12.31 ms
- Throughput: 144.89 ops/sec

**Memory:**
- Before: 23.87 MB
- After: 52.32 MB
- Delta: 28.45 MB

### array_sequential

**Performance:**
- Mean: 1.72 ms
- Median: 1.49 ms
- Std Dev: 812.96 μs
- Min: 818.59 μs
- Max: 4.02 ms
- Throughput: 581.70 ops/sec

**Memory:**
- Before: 23.53 MB
- After: 30.51 MB
- Delta: 6.98 MB

### property_access

**Performance:**
- Mean: 2.71 ms
- Median: 2.66 ms
- Std Dev: 716.31 μs
- Min: 1.46 ms
- Max: 4.63 ms
- Throughput: 369.12 ops/sec

**Memory:**
- Before: 23.55 MB
- After: 30.53 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 1.30 ms
- Median: 1.07 ms
- Std Dev: 705.78 μs
- Min: 632.62 μs
- Max: 3.90 ms
- Throughput: 766.36 ops/sec

**Memory:**
- Before: 23.56 MB
- After: 37.53 MB
- Delta: 13.98 MB

### arithmetic

**Performance:**
- Mean: 2.74 ms
- Median: 2.41 ms
- Std Dev: 930.77 μs
- Min: 2.00 ms
- Max: 5.04 ms
- Throughput: 364.54 ops/sec

**Memory:**
- Before: 23.56 MB
- After: 24.20 MB
- Delta: 656.17 KB

### method_calls

**Performance:**
- Mean: 2.17 ms
- Median: 1.92 ms
- Std Dev: 827.31 μs
- Min: 1.62 ms
- Max: 5.44 ms
- Throughput: 461.80 ops/sec

**Memory:**
- Before: 23.56 MB
- After: 51.05 MB
- Delta: 27.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 7ms | 6ms | 2ms | 5ms | 12ms | 144.89 ops/s |
 array_sequential | 2ms | 1ms | 813μs | 819μs | 4ms | 581.70 ops/s |
 property_access | 3ms | 3ms | 716μs | 1ms | 5ms | 369.12 ops/s |
 string_concat | 1ms | 1ms | 706μs | 633μs | 4ms | 766.36 ops/s |
 arithmetic | 3ms | 2ms | 931μs | 2ms | 5ms | 364.54 ops/s |
 method_calls | 2ms | 2ms | 827μs | 2ms | 5ms | 461.80 ops/s |
