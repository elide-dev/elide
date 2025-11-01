/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package dev.truffle.php

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for PHP variadic functions and spread operator.
 */
class PhpVariadicTest : AbstractPhpTest() {
    @Test fun `variadic function collects all arguments`() {
        val output = executePhp("""
            <?php
            function sum(...${'$'}numbers) {
                ${'$'}total = 0;
                foreach (${'$'}numbers as ${'$'}num) {
                    ${'$'}total += ${'$'}num;
                }
                return ${'$'}total;
            }
            echo sum(1, 2, 3, 4, 5);
        """.trimIndent())
        assertEquals("15", output.trim())
    }

    @Test fun `variadic function with no arguments`() {
        val output = executePhp("""
            <?php
            function count_args(...${'$'}args) {
                return count(${'$'}args);
            }
            echo count_args();
        """.trimIndent())
        assertEquals("0", output.trim())
    }

    @Test fun `variadic function with fixed and variadic parameters`() {
        val output = executePhp("""
            <?php
            function greet(${'$'}greeting, ...${'$'}names) {
                foreach (${'$'}names as ${'$'}name) {
                    echo ${'$'}greeting . " " . ${'$'}name . " ";
                }
            }
            greet("Hello", "Alice", "Bob", "Charlie");
        """.trimIndent())
        assertEquals("Hello Alice Hello Bob Hello Charlie", output.trim())
    }

    @Test fun `spread operator unpacks array in function call`() {
        val output = executePhp("""
            <?php
            function add(${'$'}a, ${'$'}b, ${'$'}c) {
                return ${'$'}a + ${'$'}b + ${'$'}c;
            }
            ${'$'}numbers = [1, 2, 3];
            echo add(...${'$'}numbers);
        """.trimIndent())
        assertEquals("6", output.trim())
    }

    @Test fun `spread operator with mixed arguments`() {
        val output = executePhp("""
            <?php
            function test(${'$'}a, ${'$'}b, ${'$'}c, ${'$'}d) {
                return ${'$'}a + ${'$'}b + ${'$'}c + ${'$'}d;
            }
            ${'$'}arr = [2, 3];
            echo test(1, ...${'$'}arr, 4);
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    @Test fun `variadic and spread together`() {
        val output = executePhp("""
            <?php
            function sum(...${'$'}numbers) {
                ${'$'}total = 0;
                foreach (${'$'}numbers as ${'$'}num) {
                    ${'$'}total += ${'$'}num;
                }
                return ${'$'}total;
            }
            ${'$'}nums = [10, 20, 30];
            echo sum(...${'$'}nums);
        """.trimIndent())
        assertEquals("60", output.trim())
    }

    @Test fun `multiple spread operators in call`() {
        val output = executePhp("""
            <?php
            function concat(...${'$'}strings) {
                ${'$'}result = "";
                foreach (${'$'}strings as ${'$'}str) {
                    ${'$'}result .= ${'$'}str;
                }
                return ${'$'}result;
            }
            ${'$'}arr1 = ["a", "b"];
            ${'$'}arr2 = ["c", "d"];
            echo concat(...${'$'}arr1, ...${'$'}arr2);
        """.trimIndent())
        assertEquals("abcd", output.trim())
    }

    @Test fun `variadic parameter receives array type`() {
        val output = executePhp("""
            <?php
            function test(...${'$'}args) {
                echo gettype(${'$'}args);
            }
            test(1, 2, 3);
        """.trimIndent())
        assertEquals("array", output.trim())
    }

    @Test fun `variadic in method`() {
        val output = executePhp("""
            <?php
            class Calculator {
                public function sum(...${'$'}numbers) {
                    ${'$'}total = 0;
                    foreach (${'$'}numbers as ${'$'}num) {
                        ${'$'}total += ${'$'}num;
                    }
                    return ${'$'}total;
                }
            }
            ${'$'}calc = new Calculator();
            echo ${'$'}calc->sum(5, 10, 15);
        """.trimIndent())
        assertEquals("30", output.trim())
    }

    @Test fun `spread with empty array`() {
        val output = executePhp("""
            <?php
            function test(...${'$'}args) {
                return count(${'$'}args);
            }
            ${'$'}empty = [];
            echo test(...${'$'}empty);
        """.trimIndent())
        assertEquals("0", output.trim())
    }
}
