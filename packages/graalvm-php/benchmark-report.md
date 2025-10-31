# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 7.82 ms
- Median: 7.71 ms
- Std Dev: 2.08 ms
- Min: 5.24 ms
- Max: 14.25 ms
- Throughput: 127.90 ops/sec

**Memory:**
- Before: 23.41 MB
- After: 49.49 MB
- Delta: 26.08 MB

### array_sequential

**Performance:**
- Mean: 2.40 ms
- Median: 2.19 ms
- Std Dev: 914.37 μs
- Min: 1.33 ms
- Max: 5.32 ms
- Throughput: 416.34 ops/sec

**Memory:**
- Before: 23.15 MB
- After: 29.63 MB
- Delta: 6.48 MB

### property_access

**Performance:**
- Mean: 2.38 ms
- Median: 2.38 ms
- Std Dev: 698.81 μs
- Min: 1.41 ms
- Max: 4.20 ms
- Throughput: 420.99 ops/sec

**Memory:**
- Before: 23.17 MB
- After: 30.15 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 2.10 ms
- Median: 1.87 ms
- Std Dev: 685.52 μs
- Min: 1.22 ms
- Max: 4.20 ms
- Throughput: 476.63 ops/sec

**Memory:**
- Before: 23.18 MB
- After: 37.16 MB
- Delta: 13.98 MB

### arithmetic

**Performance:**
- Mean: 2.78 ms
- Median: 2.40 ms
- Std Dev: 1.01 ms
- Min: 1.90 ms
- Max: 5.81 ms
- Throughput: 360.30 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 27.83 MB
- Delta: 4.64 MB

### method_calls

**Performance:**
- Mean: 2.46 ms
- Median: 2.07 ms
- Std Dev: 928.88 μs
- Min: 1.72 ms
- Max: 5.52 ms
- Throughput: 406.00 ops/sec

**Memory:**
- Before: 23.19 MB
- After: 50.68 MB
- Delta: 27.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 8ms | 8ms | 2ms | 5ms | 14ms | 127.90 ops/s |
 array_sequential | 2ms | 2ms | 914μs | 1ms | 5ms | 416.34 ops/s |
 property_access | 2ms | 2ms | 699μs | 1ms | 4ms | 420.99 ops/s |
 string_concat | 2ms | 2ms | 686μs | 1ms | 4ms | 476.63 ops/s |
 arithmetic | 3ms | 2ms | 1ms | 2ms | 6ms | 360.30 ops/s |
 method_calls | 2ms | 2ms | 929μs | 2ms | 6ms | 406.00 ops/s |
