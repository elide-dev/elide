<?php
// Benchmark: Object property access
// Tests property read/write performance

class Point {
    public $x;
    public $y;
    private $z;

    public function __construct($x, $y, $z) {
        $this->x = $x;
        $this->y = $y;
        $this->z = $z;
    }

    public function getZ() {
        return $this->z;
    }

    public function setZ($z) {
        $this->z = $z;
    }
}

// Create objects
$points = [];
for ($i = 0; $i < 100; $i = $i + 1) {
    $points[] = new Point($i, $i * 2, $i * 3);
}

// Access properties
$sum = 0;
for ($i = 0; $i < 100; $i = $i + 1) {
    $sum += $points[$i]->x;
    $sum += $points[$i]->y;
    $sum += $points[$i]->getZ();

    $points[$i]->x = $i * 4;
    $points[$i]->setZ($i * 5);
}

echo $sum;