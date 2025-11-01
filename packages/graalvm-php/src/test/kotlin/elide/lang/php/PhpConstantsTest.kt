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
package elide.lang.php

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for PHP constants functionality.
 * Constants provide named, immutable values that can be accessed anywhere in the code.
 */
class PhpConstantsTest : AbstractPhpTest() {

    // ========== define() Function ==========

    @Test fun `define creates global constant`() {
        val output = executePhp("""
            <?php
            define('MY_CONSTANT', 42);
            echo MY_CONSTANT;
        """.trimIndent())
        assertEquals("42", output.trim())
    }

    @Test fun `define with string value`() {
        val output = executePhp("""
            <?php
            define('GREETING', 'Hello, World!');
            echo GREETING;
        """.trimIndent())
        assertEquals("Hello, World!", output.trim())
    }

    @Test fun `define with boolean value`() {
        val output = executePhp("""
            <?php
            define('IS_ENABLED', true);
            echo IS_ENABLED ? 'yes' : 'no';
        """.trimIndent())
        assertEquals("yes", output.trim())
    }

    @Test fun `define with array value`() {
        val output = executePhp("""
            <?php
            define('COLORS', ['red', 'green', 'blue']);
            echo COLORS[1];
        """.trimIndent())
        assertEquals("green", output.trim())
    }

    @Test fun `constants are case sensitive by default`() {
        val output = executePhp("""
            <?php
            define('TEST', 'uppercase');
            define('test', 'lowercase');
            echo TEST . ',' . test;
        """.trimIndent())
        assertEquals("uppercase,lowercase", output.trim())
    }

    @Test fun `constant used in function`() {
        val output = executePhp("""
            <?php
            define('MULTIPLIER', 10);
            function calculate(${'$'}x) {
                return ${'$'}x * MULTIPLIER;
            }
            echo calculate(5);
        """.trimIndent())
        assertEquals("50", output.trim())
    }

    @Test fun `constant used in class method`() {
        val output = executePhp("""
            <?php
            define('PREFIX', 'Item_');
            class Item {
                public function getName(${'$'}id) {
                    return PREFIX . ${'$'}id;
                }
            }
            ${'$'}item = new Item();
            echo ${'$'}item->getName(123);
        """.trimIndent())
        assertEquals("Item_123", output.trim())
    }

    // ========== const Keyword ==========

    @Test fun `const keyword creates constant`() {
        val output = executePhp("""
            <?php
            const MAX_SIZE = 100;
            echo MAX_SIZE;
        """.trimIndent())
        assertEquals("100", output.trim())
    }

    @Test fun `const with string expression`() {
        val output = executePhp("""
            <?php
            const SITE_NAME = 'My Website';
            echo SITE_NAME;
        """.trimIndent())
        assertEquals("My Website", output.trim())
    }

    @Test fun `const with arithmetic expression`() {
        val output = executePhp("""
            <?php
            const HOURS_PER_DAY = 24;
            const MINUTES_PER_HOUR = 60;
            const MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR;
            echo MINUTES_PER_DAY;
        """.trimIndent())
        assertEquals("1440", output.trim())
    }

    // ========== Class Constants ==========

    @Test fun `class constant definition and access`() {
        val output = executePhp("""
            <?php
            class Math {
                const PI = 3.14159;
            }
            echo Math::PI;
        """.trimIndent())
        assertEquals("3.14159", output.trim())
    }

    @Test fun `multiple class constants`() {
        val output = executePhp("""
            <?php
            class Status {
                const PENDING = 0;
                const APPROVED = 1;
                const REJECTED = 2;
            }
            echo Status::PENDING . ',' . Status::APPROVED . ',' . Status::REJECTED;
        """.trimIndent())
        assertEquals("0,1,2", output.trim())
    }

    @Test fun `class constant with visibility`() {
        val output = executePhp("""
            <?php
            class Config {
                public const PUBLIC_KEY = 'public';
                protected const PROTECTED_KEY = 'protected';
                private const PRIVATE_KEY = 'private';

                public function getKeys() {
                    return self::PUBLIC_KEY . ',' . self::PROTECTED_KEY . ',' . self::PRIVATE_KEY;
                }
            }
            ${'$'}c = new Config();
            echo ${'$'}c->getKeys();
        """.trimIndent())
        assertEquals("public,protected,private", output.trim())
    }

