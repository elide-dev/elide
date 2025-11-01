package dev.truffle.php

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for PHP reference functionality (&$var).
 * References allow multiple variables to refer to the same value.
 */
class PhpReferenceTest : AbstractPhpTest() {

    // ========== Function Parameters by Reference ==========

    @Test fun `function parameter by reference modifies original`() {
        val output = executePhp("""
            <?php
            function increment(&${'$'}value) {
                ${'$'}value++;
            }
            ${'$'}num = 5;
            increment(${'$'}num);
            echo ${'$'}num;
        """.trimIndent())
        assertEquals("6", output.trim())
    }

    @Test fun `function parameter by reference with multiple parameters`() {
        val output = executePhp("""
            <?php
            function swap(&${'$'}a, &${'$'}b) {
                ${'$'}temp = ${'$'}a;
                ${'$'}a = ${'$'}b;
                ${'$'}b = ${'$'}temp;
            }
            ${'$'}x = 10;
            ${'$'}y = 20;
            swap(${'$'}x, ${'$'}y);
            echo ${'$'}x . "," . ${'$'}y;
        """.trimIndent())
        assertEquals("20,10", output.trim())
    }

    @Test fun `function with mixed by-value and by-reference parameters`() {
        val output = executePhp("""
            <?php
            function addAndUpdate(${'$'}value, &${'$'}result) {
                ${'$'}result += ${'$'}value;
            }
            ${'$'}sum = 10;
            addAndUpdate(5, ${'$'}sum);
            addAndUpdate(3, ${'$'}sum);
            echo ${'$'}sum;
        """.trimIndent())
        assertEquals("18", output.trim())
    }

    // ========== Reference Assignment ==========

    @Test fun `reference assignment creates alias`() {
        val output = executePhp("""
            <?php
            ${'$'}a = 5;
            ${'$'}b =& ${'$'}a;
            ${'$'}b = 10;
            echo ${'$'}a;
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    @Test fun `reference assignment works both ways`() {
        val output = executePhp("""
            <?php
            ${'$'}a = 5;
            ${'$'}b =& ${'$'}a;
            ${'$'}a = 15;
            ${'$'}b = 20;
            echo ${'$'}a . "," . ${'$'}b;
        """.trimIndent())
        assertEquals("20,20", output.trim())
    }

    @Test fun `multiple variables can reference same value`() {
        val output = executePhp("""
            <?php
            ${'$'}a = 1;
            ${'$'}b =& ${'$'}a;
            ${'$'}c =& ${'$'}a;
            ${'$'}c = 100;
            echo ${'$'}a . "," . ${'$'}b . "," . ${'$'}c;
        """.trimIndent())
        assertEquals("100,100,100", output.trim())
    }

    // ========== Foreach with References ==========

    @Test fun `foreach with reference modifies original array`() {
        val output = executePhp("""
            <?php
            ${'$'}arr = [1, 2, 3];
            foreach (${'$'}arr as &${'$'}value) {
                ${'$'}value *= 2;
            }
            echo implode(",", ${'$'}arr);
        """.trimIndent())
        assertEquals("2,4,6", output.trim())
    }

    @Test fun `foreach with key and reference value`() {
        val output = executePhp("""
            <?php
            ${'$'}arr = ["a" => 1, "b" => 2];
            foreach (${'$'}arr as ${'$'}key => &${'$'}value) {
                ${'$'}value += 10;
            }
            echo ${'$'}arr["a"] . "," . ${'$'}arr["b"];
        """.trimIndent())
        assertEquals("11,12", output.trim())
    }

    // ========== Return by Reference ==========

    @Test fun `function returning by reference`() {
        val output = executePhp("""
            <?php
            ${'$'}value = 5;
            function &getValue() {
                global ${'$'}value;
                return ${'$'}value;
            }
            ${'$'}ref =& getValue();
            ${'$'}ref = 10;
            echo ${'$'}value;
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    // ========== Edge Cases ==========

    @Test fun `reference to array element`() {
        val output = executePhp("""
            <?php
            ${'$'}arr = [1, 2, 3];
            ${'$'}ref =& ${'$'}arr[1];
            ${'$'}ref = 20;
            echo implode(",", ${'$'}arr);
        """.trimIndent())
        assertEquals("1,20,3", output.trim())
    }

    @Test fun `unset does not affect other references`() {
        val output = executePhp("""
            <?php
            ${'$'}a = 5;
            ${'$'}b =& ${'$'}a;
            unset(${'$'}a);
            echo ${'$'}b;
        """.trimIndent())
        assertEquals("5", output.trim())
    }

    @Test fun `reference parameter with object`() {
        val output = executePhp("""
            <?php
            class Counter {
                public ${'$'}count = 0;
            }
            function incrementCounter(&${'$'}counter) {
                ${'$'}counter->count++;
            }
            ${'$'}c = new Counter();
            incrementCounter(${'$'}c);
            incrementCounter(${'$'}c);
            echo ${'$'}c->count;
        """.trimIndent())
        assertEquals("2", output.trim())
    }
}
