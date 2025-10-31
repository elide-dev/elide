# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 5.68 ms
- Median: 5.37 ms
- Std Dev: 1.09 ms
- Min: 4.23 ms
- Max: 8.40 ms
- Throughput: 176.08 ops/sec

**Memory:**
- Before: 23.72 MB
- After: 50.73 MB
- Delta: 27.01 MB

### array_sequential

**Performance:**
- Mean: 1.67 ms
- Median: 1.25 ms
- Std Dev: 966.99 μs
- Min: 863.93 μs
- Max: 4.42 ms
- Throughput: 600.43 ops/sec

**Memory:**
- Before: 23.39 MB
- After: 29.86 MB
- Delta: 6.47 MB

### property_access

**Performance:**
- Mean: 2.31 ms
- Median: 2.14 ms
- Std Dev: 848.86 μs
- Min: 1.25 ms
- Max: 4.60 ms
- Throughput: 433.63 ops/sec

**Memory:**
- Before: 23.41 MB
- After: 30.37 MB
- Delta: 6.97 MB

### string_concat

**Performance:**
- Mean: 3.37 ms
- Median: 2.09 ms
- Std Dev: 4.32 ms
- Min: 1.43 ms
- Max: 21.48 ms
- Throughput: 296.36 ops/sec

**Memory:**
- Before: 23.41 MB
- After: 38.38 MB
- Delta: 14.97 MB

### arithmetic

**Performance:**
- Mean: 2.88 ms
- Median: 2.61 ms
- Std Dev: 829.33 μs
- Min: 2.03 ms
- Max: 4.84 ms
- Throughput: 347.17 ops/sec

**Memory:**
- Before: 23.41 MB
- After: 24.06 MB
- Delta: 662.90 KB

### method_calls

**Performance:**
- Mean: 2.28 ms
- Median: 1.97 ms
- Std Dev: 1.01 ms
- Min: 1.60 ms
- Max: 6.46 ms
- Throughput: 439.22 ops/sec

**Memory:**
- Before: 23.47 MB
- After: 49.89 MB
- Delta: 26.42 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 6ms | 5ms | 1ms | 4ms | 8ms | 176.08 ops/s |
 array_sequential | 2ms | 1ms | 967μs | 864μs | 4ms | 600.43 ops/s |
 property_access | 2ms | 2ms | 849μs | 1ms | 5ms | 433.63 ops/s |
 string_concat | 3ms | 2ms | 4ms | 1ms | 21ms | 296.36 ops/s |
 arithmetic | 3ms | 3ms | 829μs | 2ms | 5ms | 347.17 ops/s |
 method_calls | 2ms | 2ms | 1ms | 2ms | 6ms | 439.22 ops/s |
