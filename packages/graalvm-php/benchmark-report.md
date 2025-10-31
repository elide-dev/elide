# TrufflePHP Benchmark Report

## Benchmark Suite: Micro Benchmarks

### array_associative

**Performance:**
- Mean: 6.14 ms
- Median: 6.08 ms
- Std Dev: 1.16 ms
- Min: 4.75 ms
- Max: 9.44 ms
- Throughput: 162.79 ops/sec

**Memory:**
- Before: 23.63 MB
- After: 50.39 MB
- Delta: 26.76 MB

### array_sequential

**Performance:**
- Mean: 1.92 ms
- Median: 1.67 ms
- Std Dev: 899.38 μs
- Min: 1.05 ms
- Max: 5.00 ms
- Throughput: 520.98 ops/sec

**Memory:**
- Before: 23.31 MB
- After: 31.28 MB
- Delta: 7.97 MB

### property_access

**Performance:**
- Mean: 2.02 ms
- Median: 1.95 ms
- Std Dev: 637.09 μs
- Min: 1.17 ms
- Max: 3.73 ms
- Throughput: 494.46 ops/sec

**Memory:**
- Before: 23.32 MB
- After: 30.29 MB
- Delta: 6.97 MB

### string_concat

**Performance:**
- Mean: 1.28 ms
- Median: 1.17 ms
- Std Dev: 633.80 μs
- Min: 723.47 μs
- Max: 3.85 ms
- Throughput: 778.81 ops/sec

**Memory:**
- Before: 23.33 MB
- After: 37.80 MB
- Delta: 14.47 MB

### arithmetic

**Performance:**
- Mean: 2.06 ms
- Median: 1.77 ms
- Std Dev: 763.56 μs
- Min: 1.42 ms
- Max: 4.30 ms
- Throughput: 486.10 ops/sec

**Memory:**
- Before: 23.33 MB
- After: 23.93 MB
- Delta: 614.79 KB

### method_calls

**Performance:**
- Mean: 2.09 ms
- Median: 1.91 ms
- Std Dev: 749.47 μs
- Min: 1.61 ms
- Max: 5.14 ms
- Throughput: 479.31 ops/sec

**Memory:**
- Before: 23.33 MB
- After: 50.81 MB
- Delta: 27.48 MB



## Summary

| Benchmark | Mean | Median | Std Dev | Min | Max | Throughput |
-----------|------|--------|---------|-----|-----|------------|
 array_associative | 6ms | 6ms | 1ms | 5ms | 9ms | 162.79 ops/s |
 array_sequential | 2ms | 2ms | 899μs | 1ms | 5ms | 520.98 ops/s |
 property_access | 2ms | 2ms | 637μs | 1ms | 4ms | 494.46 ops/s |
 string_concat | 1ms | 1ms | 634μs | 723μs | 4ms | 778.81 ops/s |
 arithmetic | 2ms | 2ms | 764μs | 1ms | 4ms | 486.10 ops/s |
 method_calls | 2ms | 2ms | 749μs | 2ms | 5ms | 479.31 ops/s |
