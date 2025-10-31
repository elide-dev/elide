package dev.truffle.php

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for PHP magic constants (__FILE__, __DIR__, __LINE__, etc.).
 */
class PhpMagicConstantTest : AbstractPhpTest() {

    @Test fun `__FILE__ returns source path`() {
        val output = executePhp("""
            <?php
            echo __FILE__;
        """.trimIndent())
        // Should return a non-empty path
        assertTrue(output.trim().isNotEmpty(), "__ FILE__ should return source path")
    }

    @Test fun `__DIR__ returns source directory`() {
        val output = executePhp("""
            <?php
            echo __DIR__;
        """.trimIndent())
        // Should return a non-empty directory path
        assertTrue(output.trim().isNotEmpty(), "__DIR__ should return source directory")
    }

    @Test fun `__LINE__ is defined`() {
        // Note: __LINE__ returns 0 for now (requires lexer line tracking)
        val output = executePhp("""
            <?php
            echo __LINE__;
        """.trimIndent())
        assertEquals("0", output.trim())
    }

    @Test fun `__CLASS__ returns empty outside class`() {
        val output = executePhp("""
            <?php
            echo __CLASS__;
        """.trimIndent())
        assertEquals("", output.trim())
    }

    @Test fun `__CLASS__ returns class name inside class`() {
        val output = executePhp("""
            <?php
            class MyClass {
                public function test() {
                    echo __CLASS__;
                }
            }
            ${'$'}obj = new MyClass();
            ${'$'}obj->test();
        """.trimIndent())
        assertEquals("MyClass", output.trim())
    }

    @Test fun `__METHOD__ returns empty outside method`() {
        val output = executePhp("""
            <?php
            echo __METHOD__;
        """.trimIndent())
        assertEquals("", output.trim())
    }

    @Test fun `__METHOD__ returns class and method name`() {
        val output = executePhp("""
            <?php
            class TestClass {
                public function myMethod() {
                    echo __METHOD__;
                }
            }
            ${'$'}obj = new TestClass();
            ${'$'}obj->myMethod();
        """.trimIndent())
        assertEquals("TestClass::myMethod", output.trim())
    }

    @Test fun `__FUNCTION__ returns empty outside function`() {
        val output = executePhp("""
            <?php
            echo __FUNCTION__;
        """.trimIndent())
        assertEquals("", output.trim())
    }

    @Test fun `__FUNCTION__ returns function name`() {
        val output = executePhp("""
            <?php
            function myFunction() {
                echo __FUNCTION__;
            }
            myFunction();
        """.trimIndent())
        assertEquals("myFunction", output.trim())
    }

    @Test fun `__FUNCTION__ returns method name in class`() {
        val output = executePhp("""
            <?php
            class TestClass {
                public function myMethod() {
                    echo __FUNCTION__;
                }
            }
            ${'$'}obj = new TestClass();
            ${'$'}obj->myMethod();
        """.trimIndent())
        assertEquals("myMethod", output.trim())
    }

    @Test fun `__NAMESPACE__ returns empty in global namespace`() {
        val output = executePhp("""
            <?php
            echo __NAMESPACE__;
        """.trimIndent())
        assertEquals("", output.trim())
    }

    @Test fun `__NAMESPACE__ returns namespace name`() {
        val output = executePhp("""
            <?php
            namespace MyApp\Utils;
            echo __NAMESPACE__;
        """.trimIndent())
        assertEquals("MyApp\\Utils", output.trim())
    }

    @Test fun `__NAMESPACE__ works in functions`() {
        val output = executePhp("""
            <?php
            namespace MyApp\Services;

            function helper() {
                echo __NAMESPACE__;
            }

            helper();
        """.trimIndent())
        assertEquals("MyApp\\Services", output.trim())
    }

    @Test fun `__TRAIT__ returns empty (traits not yet implemented)`() {
        val output = executePhp("""
            <?php
            echo __TRAIT__;
        """.trimIndent())
        assertEquals("", output.trim())
    }

    @Test fun `magic constants can be used in expressions`() {
        val output = executePhp("""
            <?php
            class Test {
                public function show() {
                    echo "Class: " . __CLASS__ . ", Method: " . __METHOD__;
                }
            }
            ${'$'}t = new Test();
            ${'$'}t->show();
        """.trimIndent())
        assertEquals("Class: Test, Method: Test::show", output.trim())
    }

    @Test fun `magic constants work in nested function calls`() {
        val output = executePhp("""
            <?php
            function outer() {
                echo __FUNCTION__;
            }

            function middle() {
                outer();
            }

            middle();
        """.trimIndent())
        assertEquals("outer", output.trim())
    }
}
