<?php
// Benchmark: String concatenation
// Tests string operations performance

$iterations = 1000;

// String concatenation
$result = "";
for ($i = 0; $i < $iterations; $i = $i + 1) {
    $result = "Item " . $i . ": ";
    $result = $result . "value";
}

// String interpolation
$interpolated = "";
for ($i = 0; $i < $iterations; $i = $i + 1) {
    $interpolated = "Item $i has value";
}

// String functions
$length = 0;
for ($i = 0; $i < 100; $i = $i + 1) {
    $str = "Hello World " . $i;
    $length += strlen($str);
    $lower = strtolower($str);
    $upper = strtoupper($str);
}

echo $length;