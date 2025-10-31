# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 6.11 ms
- Median: 6.09 ms
- Std Dev: 966.06 μs
- Min: 4.35 ms
- Max: 8.77 ms
- Throughput: 163.69 ops/sec

**Memory:**
- Before: 23.95 MB
- After: 50.22 MB
- Delta: 26.27 MB

### array_sequential

**Performance:**
- Mean: 1.49 ms
- Median: 1.15 ms
- Std Dev: 871.44 μs
- Min: 875.92 μs
- Max: 4.92 ms
- Throughput: 670.21 ops/sec

**Memory:**
- Before: 23.63 MB
- After: 30.10 MB
- Delta: 6.47 MB

### property_access

**Performance:**
- Mean: 3.03 ms
- Median: 2.16 ms
- Std Dev: 3.75 ms
- Min: 791.84 μs
- Max: 18.68 ms
- Throughput: 330.21 ops/sec

**Memory:**
- Before: 23.64 MB
- After: 31.60 MB
- Delta: 7.97 MB

### string_concat

**Performance:**
- Mean: 1.33 ms
- Median: 1.18 ms
- Std Dev: 717.16 μs
- Min: 737.11 μs
- Max: 4.18 ms
- Throughput: 754.31 ops/sec

**Memory:**
- Before: 23.64 MB
- After: 38.11 MB
- Delta: 14.47 MB

### arithmetic

**Performance:**
- Mean: 2.67 ms
- Median: 2.42 ms
- Std Dev: 683.08 μs
- Min: 2.05 ms
- Max: 4.76 ms
- Throughput: 374.88 ops/sec

**Memory:**
- Before: 23.64 MB
- After: 25.76 MB
- Delta: 2.12 MB

### method_calls

**Performance:**
- Mean: 2.11 ms
- Median: 1.91 ms
- Std Dev: 787.05 μs
- Min: 1.55 ms
- Max: 5.41 ms
- Throughput: 474.12 ops/sec

**Memory:**
- Before: 23.64 MB
- After: 50.12 MB
- Delta: 26.48 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 6ms | 6ms | 966μs | 4ms | 9ms | 163.69 ops/s |
 array_sequential | 1ms | 1ms | 871μs | 876μs | 5ms | 670.21 ops/s |
 property_access | 3ms | 2ms | 4ms | 792μs | 19ms | 330.21 ops/s |
 string_concat | 1ms | 1ms | 717μs | 737μs | 4ms | 754.31 ops/s |
 arithmetic | 3ms | 2ms | 683μs | 2ms | 5ms | 374.88 ops/s |
 method_calls | 2ms | 2ms | 787μs | 2ms | 5ms | 474.12 ops/s |
