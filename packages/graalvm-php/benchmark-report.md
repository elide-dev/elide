# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 7.25 ms
- Median: 6.87 ms
- Std Dev: 2.00 ms
- Min: 5.13 ms
- Max: 14.48 ms
- Throughput: 137.94 ops/sec

**Memory:**
- Before: 23.38 MB
- After: 50.98 MB
- Delta: 27.59 MB

### array_sequential

**Performance:**
- Mean: 2.29 ms
- Median: 1.98 ms
- Std Dev: 814.97 μs
- Min: 1.39 ms
- Max: 4.98 ms
- Throughput: 437.29 ops/sec

**Memory:**
- Before: 23.12 MB
- After: 29.59 MB
- Delta: 6.48 MB

### property_access

**Performance:**
- Mean: 2.57 ms
- Median: 2.27 ms
- Std Dev: 1.07 ms
- Min: 1.38 ms
- Max: 5.49 ms
- Throughput: 388.83 ops/sec

**Memory:**
- Before: 23.13 MB
- After: 30.11 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 2.04 ms
- Median: 1.70 ms
- Std Dev: 1.01 ms
- Min: 1.14 ms
- Max: 4.97 ms
- Throughput: 491.38 ops/sec

**Memory:**
- Before: 23.15 MB
- After: 37.13 MB
- Delta: 13.98 MB

### arithmetic

**Performance:**
- Mean: 2.56 ms
- Median: 2.23 ms
- Std Dev: 969.98 μs
- Min: 1.56 ms
- Max: 5.24 ms
- Throughput: 390.90 ops/sec

**Memory:**
- Before: 23.15 MB
- After: 26.79 MB
- Delta: 3.63 MB

### method_calls

**Performance:**
- Mean: 4.60 ms
- Median: 4.45 ms
- Std Dev: 907.14 μs
- Min: 3.19 ms
- Max: 6.67 ms
- Throughput: 217.52 ops/sec

**Memory:**
- Before: 23.16 MB
- After: 49.65 MB
- Delta: 26.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 7ms | 7ms | 2ms | 5ms | 14ms | 137.94 ops/s |
 array_sequential | 2ms | 2ms | 815μs | 1ms | 5ms | 437.29 ops/s |
 property_access | 3ms | 2ms | 1ms | 1ms | 5ms | 388.83 ops/s |
 string_concat | 2ms | 2ms | 1ms | 1ms | 5ms | 491.38 ops/s |
 arithmetic | 3ms | 2ms | 970μs | 2ms | 5ms | 390.90 ops/s |
 method_calls | 5ms | 4ms | 907μs | 3ms | 7ms | 217.52 ops/s |
