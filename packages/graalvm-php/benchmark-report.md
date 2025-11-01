# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 5.86 ms
- Median: 5.08 ms
- Std Dev: 1.45 ms
- Min: 4.36 ms
- Max: 8.94 ms
- Throughput: 170.79 ops/sec

**Memory:**
- Before: 23.88 MB
- After: 51.15 MB
- Delta: 27.27 MB

### array_sequential

**Performance:**
- Mean: 1.51 ms
- Median: 1.24 ms
- Std Dev: 881.74 μs
- Min: 802.43 μs
- Max: 4.83 ms
- Throughput: 664.11 ops/sec

**Memory:**
- Before: 23.76 MB
- After: 30.03 MB
- Delta: 6.27 MB

### property_access

**Performance:**
- Mean: 2.22 ms
- Median: 2.07 ms
- Std Dev: 800.13 μs
- Min: 1.27 ms
- Max: 4.77 ms
- Throughput: 449.98 ops/sec

**Memory:**
- Before: 23.55 MB
- After: 30.53 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 1.51 ms
- Median: 1.18 ms
- Std Dev: 948.12 μs
- Min: 703.76 μs
- Max: 4.65 ms
- Throughput: 661.20 ops/sec

**Memory:**
- Before: 23.56 MB
- After: 38.04 MB
- Delta: 14.48 MB

### arithmetic

**Performance:**
- Mean: 2.77 ms
- Median: 2.41 ms
- Std Dev: 1.01 ms
- Min: 1.89 ms
- Max: 5.37 ms
- Throughput: 360.69 ops/sec

**Memory:**
- Before: 23.56 MB
- After: 24.71 MB
- Delta: 1.15 MB

### method_calls

**Performance:**
- Mean: 2.42 ms
- Median: 2.24 ms
- Std Dev: 675.20 μs
- Min: 1.78 ms
- Max: 4.75 ms
- Throughput: 412.63 ops/sec

**Memory:**
- Before: 23.56 MB
- After: 51.05 MB
- Delta: 27.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 6ms | 5ms | 1ms | 4ms | 9ms | 170.79 ops/s |
 array_sequential | 2ms | 1ms | 882μs | 802μs | 5ms | 664.11 ops/s |
 property_access | 2ms | 2ms | 800μs | 1ms | 5ms | 449.98 ops/s |
 string_concat | 2ms | 1ms | 948μs | 704μs | 5ms | 661.20 ops/s |
 arithmetic | 3ms | 2ms | 1ms | 2ms | 5ms | 360.69 ops/s |
 method_calls | 2ms | 2ms | 675μs | 2ms | 5ms | 412.63 ops/s |
