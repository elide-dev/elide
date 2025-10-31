package dev.truffle.php

import kotlin.test.*

/**
 * Tests for PHP namespace support.
 */
class PhpNamespaceTest : AbstractPhpTest() {

    // ========== Namespace Declaration Tests ==========

    @Test fun `namespace declaration works`() {
        val output = executePhp("""
            <?php
            namespace MyApp;

            function greet() {
                echo "Hello";
            }

            greet();
        """.trimIndent())
        assertEquals("Hello", output.trim())
    }

    @Test fun `namespace with multiple segments works`() {
        val output = executePhp("""
            <?php
            namespace MyApp\Controllers\Admin;

            function test() {
                echo "works";
            }

            test();
        """.trimIndent())
        assertEquals("works", output.trim())
    }

    // ========== Use Statement Tests ==========

    @Test fun `use statement without alias works`() {
        val output = executePhp("""
            <?php
            namespace App;

            class User {
                public ${'$'}name = "Alice";
            }

            namespace Main;

            use App\User;

            ${'$'}user = new User();
            echo ${'$'}user->name;
        """.trimIndent())
        assertEquals("Alice", output.trim())
    }

    @Test fun `use statement with alias works`() {
        val output = executePhp("""
            <?php
            namespace App\Models;

            class User {
                public ${'$'}name = "Bob";
            }

            namespace Main;

            use App\Models\User as UserModel;

            ${'$'}user = new UserModel();
            echo ${'$'}user->name;
        """.trimIndent())
        assertEquals("Bob", output.trim())
    }

    // ========== Function Namespace Tests ==========

    @Test fun `namespaced function call works`() {
        val output = executePhp("""
            <?php
            namespace Utils;

            function helper() {
                echo "helper";
            }

            \Utils\helper();
        """.trimIndent())
        assertEquals("helper", output.trim())
    }

    @Test fun `use function statement works`() {
        val output = executePhp("""
            <?php
            namespace Utils;

            function calculate() {
                return 42;
            }

            namespace Main;

            use function Utils\calculate;

            echo calculate();
        """.trimIndent())
        assertEquals("42", output.trim())
    }

    @Test fun `use function with alias works`() {
        val output = executePhp("""
            <?php
            namespace Helpers;

            function process() {
                return "processed";
            }

            namespace Main;

            use function Helpers\process as helperProcess;

            echo helperProcess();
        """.trimIndent())
        assertEquals("processed", output.trim())
    }

    // ========== Class Namespace Tests ==========

    @Test fun `fully qualified class name works`() {
        val output = executePhp("""
            <?php
            namespace App\Models;

            class Product {
                public ${'$'}name = "Widget";
            }

            namespace Main;

            ${'$'}product = new \App\Models\Product();
            echo ${'$'}product->name;
        """.trimIndent())
        assertEquals("Widget", output.trim())
    }

    @Test fun `multiple namespaces in same file work`() {
        val output = executePhp("""
            <?php
            namespace First;

            function test() {
                echo "first";
            }

            namespace Second;

            function test() {
                echo "second";
            }

            test();
        """.trimIndent())
        assertEquals("second", output.trim())
    }

    @Test fun `calling function from different namespace works`() {
        val output = executePhp("""
            <?php
            namespace Utils;

            function helper() {
                echo "utils";
            }

            namespace App;

            \Utils\helper();
        """.trimIndent())
        assertEquals("utils", output.trim())
    }

    // ========== Global Namespace Tests ==========

    @Test fun `functions in global namespace work`() {
        val output = executePhp("""
            <?php
            function globalFunc() {
                echo "global";
            }

            namespace App;

            \globalFunc();
        """.trimIndent())
        assertEquals("global", output.trim())
    }

    @Test fun `calling global function from namespace works`() {
        val output = executePhp("""
            <?php
            function helper() {
                return 123;
            }

            namespace App;

            echo \helper();
        """.trimIndent())
        assertEquals("123", output.trim())
    }

    // ========== Built-in Function Tests ==========

    @Test fun `built-in functions work in namespaces`() {
        val output = executePhp("""
            <?php
            namespace MyApp;

            echo strlen("hello");
        """.trimIndent())
        assertEquals("5", output.trim())
    }

    @Test fun `built-in functions work with backslash prefix`() {
        val output = executePhp("""
            <?php
            namespace MyApp;

            echo \strlen("test");
        """.trimIndent())
        assertEquals("4", output.trim())
    }

    // ========== Mixed Tests ==========

    @Test fun `namespace with class and function works`() {
        val output = executePhp("""
            <?php
            namespace App\Services;

            class Calculator {
                public function add(${'$'}a, ${'$'}b) {
                    return ${'$'}a + ${'$'}b;
                }
            }

            function createCalculator() {
                return new Calculator();
            }

            ${'$'}calc = createCalculator();
            echo ${'$'}calc->add(10, 20);
        """.trimIndent())
        assertEquals("30", output.trim())
    }

    @Test fun `use with multiple imports works`() {
        val output = executePhp("""
            <?php
            namespace Models;

            class User {
                public ${'$'}type = "user";
            }

            class Admin {
                public ${'$'}type = "admin";
            }

            namespace Main;

            use Models\User;
            use Models\Admin;

            ${'$'}u = new User();
            ${'$'}a = new Admin();
            echo ${'$'}u->type . "-" . ${'$'}a->type;
        """.trimIndent())
        assertEquals("user-admin", output.trim())
    }
}
