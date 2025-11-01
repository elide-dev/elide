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
 * Tests for PHP trait declarations, usage, composition, and conflict resolution.
 *
 * Covers:
 * - Basic trait declaration and usage
 * - Multiple traits
 * - Conflict resolution (insteadof)
 * - Method aliasing (as)
 * - Visibility modifiers
 * - Properties in traits
 * - Static members
 * - Abstract methods
 * - Precedence rules
 * - Nested traits
 */
class PhpTraitTest : AbstractPhpTest() {
    @Test fun `basic trait with method`() {
        val output = executePhp("""
            <?php
            trait Greetable {
                public function greet() {
                    return "Hello";
                }
            }

            class Person {
                use Greetable;
            }

            ${'$'}p = new Person();
            echo ${'$'}p->greet();
        """.trimIndent())
        assertEquals("Hello", output.trim())
    }

    @Test fun `trait with multiple methods`() {
        val output = executePhp("""
            <?php
            trait Math {
                public function add(${'$'}a, ${'$'}b) {
                    return ${'$'}a + ${'$'}b;
                }

                public function multiply(${'$'}a, ${'$'}b) {
                    return ${'$'}a * ${'$'}b;
                }
            }

            class Calculator {
                use Math;
            }

            ${'$'}calc = new Calculator();
            echo ${'$'}calc->add(3, 4) . " ";
            echo ${'$'}calc->multiply(3, 4);
        """.trimIndent())
        assertEquals("7 12", output.trim())
    }

    @Test fun `class using multiple traits`() {
        val output = executePhp("""
            <?php
            trait A {
                public function methodA() {
                    return "A";
                }
            }

            trait B {
                public function methodB() {
                    return "B";
                }
            }

            class MyClass {
                use A, B;
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->methodA() . ${'$'}obj->methodB();
        """.trimIndent())
        assertEquals("AB", output.trim())
    }

    @Test fun `trait with property`() {
        val output = executePhp("""
            <?php
            trait HasName {
                public ${'$'}name = "Default";
            }

            class Person {
                use HasName;
            }

            ${'$'}p = new Person();
            echo ${'$'}p->name;
        """.trimIndent())
        assertEquals("Default", output.trim())
    }

    @Test fun `trait accessing this`() {
        val output = executePhp("""
            <?php
            trait HasGreeting {
                public function greet() {
                    return "Hello, " . ${'$'}this->name;
                }
            }

            class Person {
                use HasGreeting;
                public ${'$'}name;

                public function __construct(${'$'}name) {
                    ${'$'}this->name = ${'$'}name;
                }
            }

            ${'$'}p = new Person("Alice");
            echo ${'$'}p->greet();
        """.trimIndent())
        assertEquals("Hello, Alice", output.trim())
    }

