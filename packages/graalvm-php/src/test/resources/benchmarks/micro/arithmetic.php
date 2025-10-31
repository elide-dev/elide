<?php
// Benchmark: Arithmetic operations
// Tests integer and float arithmetic performance

$iterations = 10000;

// Integer arithmetic
$sum = 0;
for ($i = 0; $i < $iterations; $i = $i + 1) {
    $sum = $sum + $i;
    $sum = $sum - ($i / 2);
    $sum = $sum * 2;
    $sum = $sum / 3;
}

// Float arithmetic
$float_sum = 0.0;
for ($i = 0; $i < $iterations; $i = $i + 1) {
    $float_sum = $float_sum + ($i * 1.5);
    $float_sum = $float_sum - ($i * 0.5);
    $float_sum = $float_sum * 1.1;
    $float_sum = $float_sum / 2.2;
}

echo $sum + $float_sum;