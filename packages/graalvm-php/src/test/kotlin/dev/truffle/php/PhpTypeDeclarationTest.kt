package dev.truffle.php

import kotlin.test.*

/**
 * Tests for PHP type declarations (parameter and return types).
 */
class PhpTypeDeclarationTest : AbstractPhpTest() {

    // ========== Function Parameter Type Tests ==========

    @Test fun `function with int parameter accepts integer`() {
        val output = executePhp("""
            <?php
            function add(int ${'$'}a, int ${'$'}b) {
                return ${'$'}a + ${'$'}b;
            }
            echo add(5, 3);
        """.trimIndent())
        assertEquals("8", output.trim())
    }

    @Test fun `function with string parameter accepts string`() {
        val output = executePhp("""
            <?php
            function greet(string ${'$'}name) {
                echo "Hello, " . ${'$'}name;
            }
            greet("World");
        """.trimIndent())
        assertEquals("Hello, World", output.trim())
    }

    @Test fun `function with bool parameter accepts boolean`() {
        val output = executePhp("""
            <?php
            function test(bool ${'$'}flag) {
                if (${'$'}flag) {
                    echo "true";
                } else {
                    echo "false";
                }
            }
            test(true);
        """.trimIndent())
        assertEquals("true", output.trim())
    }

    @Test fun `function with array parameter accepts array`() {
        val output = executePhp("""
            <?php
            function count_items(array ${'$'}items) {
                echo count(${'$'}items);
            }
            count_items([1, 2, 3]);
        """.trimIndent())
        assertEquals("3", output.trim())
    }

    @Test fun `function with nullable parameter accepts null`() {
        val output = executePhp("""
            <?php
            function maybe_echo(?string ${'$'}value) {
                if (${'$'}value == null) {
                    echo "null";
                } else {
                    echo ${'$'}value;
                }
            }
            maybe_echo(null);
        """.trimIndent())
        assertEquals("null", output.trim())
    }

    @Test fun `function with nullable parameter accepts value`() {
        val output = executePhp("""
            <?php
            function maybe_echo(?string ${'$'}value) {
                if (${'$'}value == null) {
                    echo "null";
                } else {
                    echo ${'$'}value;
                }
            }
            maybe_echo("test");
        """.trimIndent())
        assertEquals("test", output.trim())
    }

    @Test fun `function rejects wrong parameter type`() {
        val error = executePhpExpectingError("""
            <?php
            function add(int ${'$'}a, int ${'$'}b) {
                return ${'$'}a + ${'$'}b;
            }
            add("not", "numbers");
        """.trimIndent())
        assertTrue(error.contains("TypeError") || error.contains("type"))
    }

    @Test fun `function with mixed type accepts any value`() {
        val output = executePhp("""
            <?php
            function accept_anything(mixed ${'$'}value) {
                echo "ok";
            }
            accept_anything(42);
        """.trimIndent())
        assertEquals("ok", output.trim())
    }

    // ========== Return Type Tests ==========

    @Test fun `function with return type returns correct type`() {
        val output = executePhp("""
            <?php
            function get_number(): int {
                return 42;
            }
            echo get_number();
        """.trimIndent())
        assertEquals("42", output.trim())
    }

    @Test fun `function with string return type works`() {
        val output = executePhp("""
            <?php
            function get_name(): string {
                return "Alice";
            }
            echo get_name();
        """.trimIndent())
        assertEquals("Alice", output.trim())
    }

    @Test fun `function with bool return type works`() {
        val output = executePhp("""
            <?php
            function is_ready(): bool {
                return true;
            }
            if (is_ready()) {
                echo "yes";
            }
        """.trimIndent())
        assertEquals("yes", output.trim())
    }

    @Test fun `function with nullable return type can return null`() {
        val output = executePhp("""
            <?php
            function maybe_value(): ?string {
                return null;
            }
            ${'$'}result = maybe_value();
            if (${'$'}result == null) {
                echo "null";
            }
        """.trimIndent())
        assertEquals("null", output.trim())
    }

    // ========== Class Method Type Tests ==========

    @Test fun `class method with typed parameters works`() {
        val output = executePhp("""
            <?php
            class Calculator {
                public function add(int ${'$'}a, int ${'$'}b): int {
                    return ${'$'}a + ${'$'}b;
                }
            }
            ${'$'}calc = new Calculator();
            echo ${'$'}calc->add(10, 20);
        """.trimIndent())
        assertEquals("30", output.trim())
    }

    @Test fun `class method with return type works`() {
        val output = executePhp("""
            <?php
            class Greeter {
                public function greet(string ${'$'}name): string {
                    return "Hello, " . ${'$'}name;
                }
            }
            ${'$'}greeter = new Greeter();
            echo ${'$'}greeter->greet("Bob");
        """.trimIndent())
        assertEquals("Hello, Bob", output.trim())
    }

    @Test fun `class method with self return type works`() {
        val output = executePhp("""
            <?php
            class Builder {
                private ${'$'}value = 0;

                public function setValue(int ${'$'}v): self {
                    ${'$'}this->value = ${'$'}v;
                    return ${'$'}this;
                }

                public function getValue(): int {
                    return ${'$'}this->value;
                }
            }
            ${'$'}b = new Builder();
            echo ${'$'}b->setValue(42)->getValue();
        """.trimIndent())
        assertEquals("42", output.trim())
    }

    @Test fun `static method with typed parameters works`() {
        val output = executePhp("""
            <?php
            class Math {
                public static function multiply(int ${'$'}a, int ${'$'}b): int {
                    return ${'$'}a * ${'$'}b;
                }
            }
            echo Math::multiply(6, 7);
        """.trimIndent())
        assertEquals("42", output.trim())
    }

    // ========== Mixed Type Tests ==========

    @Test fun `function with multiple typed parameters works`() {
        val output = executePhp("""
            <?php
            function format_message(string ${'$'}name, int ${'$'}age, bool ${'$'}active): string {
                ${'$'}status = ${'$'}active ? "active" : "inactive";
                return ${'$'}name . " is " . ${'$'}age . " and " . ${'$'}status;
            }
            echo format_message("Alice", 25, true);
        """.trimIndent())
        assertEquals("Alice is 25 and active", output.trim())
    }

    @Test fun `function without types still works`() {
        val output = executePhp("""
            <?php
            function old_style(${'$'}a, ${'$'}b) {
                return ${'$'}a + ${'$'}b;
            }
            echo old_style(1, 2);
        """.trimIndent())
        assertEquals("3", output.trim())
    }

    @Test fun `mixing typed and untyped parameters works`() {
        val output = executePhp("""
            <?php
            function mixed_types(int ${'$'}num, ${'$'}any) {
                echo ${'$'}num . " and " . ${'$'}any;
            }
            mixed_types(42, "test");
        """.trimIndent())
        assertEquals("42 and test", output.trim())
    }
}
