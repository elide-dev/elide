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
 * Tests for PHP closures and anonymous functions.
 */
class PhpClosureTest : AbstractPhpTest() {
    @Test fun `basic anonymous function`() {
        val output = executePhp("""
            <?php
            ${'$'}double = function(${'$'}x) {
                return ${'$'}x * 2;
            };
            echo ${'$'}double(5);
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    @Test fun `anonymous function with multiple parameters`() {
        val output = executePhp("""
            <?php
            ${'$'}add = function(${'$'}a, ${'$'}b) {
                return ${'$'}a + ${'$'}b;
            };
            echo ${'$'}add(3, 7);
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    @Test fun `closure with use clause captures variable`() {
        val output = executePhp("""
            <?php
            ${'$'}multiplier = 3;
            ${'$'}multiply = function(${'$'}x) use (${'$'}multiplier) {
                return ${'$'}x * ${'$'}multiplier;
            };
            echo ${'$'}multiply(4);
        """.trimIndent())
        assertEquals("12", output.trim())
    }

    @Test fun `closure with multiple captured variables`() {
        val output = executePhp("""
            <?php
            ${'$'}a = 10;
            ${'$'}b = 5;
            ${'$'}calc = function(${'$'}x) use (${'$'}a, ${'$'}b) {
                return ${'$'}x + ${'$'}a - ${'$'}b;
            };
            echo ${'$'}calc(2);
        """.trimIndent())
        assertEquals("7", output.trim())
    }

    @Test fun `closure passed as argument`() {
        val output = executePhp("""
            <?php
            function apply(${'$'}value, ${'$'}func) {
                return ${'$'}func(${'$'}value);
            }

            ${'$'}square = function(${'$'}x) {
                return ${'$'}x * ${'$'}x;
            };

            echo apply(5, ${'$'}square);
        """.trimIndent())
        assertEquals("25", output.trim())
    }

    @Test fun `closure returned from function`() {
        val output = executePhp("""
            <?php
            function makeAdder(${'$'}n) {
                return function(${'$'}x) use (${'$'}n) {
                    return ${'$'}x + ${'$'}n;
                };
            }

            ${'$'}add5 = makeAdder(5);
            echo ${'$'}add5(10);
        """.trimIndent())
        assertEquals("15", output.trim())
    }

    @Test fun `closure with no parameters`() {
        val output = executePhp("""
            <?php
            ${'$'}getMessage = function() {
                return "Hello";
            };
            echo ${'$'}getMessage();
        """.trimIndent())
        assertEquals("Hello", output.trim())
    }

    @Test fun `closure modifying captured variable by reference`() {
        val output = executePhp("""
            <?php
            ${'$'}counter = 0;
            ${'$'}increment = function() use (&${'$'}counter) {
                ${'$'}counter++;
            };
            ${'$'}increment();
            ${'$'}increment();
            echo ${'$'}counter;
        """.trimIndent())
        assertEquals("2", output.trim())
    }

    @Test fun `arrow function basic`() {
        val output = executePhp("""
            <?php
            ${'$'}double = fn(${'$'}x) => ${'$'}x * 2;
            echo ${'$'}double(5);
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    @Test fun `arrow function with multiple parameters`() {
        val output = executePhp("""
            <?php
            ${'$'}add = fn(${'$'}a, ${'$'}b) => ${'$'}a + ${'$'}b;
            echo ${'$'}add(3, 7);
        """.trimIndent())
        assertEquals("10", output.trim())
    }

    @Test fun `arrow function auto-captures variables`() {
        val output = executePhp("""
            <?php
            ${'$'}multiplier = 3;
            ${'$'}multiply = fn(${'$'}x) => ${'$'}x * ${'$'}multiplier;
            echo ${'$'}multiply(4);
        """.trimIndent())
        assertEquals("12", output.trim())
    }

    @Test fun `arrow function passed as argument`() {
        val output = executePhp("""
            <?php
            function apply(${'$'}value, ${'$'}func) {
                return ${'$'}func(${'$'}value);
            }

            echo apply(5, fn(${'$'}x) => ${'$'}x * ${'$'}x);
        """.trimIndent())
        assertEquals("25", output.trim())
    }

    @Test fun `arrow function with expression`() {
        val output = executePhp("""
            <?php
            ${'$'}isEven = fn(${'$'}n) => ${'$'}n % 2 == 0;
            echo ${'$'}isEven(4) ? "yes" : "no";
        """.trimIndent())
        assertEquals("yes", output.trim())
    }
}
