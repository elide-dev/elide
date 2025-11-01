# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 19.44 ms
- Median: 16.77 ms
- Std Dev: 8.09 ms
- Min: 12.37 ms
- Max: 45.78 ms
- Throughput: 51.44 ops/sec

**Memory:**
- Before: 23.91 MB
- After: 50.02 MB
- Delta: 26.11 MB

### array_sequential

**Performance:**
- Mean: 3.57 ms
- Median: 2.56 ms
- Std Dev: 4.17 ms
- Min: 1.38 ms
- Max: 21.44 ms
- Throughput: 280.13 ops/sec

**Memory:**
- Before: 23.59 MB
- After: 30.92 MB
- Delta: 7.33 MB

### property_access

**Performance:**
- Mean: 20.82 ms
- Median: 18.09 ms
- Std Dev: 16.34 ms
- Min: 3.30 ms
- Max: 69.94 ms
- Throughput: 48.04 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 30.56 MB
- Delta: 6.97 MB

### string_concat

**Performance:**
- Mean: 4.31 ms
- Median: 4.04 ms
- Std Dev: 966.43 μs
- Min: 3.12 ms
- Max: 6.85 ms
- Throughput: 232.19 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 38.57 MB
- Delta: 14.97 MB

### arithmetic

**Performance:**
- Mean: 7.86 ms
- Median: 7.54 ms
- Std Dev: 1.47 ms
- Min: 5.93 ms
- Max: 10.96 ms
- Throughput: 127.21 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 63.93 MB
- Delta: 40.33 MB

### method_calls

**Performance:**
- Mean: 6.09 ms
- Median: 5.65 ms
- Std Dev: 2.14 ms
- Min: 3.44 ms
- Max: 12.44 ms
- Throughput: 164.08 ops/sec

**Memory:**
- Before: 23.60 MB
- After: 51.58 MB
- Delta: 27.99 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 19ms | 17ms | 8ms | 12ms | 46ms | 51.44 ops/s |
 array_sequential | 4ms | 3ms | 4ms | 1ms | 21ms | 280.13 ops/s |
 property_access | 21ms | 18ms | 16ms | 3ms | 70ms | 48.04 ops/s |
 string_concat | 4ms | 4ms | 966μs | 3ms | 7ms | 232.19 ops/s |
 arithmetic | 8ms | 8ms | 1ms | 6ms | 11ms | 127.21 ops/s |
 method_calls | 6ms | 6ms | 2ms | 3ms | 12ms | 164.08 ops/s |
