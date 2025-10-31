<?php
// Benchmark: Method calls
// Tests method invocation performance

class Calculator {
    private $value;

    public function __construct($initial) {
        $this->value = $initial;
    }

    public function add($x) {
        $this->value += $x;
        return $this;
    }

    public function multiply($x) {
        $this->value *= $x;
        return $this;
    }

    public function getValue() {
        return $this->value;
    }

    private function internalCalculation($x, $y) {
        return $x * $y + $this->value;
    }

    public function complexOperation($x) {
        return $this->internalCalculation($x, 2);
    }
}

// Method call benchmark
$calc = new Calculator(0);
for ($i = 0; $i < 1000; $i = $i + 1) {
    $calc->add($i)->multiply(2);
}

// Private method calls
$sum = 0;
for ($i = 0; $i < 1000; $i = $i + 1) {
    $sum += $calc->complexOperation($i);
}

echo $calc->getValue() + $sum;