    @Test fun `access class constant from instance method`() {
        val output = executePhp("""
            <?php
            class Circle {
                const PI = 3.14;
                private ${'$'}radius;

                public function __construct(${'$'}r) {
                    ${'$'}this->radius = ${'$'}r;
                }

                public function area() {
                    return self::PI * ${'$'}this->radius * ${'$'}this->radius;
                }
            }
            ${'$'}c = new Circle(10);
            echo ${'$'}c->area();
        """.trimIndent())
        assertEquals("314", output.trim())
    }

    @Test fun `access parent class constant`() {
        val output = executePhp("""
            <?php
            class Base {
                const TYPE = 'base';
            }
            class Child extends Base {
                public function getType() {
                    return parent::TYPE;
                }
            }
            ${'$'}c = new Child();
            echo ${'$'}c->getType();
        """.trimIndent())
        assertEquals("base", output.trim())
    }

    @Test fun `override class constant in child class`() {
        val output = executePhp("""
            <?php
            class Animal {
                const LEGS = 4;
            }
            class Bird extends Animal {
                const LEGS = 2;
            }
            echo Animal::LEGS . ',' . Bird::LEGS;
        """.trimIndent())
        assertEquals("4,2", output.trim())
    }

    // ========== Magic Constants ==========

    @Test fun `__LINE__ magic constant`() {
        val output = executePhp("""
            <?php
            echo __LINE__;
        """.trimIndent())
        assertEquals("2", output.trim())
    }

    @Test fun `__FILE__ magic constant`() {
        val output = executePhp("""
            <?php
            echo basename(__FILE__);
        """.trimIndent())
        assertEquals("script.php", output.trim())
    }

    @Test fun `__DIR__ magic constant`() {
        val output = executePhp("""
            <?php
            echo __DIR__ !== '' ? 'has-value' : 'empty';
        """.trimIndent())
        assertEquals("has-value", output.trim())
    }

    @Test fun `__CLASS__ magic constant`() {
        val output = executePhp("""
            <?php
            class MyClass {
                public function getClassName() {
                    return __CLASS__;
                }
            }
            ${'$'}obj = new MyClass();
            echo ${'$'}obj->getClassName();
        """.trimIndent())
        assertEquals("MyClass", output.trim())
    }

    @Test fun `__METHOD__ magic constant`() {
        val output = executePhp("""
            <?php
            class Test {
                public function myMethod() {
                    return __METHOD__;
                }
            }
            ${'$'}t = new Test();
            echo ${'$'}t->myMethod();
        """.trimIndent())
        assertEquals("Test::myMethod", output.trim())
    }

    @Test fun `__FUNCTION__ magic constant`() {
        val output = executePhp("""
            <?php
            function testFunction() {
                return __FUNCTION__;
            }
            echo testFunction();
        """.trimIndent())
        assertEquals("testFunction", output.trim())
    }

    // ========== Edge Cases ==========

    @Test fun `constant in conditional expression`() {
        val output = executePhp("""
            <?php
            define('DEBUG', false);
            if (DEBUG) {
                echo 'debug';
            } else {
                echo 'production';
            }
        """.trimIndent())
        assertEquals("production", output.trim())
    }

    @Test fun `constant used in array`() {
        val output = executePhp("""
            <?php
            define('KEY', 'mykey');
            ${'$'}arr = [KEY => 'value'];
            echo ${'$'}arr['mykey'];
        """.trimIndent())
        assertEquals("value", output.trim())
    }

    @Test fun `constant used in string concatenation`() {
        val output = executePhp("""
            <?php
            define('NAME', 'John');
            echo 'Hello, ' . NAME . '!';
        """.trimIndent())
        assertEquals("Hello, John!", output.trim())
    }

    @Test fun `defined() function checks constant existence`() {
        val output = executePhp("""
            <?php
            define('EXISTS', 'yes');
            echo defined('EXISTS') ? 'true' : 'false';
            echo ',';
            echo defined('NOT_EXISTS') ? 'true' : 'false';
        """.trimIndent())
        assertEquals("true,false", output.trim())
    }

    @Test fun `constant_get() retrieves constant value`() {
        val output = executePhp("""
            <?php
            define('MY_CONST', 'test_value');
            echo constant('MY_CONST');
        """.trimIndent())
        assertEquals("test_value", output.trim())
    }
}
