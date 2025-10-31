# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 7.46 ms
- Median: 6.96 ms
- Std Dev: 1.45 ms
- Min: 5.89 ms
- Max: 11.38 ms
- Throughput: 134.09 ops/sec

**Memory:**
- Before: 23.71 MB
- After: 51.48 MB
- Delta: 27.77 MB

### array_sequential

**Performance:**
- Mean: 2.22 ms
- Median: 1.64 ms
- Std Dev: 1.40 ms
- Min: 1.18 ms
- Max: 6.06 ms
- Throughput: 450.37 ops/sec

**Memory:**
- Before: 23.38 MB
- After: 30.86 MB
- Delta: 7.48 MB

### property_access

**Performance:**
- Mean: 2.07 ms
- Median: 1.84 ms
- Std Dev: 856.26 μs
- Min: 1.07 ms
- Max: 4.54 ms
- Throughput: 483.08 ops/sec

**Memory:**
- Before: 23.39 MB
- After: 30.37 MB
- Delta: 6.98 MB

### string_concat

**Performance:**
- Mean: 1.53 ms
- Median: 1.37 ms
- Std Dev: 830.86 μs
- Min: 935.23 μs
- Max: 4.97 ms
- Throughput: 653.70 ops/sec

**Memory:**
- Before: 23.40 MB
- After: 37.88 MB
- Delta: 14.48 MB

### arithmetic

**Performance:**
- Mean: 3.45 ms
- Median: 2.36 ms
- Std Dev: 3.12 ms
- Min: 1.49 ms
- Max: 16.34 ms
- Throughput: 290.27 ops/sec

**Memory:**
- Before: 23.40 MB
- After: 24.54 MB
- Delta: 1.14 MB

### method_calls

**Performance:**
- Mean: 2.03 ms
- Median: 1.89 ms
- Std Dev: 792.26 μs
- Min: 1.45 ms
- Max: 5.13 ms
- Throughput: 492.01 ops/sec

**Memory:**
- Before: 23.40 MB
- After: 49.89 MB
- Delta: 26.49 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 7ms | 7ms | 1ms | 6ms | 11ms | 134.09 ops/s |
 array_sequential | 2ms | 2ms | 1ms | 1ms | 6ms | 450.37 ops/s |
 property_access | 2ms | 2ms | 856μs | 1ms | 5ms | 483.08 ops/s |
 string_concat | 2ms | 1ms | 831μs | 935μs | 5ms | 653.70 ops/s |
 arithmetic | 3ms | 2ms | 3ms | 1ms | 16ms | 290.27 ops/s |
 method_calls | 2ms | 2ms | 792μs | 1ms | 5ms | 492.01 ops/s |
