<?php
// Benchmark: Associative array operations
// Tests hash map performance

$size = 500;
$arr = [];

// Create associative array
for ($i = 0; $i < $size; $i = $i + 1) {
    $key = "key_" . $i;
    $arr[$key] = $i * 2;
}

// Access all elements
$sum = 0;
foreach ($arr as $key => $value) {
    $sum += $value;
}

echo $sum;