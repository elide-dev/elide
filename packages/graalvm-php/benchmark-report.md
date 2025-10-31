# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 8.11 ms
- Median: 7.20 ms
- Std Dev: 1.96 ms
- Min: 5.16 ms
- Max: 12.91 ms
- Throughput: 123.28 ops/sec

**Memory:**
- Before: 23.36 MB
- After: 48.61 MB
- Delta: 25.25 MB

### array_sequential

**Performance:**
- Mean: 2.20 ms
- Median: 1.91 ms
- Std Dev: 831.16 μs
- Min: 1.18 ms
- Max: 4.89 ms
- Throughput: 453.55 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 29.65 MB
- Delta: 6.46 MB

### property_access

**Performance:**
- Mean: 3.23 ms
- Median: 2.78 ms
- Std Dev: 982.52 μs
- Min: 2.08 ms
- Max: 5.61 ms
- Throughput: 309.74 ops/sec

**Memory:**
- Before: 23.21 MB
- After: 30.17 MB
- Delta: 6.97 MB

### string_concat

**Performance:**
- Mean: 2.30 ms
- Median: 2.10 ms
- Std Dev: 1.10 ms
- Min: 1.05 ms
- Max: 5.70 ms
- Throughput: 434.82 ops/sec

**Memory:**
- Before: 23.22 MB
- After: 37.19 MB
- Delta: 13.97 MB

### arithmetic

**Performance:**
- Mean: 2.72 ms
- Median: 2.49 ms
- Std Dev: 899.66 μs
- Min: 1.81 ms
- Max: 5.18 ms
- Throughput: 367.50 ops/sec

**Memory:**
- Before: 23.22 MB
- After: 27.86 MB
- Delta: 4.64 MB

### method_calls

**Performance:**
- Mean: 2.34 ms
- Median: 2.05 ms
- Std Dev: 757.54 μs
- Min: 1.75 ms
- Max: 5.08 ms
- Throughput: 427.47 ops/sec

**Memory:**
- Before: 23.22 MB
- After: 49.70 MB
- Delta: 26.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 8ms | 7ms | 2ms | 5ms | 13ms | 123.28 ops/s |
 array_sequential | 2ms | 2ms | 831μs | 1ms | 5ms | 453.55 ops/s |
 property_access | 3ms | 3ms | 983μs | 2ms | 6ms | 309.74 ops/s |
 string_concat | 2ms | 2ms | 1ms | 1ms | 6ms | 434.82 ops/s |
 arithmetic | 3ms | 2ms | 900μs | 2ms | 5ms | 367.50 ops/s |
 method_calls | 2ms | 2ms | 758μs | 2ms | 5ms | 427.47 ops/s |
