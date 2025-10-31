<?php
// Benchmark: Sequential array creation and access
// Tests the most common array pattern in PHP

$size = 1000;
$arr = [];

// Create sequential array
for ($i = 0; $i < $size; $i = $i + 1) {
    $arr[] = $i;
}

// Access all elements
$sum = 0;
for ($i = 0; $i < $size; $i = $i + 1) {
    $sum += $arr[$i];
}

echo $sum;