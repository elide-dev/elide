# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 9.17 ms
- Median: 8.79 ms
- Std Dev: 1.73 ms
- Min: 7.14 ms
- Max: 13.43 ms
- Throughput: 109.03 ops/sec

**Memory:**
- Before: 23.29 MB
- After: 50.04 MB
- Delta: 26.75 MB

### array_sequential

**Performance:**
- Mean: 2.72 ms
- Median: 2.51 ms
- Std Dev: 1.04 ms
- Min: 1.52 ms
- Max: 5.20 ms
- Throughput: 367.33 ops/sec

**Memory:**
- Before: 23.26 MB
- After: 29.58 MB
- Delta: 6.32 MB

### property_access

**Performance:**
- Mean: 3.09 ms
- Median: 2.88 ms
- Std Dev: 1.06 ms
- Min: 1.88 ms
- Max: 6.27 ms
- Throughput: 323.98 ops/sec

**Memory:**
- Before: 23.12 MB
- After: 30.10 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 1.77 ms
- Median: 1.53 ms
- Std Dev: 843.05 μs
- Min: 873.60 μs
- Max: 4.03 ms
- Throughput: 563.55 ops/sec

**Memory:**
- Before: 23.14 MB
- After: 36.62 MB
- Delta: 13.48 MB

### arithmetic

**Performance:**
- Mean: 2.97 ms
- Median: 2.67 ms
- Std Dev: 1.07 ms
- Min: 2.04 ms
- Max: 6.15 ms
- Throughput: 337.06 ops/sec

**Memory:**
- Before: 23.14 MB
- After: 30.79 MB
- Delta: 7.65 MB

### method_calls

**Performance:**
- Mean: 2.13 ms
- Median: 1.96 ms
- Std Dev: 793.58 μs
- Min: 1.53 ms
- Max: 5.21 ms
- Throughput: 469.03 ops/sec

**Memory:**
- Before: 23.14 MB
- After: 50.13 MB
- Delta: 26.99 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 9ms | 9ms | 2ms | 7ms | 13ms | 109.03 ops/s |
 array_sequential | 3ms | 3ms | 1ms | 2ms | 5ms | 367.33 ops/s |
 property_access | 3ms | 3ms | 1ms | 2ms | 6ms | 323.98 ops/s |
 string_concat | 2ms | 2ms | 843μs | 874μs | 4ms | 563.55 ops/s |
 arithmetic | 3ms | 3ms | 1ms | 2ms | 6ms | 337.06 ops/s |
 method_calls | 2ms | 2ms | 794μs | 2ms | 5ms | 469.03 ops/s |