    @Test fun `conflict resolution with insteadof`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "A";
                }
            }

            trait B {
                public function test() {
                    return "B";
                }
            }

            class MyClass {
                use A, B {
                    A::test insteadof B;
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test();
        """.trimIndent())
        assertEquals("A", output.trim())
    }

    @Test fun `method aliasing with as`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "Original";
                }
            }

            class MyClass {
                use A {
                    test as testAlias;
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test() . " ";
            echo ${'$'}obj->testAlias();
        """.trimIndent())
        assertEquals("Original Original", output.trim())
    }

    @Test fun `combined insteadof and aliasing`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "A";
                }
            }

            trait B {
                public function test() {
                    return "B";
                }
            }

            class MyClass {
                use A, B {
                    A::test insteadof B;
                    B::test as testB;
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test() . " ";
            echo ${'$'}obj->testB();
        """.trimIndent())
        assertEquals("A B", output.trim())
    }

    @Test fun `visibility change with as protected`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "Public";
                }
            }

            class MyClass {
                use A {
                    test as protected;
                }

                public function callTest() {
                    return ${'$'}this->test();
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->callTest();
        """.trimIndent())
        assertEquals("Public", output.trim())
    }

    @Test fun `visibility change with as private`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "Private";
                }
            }

            class MyClass {
                use A {
                    test as private;
                }

                public function callTest() {
                    return ${'$'}this->test();
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->callTest();
        """.trimIndent())
        assertEquals("Private", output.trim())
    }

    @Test fun `aliasing with visibility change`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "Test";
                }
            }

            class MyClass {
                use A {
                    test as private privateTest;
                }

                public function callPrivate() {
                    return ${'$'}this->privateTest();
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test() . " ";
            echo ${'$'}obj->callPrivate();
        """.trimIndent())
        assertEquals("Test Test", output.trim())
    }

    @Test fun `trait with static method`() {
        val output = executePhp("""
            <?php
            trait Singleton {
                public static function getInstance() {
                    return "Instance";
                }
            }

            class MyClass {
                use Singleton;
            }

            echo MyClass::getInstance();
        """.trimIndent())
        assertEquals("Instance", output.trim())
    }

    @Test fun `trait with static property`() {
        val output = executePhp("""
            <?php
            trait Counter {
                public static ${'$'}count = 0;

                public static function increment() {
                    self::${'$'}count++;
                }
            }

            class MyClass {
                use Counter;
            }

            MyClass::increment();
            MyClass::increment();
            echo MyClass::${'$'}count;
        """.trimIndent())
        assertEquals("2", output.trim())
    }

    @Test fun `trait with abstract method`() {
        val output = executePhp("""
            <?php
            trait Validator {
                abstract public function validate();

                public function check() {
                    return ${'$'}this->validate() ? "Valid" : "Invalid";
                }
            }

            class MyClass {
                use Validator;

                public function validate() {
                    return true;
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->check();
        """.trimIndent())
        assertEquals("Valid", output.trim())
    }

    @Test fun `class method overrides trait method`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "Trait";
                }
            }

            class MyClass {
                use A;

                public function test() {
                    return "Class";
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test();
        """.trimIndent())
        assertEquals("Class", output.trim())
    }

    @Test fun `trait method overrides parent method`() {
        val output = executePhp("""
            <?php
            class Parent {
                public function test() {
                    return "Parent";
                }
            }

            trait A {
                public function test() {
                    return "Trait";
                }
            }

            class Child extends Parent {
                use A;
            }

            ${'$'}obj = new Child();
            echo ${'$'}obj->test();
        """.trimIndent())
        assertEquals("Trait", output.trim())
    }

    @Test fun `nested traits - trait using trait`() {
        val output = executePhp("""
            <?php
            trait Base {
                public function base() {
                    return "Base";
                }
            }

            trait Extended {
                use Base;

                public function extended() {
                    return "Extended";
                }
            }

            class MyClass {
                use Extended;
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->base() . " ";
            echo ${'$'}obj->extended();
        """.trimIndent())
        assertEquals("Base Extended", output.trim())
    }

    @Test fun `multiple nested traits`() {
        val output = executePhp("""
            <?php
            trait A {
                public function a() {
                    return "A";
                }
            }

            trait B {
                public function b() {
                    return "B";
                }
            }

            trait C {
                use A, B;

                public function c() {
                    return "C";
                }
            }

            class MyClass {
                use C;
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->a() . ${'$'}obj->b() . ${'$'}obj->c();
        """.trimIndent())
        assertEquals("ABC", output.trim())
    }

    @Test fun `trait method calling another trait method`() {
        val output = executePhp("""
            <?php
            trait Helper {
                public function format(${'$'}text) {
                    return "[" . ${'$'}text . "]";
                }

                public function display(${'$'}text) {
                    return ${'$'}this->format(${'$'}text);
                }
            }

            class MyClass {
                use Helper;
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->display("Test");
        """.trimIndent())
        assertEquals("[Test]", output.trim())
    }

    @Test fun `multiple traits with properties`() {
        val output = executePhp("""
            <?php
            trait HasFirst {
                public ${'$'}first = "First";
            }

            trait HasSecond {
                public ${'$'}second = "Second";
            }

            class MyClass {
                use HasFirst, HasSecond;
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->first . " " . ${'$'}obj->second;
        """.trimIndent())
        assertEquals("First Second", output.trim())
    }

    @Test fun `trait with private method`() {
        val output = executePhp("""
            <?php
            trait Helper {
                private function internal() {
                    return "Internal";
                }

                public function public() {
                    return ${'$'}this->internal();
                }
            }

            class MyClass {
                use Helper;
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->public();
        """.trimIndent())
        assertEquals("Internal", output.trim())
    }

    @Test fun `trait with protected method`() {
        val output = executePhp("""
            <?php
            trait Helper {
                protected function internal() {
                    return "Protected";
                }
            }

            class MyClass {
                use Helper;

                public function access() {
                    return ${'$'}this->internal();
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->access();
        """.trimIndent())
        assertEquals("Protected", output.trim())
    }

    @Test fun `complex conflict resolution scenario`() {
        val output = executePhp("""
            <?php
            trait A {
                public function test() {
                    return "A";
                }

                public function unique() {
                    return "UniqueA";
                }
            }

            trait B {
                public function test() {
                    return "B";
                }
            }

            trait C {
                public function test() {
                    return "C";
                }
            }

            class MyClass {
                use A, B, C {
                    A::test insteadof B, C;
                    B::test as testB;
                    C::test as testC;
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test() . " ";
            echo ${'$'}obj->testB() . " ";
            echo ${'$'}obj->testC() . " ";
            echo ${'$'}obj->unique();
        """.trimIndent())
        assertEquals("A B C UniqueA", output.trim())
    }

    @Test fun `trait precedence full hierarchy`() {
        val output = executePhp("""
            <?php
            class GrandParent {
                public function test() {
                    return "GrandParent";
                }
            }

            class Parent extends GrandParent {
                public function other() {
                    return "Parent";
                }
            }

            trait MyTrait {
                public function test() {
                    return "Trait";
                }
            }

            class Child extends Parent {
                use MyTrait;

                public function test() {
                    return "Child";
                }
            }

            ${'$'}obj = new Child();
            echo ${'$'}obj->test();
        """.trimIndent())
        assertEquals("Child", output.trim())
    }

    @Test fun `empty trait`() {
        val output = executePhp("""
            <?php
            trait Empty {
            }

            class MyClass {
                use Empty;

                public function test() {
                    return "Works";
                }
            }

            ${'$'}obj = new MyClass();
            echo ${'$'}obj->test();
        """.trimIndent())
        assertEquals("Works", output.trim())
    }
}
