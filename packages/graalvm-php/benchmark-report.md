# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 5.19 ms
- Median: 4.77 ms
- Std Dev: 1.33 ms
- Min: 4.00 ms
- Max: 9.19 ms
- Throughput: 192.68 ops/sec

**Memory:**
- Before: 23.91 MB
- After: 49.68 MB
- Delta: 25.77 MB

### array_sequential

**Performance:**
- Mean: 2.07 ms
- Median: 1.11 ms
- Std Dev: 3.20 ms
- Min: 843.98 μs
- Max: 15.68 ms
- Throughput: 482.08 ops/sec

**Memory:**
- Before: 23.59 MB
- After: 30.89 MB
- Delta: 7.30 MB

### property_access

**Performance:**
- Mean: 1.53 ms
- Median: 1.34 ms
- Std Dev: 640.42 μs
- Min: 894.05 μs
- Max: 4.08 ms
- Throughput: 653.23 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 30.56 MB
- Delta: 6.96 MB

### string_concat

**Performance:**
- Mean: 1.27 ms
- Median: 1.02 ms
- Std Dev: 726.07 μs
- Min: 700.67 μs
- Max: 4.11 ms
- Throughput: 786.48 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 38.57 MB
- Delta: 14.97 MB

### arithmetic

**Performance:**
- Mean: 2.55 ms
- Median: 2.22 ms
- Std Dev: 750.88 μs
- Min: 1.88 ms
- Max: 4.56 ms
- Throughput: 392.29 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 24.26 MB
- Delta: 678.36 KB

### method_calls

**Performance:**
- Mean: 2.14 ms
- Median: 1.91 ms
- Std Dev: 990.39 μs
- Min: 1.47 ms
- Max: 6.26 ms
- Throughput: 468.16 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 51.58 MB
- Delta: 27.98 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 5ms | 5ms | 1ms | 4ms | 9ms | 192.68 ops/s |
 array_sequential | 2ms | 1ms | 3ms | 844μs | 16ms | 482.08 ops/s |
 property_access | 2ms | 1ms | 640μs | 894μs | 4ms | 653.23 ops/s |
 string_concat | 1ms | 1ms | 726μs | 701μs | 4ms | 786.48 ops/s |
 arithmetic | 3ms | 2ms | 751μs | 2ms | 5ms | 392.29 ops/s |
 method_calls | 2ms | 2ms | 990μs | 1ms | 6ms | 468.16 ops/s |
