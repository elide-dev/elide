package dev.truffle.php

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import kotlin.test.*
import java.io.ByteArrayOutputStream

class TrufflePhpTest {
  @Test fun `simple echo statement works`() {
    val output = executePhp("<?php echo \"Hello World\";")
    assertEquals("Hello World", output.trim())
  }

  @Test fun `integer arithmetic works`() {
    val output = executePhp("<?php echo 5 + 3;")
    assertEquals("8", output.trim())
  }

  @Test fun `string concatenation works`() {
    val output = executePhp("<?php echo \"Hello\" . \" \" . \"World\";")
    assertEquals("Hello World", output.trim())
  }

  @Test fun `variable assignment and read works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 42;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `if statement with true condition works`() {
    val output = executePhp("""
      <?php
      if (true) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `if else statement works`() {
    val output = executePhp("""
      <?php
      if (false) {
        echo "no";
      } else {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `comparison operators work`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      if (${'$'}x == 5) {
        echo "equal";
      }
    """.trimIndent())
    assertEquals("equal", output.trim())
  }

  // Function tests
  @Test fun `simple function definition and call works`() {
    val output = executePhp("""
      <?php
      function greet() {
        echo "Hello";
      }
      greet();
    """.trimIndent())
    assertEquals("Hello", output.trim())
  }

  @Test fun `function with parameters works`() {
    val output = executePhp("""
      <?php
      function add(${'$'}a, ${'$'}b) {
        echo ${'$'}a + ${'$'}b;
      }
      add(3, 4);
    """.trimIndent())
    assertEquals("7", output.trim())
  }

  @Test fun `function with return value works`() {
    val output = executePhp("""
      <?php
      function multiply(${'$'}x, ${'$'}y) {
        return ${'$'}x * ${'$'}y;
      }
      ${'$'}result = multiply(6, 7);
      echo ${'$'}result;
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `function calling another function works`() {
    val output = executePhp("""
      <?php
      function double(${'$'}n) {
        return ${'$'}n * 2;
      }
      function quadruple(${'$'}n) {
        return double(double(${'$'}n));
      }
      echo quadruple(5);
    """.trimIndent())
    assertEquals("20", output.trim())
  }

  @Test fun `function with no return returns null`() {
    val output = executePhp("""
      <?php
      function noReturn() {
        ${'$'}x = 5;
      }
      ${'$'}result = noReturn();
      if (${'$'}result == null) {
        echo "null";
      }
    """.trimIndent())
    assertEquals("null", output.trim())
  }

  // Array tests
  @Test fun `indexed array literal works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      echo ${'$'}arr[0];
      echo ${'$'}arr[1];
      echo ${'$'}arr[2];
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  @Test fun `associative array works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = ["name" => "John", "age" => 30];
      echo ${'$'}arr["name"];
      echo ${'$'}arr["age"];
    """.trimIndent())
    assertEquals("John30", output.trim())
  }

  @Test fun `array element assignment works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      ${'$'}arr[1] = 99;
      echo ${'$'}arr[0];
      echo ${'$'}arr[1];
      echo ${'$'}arr[2];
    """.trimIndent())
    assertEquals("1993", output.trim())
  }

  @Test fun `array append with empty brackets works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2];
      ${'$'}arr[] = 3;
      echo ${'$'}arr[0];
      echo ${'$'}arr[1];
      echo ${'$'}arr[2];
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  @Test fun `empty array works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [];
      ${'$'}arr[0] = "hello";
      echo ${'$'}arr[0];
    """.trimIndent())
    assertEquals("hello", output.trim())
  }

  // Operator tests
  @Test fun `greater than operator works`() {
    val output = executePhp("""
      <?php
      if (10 > 5) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `less than or equal operator works`() {
    val output = executePhp("""
      <?php
      if (5 <= 5) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `greater than or equal operator works`() {
    val output = executePhp("""
      <?php
      if (6 >= 5) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `not equal operator works`() {
    val output = executePhp("""
      <?php
      if (5 != 6) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `logical AND operator works`() {
    val output = executePhp("""
      <?php
      if (true && true) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `logical OR operator works`() {
    val output = executePhp("""
      <?php
      if (false || true) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `logical NOT operator works`() {
    val output = executePhp("""
      <?php
      if (!false) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `complex logical expression works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 10;
      if ((${'$'}x > 5 && ${'$'}x < 20) || ${'$'}x == 0) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  // Loop tests
  @Test fun `while loop works`() {
    val output = executePhp("""
      <?php
      ${'$'}i = 0;
      while (${'$'}i < 3) {
        echo ${'$'}i;
        ${'$'}i = ${'$'}i + 1;
      }
    """.trimIndent())
    assertEquals("012", output.trim())
  }

  @Test fun `for loop works`() {
    val output = executePhp("""
      <?php
      for (${'$'}i = 0; ${'$'}i < 3; ${'$'}i = ${'$'}i + 1) {
        echo ${'$'}i;
      }
    """.trimIndent())
    assertEquals("012", output.trim())
  }

  @Test fun `foreach loop with indexed array works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      foreach (${'$'}arr as ${'$'}value) {
        echo ${'$'}value;
      }
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  @Test fun `foreach loop with key and value works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = ["a" => 1, "b" => 2];
      foreach (${'$'}arr as ${'$'}key => ${'$'}value) {
        echo ${'$'}key;
        echo ${'$'}value;
      }
    """.trimIndent())
    assertEquals("a1b2", output.trim())
  }

  @Test fun `nested loops work`() {
    val output = executePhp("""
      <?php
      for (${'$'}i = 0; ${'$'}i < 2; ${'$'}i = ${'$'}i + 1) {
        for (${'$'}j = 0; ${'$'}j < 2; ${'$'}j = ${'$'}j + 1) {
          echo ${'$'}i;
          echo ${'$'}j;
        }
      }
    """.trimIndent())
    assertEquals("00011011", output.trim())
  }

  // Increment/Decrement operator tests
  @Test fun `pre-increment operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      echo ++${'$'}x;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("66", output.trim())
  }

  @Test fun `post-increment operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      echo ${'$'}x++;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("56", output.trim())
  }

  @Test fun `pre-decrement operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      echo --${'$'}x;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("44", output.trim())
  }

  @Test fun `post-decrement operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      echo ${'$'}x--;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("54", output.trim())
  }

  @Test fun `increment in assignment works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 10;
      ${'$'}y = ++${'$'}x;
      echo ${'$'}y;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("1111", output.trim())
  }

  @Test fun `post-increment in assignment works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 10;
      ${'$'}y = ${'$'}x++;
      echo ${'$'}y;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("1011", output.trim())
  }

  @Test fun `increment in conditional works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 0;
      if (++${'$'}x == 1) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `post-increment in conditional works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 0;
      if (${'$'}x++ == 0) {
        echo "yes";
      }
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("yes1", output.trim())
  }

  @Test fun `increment and decrement with null works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = null;
      ++${'$'}x;
      echo ${'$'}x;
      ${'$'}y = null;
      ${'$'}z = ${'$'}y++;
      if (${'$'}z == null) {
        echo "null";
      }
      echo ${'$'}y;
    """.trimIndent())
    assertEquals("1null1", output.trim())
  }

  // Break and Continue tests
  @Test fun `break in while loop works`() {
    val output = executePhp("""
      <?php
      ${'$'}i = 0;
      while (${'$'}i < 10) {
        if (${'$'}i == 3) {
          break;
        }
        echo ${'$'}i;
        ${'$'}i = ${'$'}i + 1;
      }
    """.trimIndent())
    assertEquals("012", output.trim())
  }

  @Test fun `break in for loop works`() {
    val output = executePhp("""
      <?php
      for (${'$'}i = 0; ${'$'}i < 10; ${'$'}i = ${'$'}i + 1) {
        if (${'$'}i == 4) {
          break;
        }
        echo ${'$'}i;
      }
    """.trimIndent())
    assertEquals("0123", output.trim())
  }

  @Test fun `break in foreach loop works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3, 4, 5];
      foreach (${'$'}arr as ${'$'}val) {
        if (${'$'}val == 4) {
          break;
        }
        echo ${'$'}val;
      }
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  @Test fun `continue in while loop works`() {
    val output = executePhp("""
      <?php
      ${'$'}i = 0;
      while (${'$'}i < 5) {
        ${'$'}i = ${'$'}i + 1;
        if (${'$'}i == 3) {
          continue;
        }
        echo ${'$'}i;
      }
    """.trimIndent())
    assertEquals("1245", output.trim())
  }

  @Test fun `continue in for loop works`() {
    val output = executePhp("""
      <?php
      for (${'$'}i = 0; ${'$'}i < 5; ${'$'}i = ${'$'}i + 1) {
        if (${'$'}i == 2) {
          continue;
        }
        echo ${'$'}i;
      }
    """.trimIndent())
    assertEquals("0134", output.trim())
  }

  @Test fun `continue in foreach loop works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3, 4, 5];
      foreach (${'$'}arr as ${'$'}val) {
        if (${'$'}val == 3) {
          continue;
        }
        echo ${'$'}val;
      }
    """.trimIndent())
    assertEquals("1245", output.trim())
  }

  // Built-in function tests - String functions
  @Test fun `strlen function works`() {
    val output = executePhp("""
      <?php
      echo strlen("hello");
      echo strlen("");
    """.trimIndent())
    assertEquals("50", output.trim())
  }

  @Test fun `strtolower and strtoupper work`() {
    val output = executePhp("""
      <?php
      echo strtolower("HELLO");
      echo strtoupper("world");
    """.trimIndent())
    assertEquals("helloWORLD", output.trim())
  }

  @Test fun `substr function works`() {
    val output = executePhp("""
      <?php
      echo substr("hello", 1, 3);
      echo substr("world", 0, 2);
    """.trimIndent())
    assertEquals("ellwo", output.trim())
  }

  @Test fun `str_replace function works`() {
    val output = executePhp("""
      <?php
      echo str_replace("world", "PHP", "hello world");
    """.trimIndent())
    assertEquals("hello PHP", output.trim())
  }

  @Test fun `explode and implode work`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = explode(",", "a,b,c");
      echo ${'$'}arr[0];
      echo ${'$'}arr[1];
      echo implode("-", ${'$'}arr);
    """.trimIndent())
    assertEquals("aba-b-c", output.trim())
  }

  // Built-in function tests - Array functions
  @Test fun `count function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      echo count(${'$'}arr);
      echo count([]);
    """.trimIndent())
    assertEquals("30", output.trim())
  }

  @Test fun `array_push function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2];
      array_push(${'$'}arr, 3, 4);
      echo ${'$'}arr[2];
      echo ${'$'}arr[3];
    """.trimIndent())
    assertEquals("34", output.trim())
  }

  @Test fun `in_array function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      if (in_array(2, ${'$'}arr)) {
        echo "yes";
      }
      if (in_array(5, ${'$'}arr)) {
        echo "no";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `array_keys and array_values work`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = ["a" => 1, "b" => 2];
      ${'$'}keys = array_keys(${'$'}arr);
      ${'$'}vals = array_values(${'$'}arr);
      echo ${'$'}keys[0];
      echo ${'$'}vals[1];
    """.trimIndent())
    assertEquals("a2", output.trim())
  }

  @Test fun `array_slice function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3, 4, 5];
      ${'$'}slice = array_slice(${'$'}arr, 1, 3);
      echo count(${'$'}slice);
      echo ${'$'}slice[0];
      echo ${'$'}slice[1];
      echo ${'$'}slice[2];
    """.trimIndent())
    assertEquals("3234", output.trim())
  }

  @Test fun `array_slice with negative offset works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3, 4, 5];
      ${'$'}slice = array_slice(${'$'}arr, -2);
      echo ${'$'}slice[0];
      echo ${'$'}slice[1];
    """.trimIndent())
    assertEquals("45", output.trim())
  }

  @Test fun `array_reverse function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3, 4];
      ${'$'}rev = array_reverse(${'$'}arr);
      echo ${'$'}rev[0];
      echo ${'$'}rev[1];
      echo ${'$'}rev[2];
      echo ${'$'}rev[3];
    """.trimIndent())
    assertEquals("4321", output.trim())
  }

  @Test fun `array_search function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = ["apple", "banana", "cherry"];
      ${'$'}key = array_search("banana", ${'$'}arr);
      echo ${'$'}key;
    """.trimIndent())
    assertEquals("1", output.trim())
  }

  @Test fun `array_search returns false when not found`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      ${'$'}result = array_search(5, ${'$'}arr);
      if (${'$'}result == false) {
        echo "not found";
      }
    """.trimIndent())
    assertEquals("not found", output.trim())
  }

  @Test fun `array_key_exists function works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = ["name" => "John", "age" => 30];
      if (array_key_exists("name", ${'$'}arr)) {
        echo "yes";
      }
      if (array_key_exists("email", ${'$'}arr)) {
        echo "no";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  // Built-in function tests - Type functions
  @Test fun `is_array function works`() {
    val output = executePhp("""
      <?php
      if (is_array([1, 2])) {
        echo "yes";
      }
      if (is_array("test")) {
        echo "no";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `is_string and is_int work`() {
    val output = executePhp("""
      <?php
      if (is_string("hello")) {
        echo "str";
      }
      if (is_int(42)) {
        echo "int";
      }
    """.trimIndent())
    assertEquals("strint", output.trim())
  }

  @Test fun `gettype function works`() {
    val output = executePhp("""
      <?php
      echo gettype(42);
      echo gettype("test");
      echo gettype([1, 2]);
    """.trimIndent())
    assertEquals("integerstringarray", output.trim())
  }

  // Built-in function tests - Math functions
  @Test fun `abs function works`() {
    val output = executePhp("""
      <?php
      echo abs(-5);
      echo abs(3);
    """.trimIndent())
    assertEquals("53", output.trim())
  }

  @Test fun `max and min functions work`() {
    val output = executePhp("""
      <?php
      echo max(1, 5, 3);
      echo min(1, 5, 3);
    """.trimIndent())
    assertEquals("51", output.trim())
  }

  // Class and Object tests
  @Test fun `simple class definition and instantiation works`() {
    val output = executePhp("""
      <?php
      class Person {
        public ${'$'}name;
      }
      ${'$'}p = new Person();
      echo "created";
    """.trimIndent())
    assertEquals("created", output.trim())
  }

  @Test fun `class with constructor works`() {
    val output = executePhp("""
      <?php
      class Person {
        public ${'$'}name;
        public ${'$'}age;

        function __construct(${'$'}n, ${'$'}a) {
          ${'$'}this->name = ${'$'}n;
          ${'$'}this->age = ${'$'}a;
        }
      }
      ${'$'}p = new Person("John", 30);
      echo ${'$'}p->name;
      echo ${'$'}p->age;
    """.trimIndent())
    assertEquals("John30", output.trim())
  }

  @Test fun `property access from outside class works`() {
    val output = executePhp("""
      <?php
      class Box {
        public ${'$'}value;
      }
      ${'$'}box = new Box();
      ${'$'}box->value = 42;
      echo ${'$'}box->value;
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `method with no parameters works`() {
    val output = executePhp("""
      <?php
      class Greeter {
        function sayHello() {
          return "Hello";
        }
      }
      ${'$'}g = new Greeter();
      echo ${'$'}g->sayHello();
    """.trimIndent())
    assertEquals("Hello", output.trim())
  }

  @Test fun `method with parameters works`() {
    val output = executePhp("""
      <?php
      class Calculator {
        function add(${'$'}a, ${'$'}b) {
          return ${'$'}a + ${'$'}b;
        }
      }
      ${'$'}calc = new Calculator();
      echo ${'$'}calc->add(5, 7);
    """.trimIndent())
    assertEquals("12", output.trim())
  }

  @Test fun `method accessing properties with this works`() {
    val output = executePhp("""
      <?php
      class Counter {
        public ${'$'}count;

        function __construct() {
          ${'$'}this->count = 0;
        }

        function increment() {
          ${'$'}this->count = ${'$'}this->count + 1;
          return ${'$'}this->count;
        }
      }
      ${'$'}c = new Counter();
      echo ${'$'}c->increment();
      echo ${'$'}c->increment();
      echo ${'$'}c->count;
    """.trimIndent())
    assertEquals("122", output.trim())
  }

  @Test fun `method calling another method works`() {
    val output = executePhp("""
      <?php
      class Math {
        function double(${'$'}n) {
          return ${'$'}n * 2;
        }

        function quadruple(${'$'}n) {
          return ${'$'}this->double(${'$'}this->double(${'$'}n));
        }
      }
      ${'$'}m = new Math();
      echo ${'$'}m->quadruple(3);
    """.trimIndent())
    assertEquals("12", output.trim())
  }

  @Test fun `property with default value works`() {
    val output = executePhp("""
      <?php
      class Config {
        public ${'$'}timeout = 30;
        public ${'$'}retries = 3;
      }
      ${'$'}cfg = new Config();
      echo ${'$'}cfg->timeout;
      echo ${'$'}cfg->retries;
    """.trimIndent())
    assertEquals("303", output.trim())
  }

  @Test fun `constructor overrides default values`() {
    val output = executePhp("""
      <?php
      class Settings {
        public ${'$'}value = 10;

        function __construct(${'$'}v) {
          ${'$'}this->value = ${'$'}v;
        }
      }
      ${'$'}s = new Settings(99);
      echo ${'$'}s->value;
    """.trimIndent())
    assertEquals("99", output.trim())
  }

  @Test fun `multiple objects of same class are independent`() {
    val output = executePhp("""
      <?php
      class Point {
        public ${'$'}x;
        public ${'$'}y;

        function __construct(${'$'}x, ${'$'}y) {
          ${'$'}this->x = ${'$'}x;
          ${'$'}this->y = ${'$'}y;
        }
      }
      ${'$'}p1 = new Point(1, 2);
      ${'$'}p2 = new Point(3, 4);
      echo ${'$'}p1->x;
      echo ${'$'}p1->y;
      echo ${'$'}p2->x;
      echo ${'$'}p2->y;
    """.trimIndent())
    assertEquals("1234", output.trim())
  }

  @Test fun `method with no explicit return returns null`() {
    val output = executePhp("""
      <?php
      class Test {
        function noReturn() {
          ${'$'}x = 5;
        }
      }
      ${'$'}t = new Test();
      ${'$'}result = ${'$'}t->noReturn();
      if (${'$'}result == null) {
        echo "null";
      }
    """.trimIndent())
    assertEquals("null", output.trim())
  }

  @Test fun `method modifying property persists across calls`() {
    val output = executePhp("""
      <?php
      class Accumulator {
        public ${'$'}total;

        function __construct() {
          ${'$'}this->total = 0;
        }

        function add(${'$'}n) {
          ${'$'}this->total = ${'$'}this->total + ${'$'}n;
        }
      }
      ${'$'}acc = new Accumulator();
      ${'$'}acc->add(5);
      ${'$'}acc->add(3);
      ${'$'}acc->add(2);
      echo ${'$'}acc->total;
    """.trimIndent())
    assertEquals("10", output.trim())
  }

  @Test fun `class with multiple methods works`() {
    val output = executePhp("""
      <?php
      class String {
        public ${'$'}value;

        function __construct(${'$'}v) {
          ${'$'}this->value = ${'$'}v;
        }

        function upper() {
          return strtoupper(${'$'}this->value);
        }

        function lower() {
          return strtolower(${'$'}this->value);
        }

        function length() {
          return strlen(${'$'}this->value);
        }
      }
      ${'$'}s = new String("Hello");
      echo ${'$'}s->upper();
      echo ${'$'}s->lower();
      echo ${'$'}s->length();
    """.trimIndent())
    assertEquals("HELLOhello5", output.trim())
  }

  @Test fun `class property accessed in conditional works`() {
    val output = executePhp("""
      <?php
      class User {
        public ${'$'}age;

        function __construct(${'$'}a) {
          ${'$'}this->age = ${'$'}a;
        }

        function isAdult() {
          if (${'$'}this->age >= 18) {
            return true;
          }
          return false;
        }
      }
      ${'$'}u = new User(21);
      if (${'$'}u->isAdult()) {
        echo "adult";
      }
    """.trimIndent())
    assertEquals("adult", output.trim())
  }

  @Test fun `property assignment in method works`() {
    val output = executePhp("""
      <?php
      class Name {
        public ${'$'}first;
        public ${'$'}last;

        function setName(${'$'}f, ${'$'}l) {
          ${'$'}this->first = ${'$'}f;
          ${'$'}this->last = ${'$'}l;
        }

        function fullName() {
          return ${'$'}this->first . " " . ${'$'}this->last;
        }
      }
      ${'$'}n = new Name();
      ${'$'}n->setName("John", "Doe");
      echo ${'$'}n->fullName();
    """.trimIndent())
    assertEquals("John Doe", output.trim())
  }

  @Test fun `method returning property works`() {
    val output = executePhp("""
      <?php
      class Container {
        public ${'$'}data;

        function __construct(${'$'}d) {
          ${'$'}this->data = ${'$'}d;
        }

        function getData() {
          return ${'$'}this->data;
        }
      }
      ${'$'}c = new Container("secret");
      echo ${'$'}c->getData();
    """.trimIndent())
    assertEquals("secret", output.trim())
  }

  @Test fun `constructor with no parameters works`() {
    val output = executePhp("""
      <?php
      class Logger {
        public ${'$'}initialized;

        function __construct() {
          ${'$'}this->initialized = true;
        }
      }
      ${'$'}log = new Logger();
      if (${'$'}log->initialized) {
        echo "ready";
      }
    """.trimIndent())
    assertEquals("ready", output.trim())
  }

  // Tier 1 Feature Tests - Ternary Operator
  @Test fun `basic ternary operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      echo ${'$'}x > 3 ? "yes" : "no";
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `ternary operator with false condition works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 1;
      echo ${'$'}x > 3 ? "yes" : "no";
    """.trimIndent())
    assertEquals("no", output.trim())
  }

  @Test fun `nested ternary operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      echo ${'$'}x > 10 ? "big" : ${'$'}x > 3 ? "medium" : "small";
    """.trimIndent())
    assertEquals("medium", output.trim())
  }

  @Test fun `ternary operator with different types works`() {
    val output = executePhp("""
      <?php
      ${'$'}flag = true;
      ${'$'}result = ${'$'}flag ? 42 : "no";
      echo ${'$'}result;
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  // Tier 1 Feature Tests - Null Coalescing Operator
  @Test fun `null coalescing with null value works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = null;
      echo ${'$'}x ?? "default";
    """.trimIndent())
    assertEquals("default", output.trim())
  }

  @Test fun `null coalescing with non-null value works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = "value";
      echo ${'$'}x ?? "default";
    """.trimIndent())
    assertEquals("value", output.trim())
  }

  @Test fun `chained null coalescing works`() {
    val output = executePhp("""
      <?php
      ${'$'}a = null;
      ${'$'}b = null;
      ${'$'}c = "found";
      echo ${'$'}a ?? ${'$'}b ?? ${'$'}c ?? "fallback";
    """.trimIndent())
    assertEquals("found", output.trim())
  }

  @Test fun `null coalescing with zero does not trigger`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 0;
      echo ${'$'}x ?? "default";
    """.trimIndent())
    assertEquals("0", output.trim())
  }

  // Tier 1 Feature Tests - Switch Statement
  @Test fun `basic switch statement works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 2;
      switch (${'$'}x) {
        case 1:
          echo "one";
          break;
        case 2:
          echo "two";
          break;
        case 3:
          echo "three";
          break;
      }
    """.trimIndent())
    assertEquals("two", output.trim())
  }

  @Test fun `switch statement with fall-through works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 2;
      switch (${'$'}x) {
        case 1:
          echo "one";
        case 2:
          echo "two";
        case 3:
          echo "three";
      }
    """.trimIndent())
    assertEquals("twothree", output.trim())
  }

  @Test fun `switch statement with default case works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      switch (${'$'}x) {
        case 1:
          echo "one";
          break;
        case 2:
          echo "two";
          break;
        default:
          echo "other";
      }
    """.trimIndent())
    assertEquals("other", output.trim())
  }

  @Test fun `switch statement with string cases works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = "hello";
      switch (${'$'}x) {
        case "hi":
          echo "greeting1";
          break;
        case "hello":
          echo "greeting2";
          break;
        default:
          echo "unknown";
      }
    """.trimIndent())
    assertEquals("greeting2", output.trim())
  }

  // Tier 1 Feature Tests - Language Constructs
  @Test fun `isset with set variable returns true`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      if (isset(${'$'}x)) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `isset with null variable returns false`() {
    val output = executePhp("""
      <?php
      ${'$'}x = null;
      if (isset(${'$'}x)) {
        echo "yes";
      } else {
        echo "no";
      }
    """.trimIndent())
    assertEquals("no", output.trim())
  }

  @Test fun `isset with multiple variables works`() {
    val output = executePhp("""
      <?php
      ${'$'}a = 1;
      ${'$'}b = 2;
      ${'$'}c = null;
      if (isset(${'$'}a, ${'$'}b)) {
        echo "yes";
      }
      if (isset(${'$'}a, ${'$'}c)) {
        echo "fail";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `empty with empty values returns true`() {
    val output = executePhp("""
      <?php
      ${'$'}a = 0;
      ${'$'}b = "";
      ${'$'}c = null;
      if (empty(${'$'}a)) echo "1";
      if (empty(${'$'}b)) echo "2";
      if (empty(${'$'}c)) echo "3";
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  @Test fun `empty with non-empty value returns false`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 42;
      if (empty(${'$'}x)) {
        echo "empty";
      } else {
        echo "not empty";
      }
    """.trimIndent())
    assertEquals("not empty", output.trim())
  }

  @Test fun `unset removes variable`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      unset(${'$'}x);
      if (isset(${'$'}x)) {
        echo "fail";
      } else {
        echo "removed";
      }
    """.trimIndent())
    assertEquals("removed", output.trim())
  }

  // Tier 1 Feature Tests - String Interpolation
  @Test fun `simple string interpolation works`() {
    val output = executePhp("""
      <?php
      ${'$'}name = "World";
      echo "Hello ${'$'}name!";
    """.trimIndent())
    assertEquals("Hello World!", output.trim())
  }

  @Test fun `string interpolation with multiple variables works`() {
    val output = executePhp("""
      <?php
      ${'$'}first = "John";
      ${'$'}last = "Doe";
      echo "Name: ${'$'}first ${'$'}last";
    """.trimIndent())
    assertEquals("Name: John Doe", output.trim())
  }

  @Test fun `string interpolation with numbers works`() {
    val output = executePhp("""
      <?php
      ${'$'}count = 42;
      echo "Count: ${'$'}count";
    """.trimIndent())
    assertEquals("Count: 42", output.trim())
  }

  @Test fun `single quoted string does not interpolate`() {
    val output = executePhp("""
      <?php
      ${'$'}name = "World";
      echo 'Hello ${'$'}name!';
    """.trimIndent())
    assertEquals("Hello \$name!", output.trim())
  }

  @Test fun `escaped dollar sign in double quoted string works`() {
    val output = executePhp("""
      <?php
      ${'$'}price = 10;
      echo "Price: \$${'$'}price";
    """.trimIndent())
    assertEquals("Price: \$10", output.trim())
  }

  @Test fun `string interpolation at string boundaries works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = "start";
      ${'$'}y = "end";
      echo "${'$'}x-middle-${'$'}y";
    """.trimIndent())
    assertEquals("start-middle-end", output.trim())
  }

  // Tier 1 Feature Tests - Static Class Members & Methods
  @Test fun `static property with default value works`() {
    val output = executePhp("""
      <?php
      class Counter {
        public static ${'$'}count = 0;
      }
      echo Counter::${'$'}count;
    """.trimIndent())
    assertEquals("0", output.trim())
  }

  @Test fun `static property assignment works`() {
    val output = executePhp("""
      <?php
      class Config {
        public static ${'$'}version = 1;
      }
      Config::${'$'}version = 2;
      echo Config::${'$'}version;
    """.trimIndent())
    assertEquals("2", output.trim())
  }

  @Test fun `static method call works`() {
    val output = executePhp("""
      <?php
      class Math {
        public static function add(${'$'}a, ${'$'}b) {
          return ${'$'}a + ${'$'}b;
        }
      }
      echo Math::add(5, 3);
    """.trimIndent())
    assertEquals("8", output.trim())
  }

  @Test fun `static property persists across accesses`() {
    val output = executePhp("""
      <?php
      class Database {
        public static ${'$'}connections = 0;
      }
      Database::${'$'}connections = Database::${'$'}connections + 1;
      Database::${'$'}connections = Database::${'$'}connections + 1;
      echo Database::${'$'}connections;
    """.trimIndent())
    assertEquals("2", output.trim())
  }

  @Test fun `static method without parameters works`() {
    val output = executePhp("""
      <?php
      class Utils {
        public static function greet() {
          return "Hello";
        }
      }
      echo Utils::greet();
    """.trimIndent())
    assertEquals("Hello", output.trim())
  }

  @Test fun `multiple static properties work independently`() {
    val output = executePhp("""
      <?php
      class Settings {
        public static ${'$'}debug = false;
        public static ${'$'}timeout = 30;
      }
      Settings::${'$'}debug = true;
      Settings::${'$'}timeout = 60;
      if (Settings::${'$'}debug) {
        echo "debug";
      }
      echo Settings::${'$'}timeout;
    """.trimIndent())
    assertEquals("debug60", output.trim())
  }

  @Test fun `static and instance members coexist`() {
    val output = executePhp("""
      <?php
      class Example {
        public static ${'$'}staticProp = "static";
        public ${'$'}instanceProp = "instance";

        public static function staticMethod() {
          return "static method";
        }

        public function instanceMethod() {
          return "instance method";
        }
      }
      echo Example::${'$'}staticProp;
      echo Example::staticMethod();
      ${'$'}obj = new Example();
      echo ${'$'}obj->instanceProp;
      echo ${'$'}obj->instanceMethod();
    """.trimIndent())
    assertEquals("staticstatic methodinstanceinstance method", output.trim())
  }

  // Tier 1 Feature Tests - Class Inheritance
  @Test fun `basic class inheritance with properties works`() {
    val output = executePhp("""
      <?php
      class Animal {
        public ${'$'}name;
      }
      class Dog extends Animal {
        public ${'$'}breed;
      }
      ${'$'}dog = new Dog();
      ${'$'}dog->name = "Max";
      ${'$'}dog->breed = "Labrador";
      echo ${'$'}dog->name;
      echo ${'$'}dog->breed;
    """.trimIndent())
    assertEquals("MaxLabrador", output.trim())
  }

  @Test fun `method inheritance works`() {
    val output = executePhp("""
      <?php
      class Parent {
        public function greet() {
          return "Hello";
        }
      }
      class Child extends Parent {
      }
      ${'$'}c = new Child();
      echo ${'$'}c->greet();
    """.trimIndent())
    assertEquals("Hello", output.trim())
  }

  @Test fun `constructor inheritance works`() {
    val output = executePhp("""
      <?php
      class Base {
        public ${'$'}value;

        function __construct(${'$'}v) {
          ${'$'}this->value = ${'$'}v;
        }
      }
      class Derived extends Base {
      }
      ${'$'}d = new Derived(42);
      echo ${'$'}d->value;
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `property with default value inheritance works`() {
    val output = executePhp("""
      <?php
      class Config {
        public ${'$'}timeout = 30;
        public ${'$'}retries = 3;
      }
      class CustomConfig extends Config {
        public ${'$'}debug = true;
      }
      ${'$'}cfg = new CustomConfig();
      echo ${'$'}cfg->timeout;
      echo ${'$'}cfg->retries;
      if (${'$'}cfg->debug) {
        echo "debug";
      }
    """.trimIndent())
    assertEquals("303debug", output.trim())
  }

  @Test fun `multiple level inheritance works`() {
    val output = executePhp("""
      <?php
      class A {
        public ${'$'}a = 1;
      }
      class B extends A {
        public ${'$'}b = 2;
      }
      class C extends B {
        public ${'$'}c = 3;
      }
      ${'$'}obj = new C();
      echo ${'$'}obj->a;
      echo ${'$'}obj->b;
      echo ${'$'}obj->c;
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  @Test fun `child constructor overrides parent constructor`() {
    val output = executePhp("""
      <?php
      class Parent {
        public ${'$'}x;

        function __construct() {
          ${'$'}this->x = 10;
        }
      }
      class Child extends Parent {
        public ${'$'}y;

        function __construct(${'$'}val) {
          ${'$'}this->y = ${'$'}val;
        }
      }
      ${'$'}c = new Child(20);
      if (${'$'}c->x == null) {
        echo "x_null";
      }
      echo ${'$'}c->y;
    """.trimIndent())
    assertEquals("x_null20", output.trim())
  }

  @Test fun `inherited method can access inherited properties`() {
    val output = executePhp("""
      <?php
      class Vehicle {
        public ${'$'}speed;

        function __construct(${'$'}s) {
          ${'$'}this->speed = ${'$'}s;
        }

        function getSpeed() {
          return ${'$'}this->speed;
        }
      }
      class Car extends Vehicle {
      }
      ${'$'}car = new Car(100);
      echo ${'$'}car->getSpeed();
    """.trimIndent())
    assertEquals("100", output.trim())
  }

  // Tier 1 Feature Tests - Exception Handling
  @Test fun `basic throw and catch works`() {
    val output = executePhp("""
      <?php
      try {
        throw new Exception("error");
      } catch (Exception ${'$'}e) {
        echo "caught";
      }
    """.trimIndent())
    assertEquals("caught", output.trim())
  }

  @Test fun `catch can access exception message`() {
    val output = executePhp("""
      <?php
      try {
        throw new Exception("test message");
      } catch (Exception ${'$'}e) {
        echo ${'$'}e->message;
      }
    """.trimIndent())
    assertEquals("test message", output.trim())
  }

  @Test fun `catch can access exception code`() {
    val output = executePhp("""
      <?php
      try {
        throw new Exception("error", 42);
      } catch (Exception ${'$'}e) {
        echo ${'$'}e->code;
      }
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `finally block executes after try`() {
    val output = executePhp("""
      <?php
      try {
        echo "try";
      } finally {
        echo "finally";
      }
    """.trimIndent())
    assertEquals("tryfinally", output.trim())
  }

  @Test fun `finally executes after catch`() {
    val output = executePhp("""
      <?php
      try {
        throw new Exception("error");
      } catch (Exception ${'$'}e) {
        echo "catch";
      } finally {
        echo "finally";
      }
    """.trimIndent())
    assertEquals("catchfinally", output.trim())
  }

  @Test fun `exception propagates through function calls`() {
    val output = executePhp("""
      <?php
      function throwError() {
        throw new Exception("inner");
      }
      try {
        throwError();
      } catch (Exception ${'$'}e) {
        echo "caught:" . ${'$'}e->message;
      }
    """.trimIndent())
    assertEquals("caught:inner", output.trim())
  }

  @Test fun `multiple catch blocks work`() {
    val output = executePhp("""
      <?php
      class CustomException extends Exception {
      }
      try {
        throw new Exception("base");
      } catch (CustomException ${'$'}e) {
        echo "custom";
      } catch (Exception ${'$'}e) {
        echo "base";
      }
    """.trimIndent())
    assertEquals("base", output.trim())
  }

  @Test fun `code after throw does not execute`() {
    val output = executePhp("""
      <?php
      try {
        echo "before";
        throw new Exception("error");
        echo "after";
      } catch (Exception ${'$'}e) {
        echo "caught";
      }
    """.trimIndent())
    assertEquals("beforecaught", output.trim())
  }

  @Test fun `exception in function caught by caller`() {
    val output = executePhp("""
      <?php
      function riskyOperation() {
        echo "start";
        throw new Exception("failed");
        echo "end";
      }
      try {
        riskyOperation();
      } catch (Exception ${'$'}e) {
        echo ${'$'}e->message;
      }
    """.trimIndent())
    assertEquals("startfailed", output.trim())
  }

  @Test fun `nested try-catch works`() {
    val output = executePhp("""
      <?php
      try {
        echo "outer";
        try {
          throw new Exception("inner");
        } catch (Exception ${'$'}e) {
          echo "inner-catch";
        }
        echo "after-inner";
      } catch (Exception ${'$'}e) {
        echo "outer-catch";
      }
    """.trimIndent())
    assertEquals("outerinner-catchafter-inner", output.trim())
  }

  @Test fun `exception with inheritance matching works`() {
    val output = executePhp("""
      <?php
      class MyException extends Exception {
      }
      try {
        throw new MyException("custom");
      } catch (Exception ${'$'}e) {
        echo "caught:" . ${'$'}e->message;
      }
    """.trimIndent())
    assertEquals("caught:custom", output.trim())
  }

  // Phase 1 Feature Tests - instanceof Operator
  @Test fun `instanceof with direct class returns true`() {
    val output = executePhp("""
      <?php
      class Person {
        public ${'$'}name;
      }
      ${'$'}p = new Person();
      if (${'$'}p instanceof Person) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `instanceof with parent class returns true`() {
    val output = executePhp("""
      <?php
      class Animal {
        public ${'$'}name;
      }
      class Dog extends Animal {
        public ${'$'}breed;
      }
      ${'$'}dog = new Dog();
      if (${'$'}dog instanceof Animal) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `instanceof with wrong class returns false`() {
    val output = executePhp("""
      <?php
      class Person {
        public ${'$'}name;
      }
      class Car {
        public ${'$'}model;
      }
      ${'$'}p = new Person();
      if (${'$'}p instanceof Car) {
        echo "yes";
      } else {
        echo "no";
      }
    """.trimIndent())
    assertEquals("no", output.trim())
  }

  @Test fun `instanceof with non-object returns false`() {
    val output = executePhp("""
      <?php
      class Person {
      }
      ${'$'}x = 42;
      if (${'$'}x instanceof Person) {
        echo "yes";
      } else {
        echo "no";
      }
    """.trimIndent())
    assertEquals("no", output.trim())
  }

  @Test fun `instanceof with null returns false`() {
    val output = executePhp("""
      <?php
      class Person {
      }
      ${'$'}x = null;
      if (${'$'}x instanceof Person) {
        echo "yes";
      } else {
        echo "no";
      }
    """.trimIndent())
    assertEquals("no", output.trim())
  }

  @Test fun `instanceof with multi-level inheritance works`() {
    val output = executePhp("""
      <?php
      class A {
      }
      class B extends A {
      }
      class C extends B {
      }
      ${'$'}obj = new C();
      if (${'$'}obj instanceof A) {
        echo "A";
      }
      if (${'$'}obj instanceof B) {
        echo "B";
      }
      if (${'$'}obj instanceof C) {
        echo "C";
      }
    """.trimIndent())
    assertEquals("ABC", output.trim())
  }

  @Test fun `instanceof in logical expression works`() {
    val output = executePhp("""
      <?php
      class Person {
        public ${'$'}age;
      }
      ${'$'}p = new Person();
      ${'$'}p->age = 25;
      if (${'$'}p instanceof Person && ${'$'}p->age > 18) {
        echo "adult";
      }
    """.trimIndent())
    assertEquals("adult", output.trim())
  }

  // Phase 1 Feature Tests - Compound Assignment Operators
  @Test fun `add assign operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 10;
      ${'$'}x += 5;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("15", output.trim())
  }

  @Test fun `subtract assign operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 20;
      ${'$'}x -= 7;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("13", output.trim())
  }

  @Test fun `multiply assign operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 6;
      ${'$'}x *= 4;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("24", output.trim())
  }

  @Test fun `divide assign operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 20;
      ${'$'}x /= 4;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("5.0", output.trim())
  }

  @Test fun `concatenate assign operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}str = "Hello";
      ${'$'}str .= " World";
      echo ${'$'}str;
    """.trimIndent())
    assertEquals("Hello World", output.trim())
  }

  @Test fun `modulo assign operator works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 17;
      ${'$'}x %= 5;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("2", output.trim())
  }

  @Test fun `basic modulo operator works`() {
    val output = executePhp("""
      <?php
      echo 17 % 5;
      echo 20 % 6;
    """.trimIndent())
    assertEquals("22", output.trim())
  }

  @Test fun `modulo with variables works`() {
    val output = executePhp("""
      <?php
      ${'$'}a = 15;
      ${'$'}b = 4;
      echo ${'$'}a % ${'$'}b;
    """.trimIndent())
    assertEquals("3", output.trim())
  }

  @Test fun `modulo in expressions works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = (10 % 3) + (20 % 7);
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("7", output.trim())
  }

  @Test fun `modulo with negative numbers works`() {
    val output = executePhp("""
      <?php
      echo -17 % 5;
      echo 17 % -5;
    """.trimIndent())
    assertEquals("-22", output.trim())
  }

  @Test fun `modulo in conditional works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 10;
      if (${'$'}x % 2 == 0) {
        echo "even";
      } else {
        echo "odd";
      }
    """.trimIndent())
    assertEquals("even", output.trim())
  }

  @Test fun `compound assignment returns value`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 10;
      ${'$'}y = (${'$'}x += 5);
      echo ${'$'}y;
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("1515", output.trim())
  }

  @Test fun `multiple compound assignments work`() {
    val output = executePhp("""
      <?php
      ${'$'}a = 5;
      ${'$'}a += 3;
      ${'$'}a *= 2;
      ${'$'}a -= 4;
      echo ${'$'}a;
    """.trimIndent())
    assertEquals("12", output.trim())
  }

  @Test fun `compound assignment with concatenation in loop`() {
    val output = executePhp("""
      <?php
      ${'$'}result = "";
      for (${'$'}i = 1; ${'$'}i <= 3; ${'$'}i = ${'$'}i + 1) {
        ${'$'}result .= ${'$'}i;
      }
      echo ${'$'}result;
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  // Phase 1 Feature Tests - Visibility Modifiers
  @Test fun `public property can be accessed from outside`() {
    val output = executePhp("""
      <?php
      class Box {
        public ${'$'}value = 42;
      }
      ${'$'}box = new Box();
      echo ${'$'}box->value;
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `private property cannot be accessed from outside`() {
    try {
      executePhp("""
        <?php
        class Box {
          private ${'$'}value = 42;
        }
        ${'$'}box = new Box();
        echo ${'$'}box->value;
      """.trimIndent())
      fail("Expected runtime exception for private property access")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Cannot access private property") == true ||
                 e.message?.contains("private") == true)
    }
  }

  @Test fun `protected property cannot be accessed from outside`() {
    try {
      executePhp("""
        <?php
        class Box {
          protected ${'$'}value = 42;
        }
        ${'$'}box = new Box();
        echo ${'$'}box->value;
      """.trimIndent())
      fail("Expected runtime exception for protected property access")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Cannot access protected property") == true ||
                 e.message?.contains("protected") == true)
    }
  }

  @Test fun `private property can be accessed from within class`() {
    val output = executePhp("""
      <?php
      class Box {
        private ${'$'}value = 42;

        public function getValue() {
          return ${'$'}this->value;
        }
      }
      ${'$'}box = new Box();
      echo ${'$'}box->getValue();
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `protected property can be accessed from within class`() {
    val output = executePhp("""
      <?php
      class Box {
        protected ${'$'}value = 42;

        public function getValue() {
          return ${'$'}this->value;
        }
      }
      ${'$'}box = new Box();
      echo ${'$'}box->getValue();
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `protected property can be accessed from subclass`() {
    val output = executePhp("""
      <?php
      class Parent {
        protected ${'$'}value = 42;
      }
      class Child extends Parent {
        public function getValue() {
          return ${'$'}this->value;
        }
      }
      ${'$'}child = new Child();
      echo ${'$'}child->getValue();
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `private property cannot be accessed from subclass`() {
    try {
      executePhp("""
        <?php
        class Parent {
          private ${'$'}value = 42;
        }
        class Child extends Parent {
          public function getValue() {
            return ${'$'}this->value;
          }
        }
        ${'$'}child = new Child();
        echo ${'$'}child->getValue();
      """.trimIndent())
      fail("Expected runtime exception for private property access from subclass")
    } catch (e: Exception) {
      // Expected - private properties are not accessible from subclasses
      assertTrue(e.message?.contains("Undefined property") == true ||
                 e.message?.contains("private") == true)
    }
  }

  @Test fun `private method cannot be called from outside`() {
    try {
      executePhp("""
        <?php
        class Box {
          private function secret() {
            return "hidden";
          }
        }
        ${'$'}box = new Box();
        echo ${'$'}box->secret();
      """.trimIndent())
      fail("Expected runtime exception for private method call")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Cannot call private method") == true ||
                 e.message?.contains("private") == true)
    }
  }

  @Test fun `protected method cannot be called from outside`() {
    try {
      executePhp("""
        <?php
        class Box {
          protected function secret() {
            return "hidden";
          }
        }
        ${'$'}box = new Box();
        echo ${'$'}box->secret();
      """.trimIndent())
      fail("Expected runtime exception for protected method call")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Cannot call protected method") == true ||
                 e.message?.contains("protected") == true)
    }
  }

  @Test fun `private method can be called from within class`() {
    val output = executePhp("""
      <?php
      class Box {
        private function secret() {
          return "hidden";
        }

        public function reveal() {
          return ${'$'}this->secret();
        }
      }
      ${'$'}box = new Box();
      echo ${'$'}box->reveal();
    """.trimIndent())
    assertEquals("hidden", output.trim())
  }

  @Test fun `protected method can be called from subclass`() {
    val output = executePhp("""
      <?php
      class Parent {
        protected function secret() {
          return "hidden";
        }
      }
      class Child extends Parent {
        public function reveal() {
          return ${'$'}this->secret();
        }
      }
      ${'$'}child = new Child();
      echo ${'$'}child->reveal();
    """.trimIndent())
    assertEquals("hidden", output.trim())
  }

  @Test fun `default visibility is public`() {
    val output = executePhp("""
      <?php
      class Box {
        ${'$'}value = 42;

        function getValue() {
          return ${'$'}this->value;
        }
      }
      ${'$'}box = new Box();
      echo ${'$'}box->value;
      echo ${'$'}box->getValue();
    """.trimIndent())
    assertEquals("4242", output.trim())
  }

  @Test fun `multiple visibility levels in same class work`() {
    val output = executePhp("""
      <?php
      class Example {
        public ${'$'}publicProp = "public";
        private ${'$'}privateProp = "private";
        protected ${'$'}protectedProp = "protected";

        public function getAll() {
          return ${'$'}this->publicProp . ${'$'}this->privateProp . ${'$'}this->protectedProp;
        }
      }
      ${'$'}ex = new Example();
      echo ${'$'}ex->publicProp;
      echo ${'$'}ex->getAll();
    """.trimIndent())
    assertEquals("publicpublicprivateprotected", output.trim())
  }

  // Additional Coverage Tests
  @Test fun `nested arrays work correctly`() {
    val output = executePhp("""
      <?php
      ${'$'}nested = [[1, 2], [3, 4]];
      echo ${'$'}nested[0][0];
      echo ${'$'}nested[0][1];
      echo ${'$'}nested[1][0];
      echo ${'$'}nested[1][1];
    """.trimIndent())
    assertEquals("1234", output.trim())
  }

  @Test fun `mixed type array with numeric and string keys works`() {
    val output = executePhp("""
      <?php
      ${'$'}mixed = [0 => "zero", "one" => 1, 2 => "two"];
      echo ${'$'}mixed[0];
      echo ${'$'}mixed["one"];
      echo ${'$'}mixed[2];
    """.trimIndent())
    assertEquals("zero1two", output.trim())
  }

  @Test fun `recursive function works`() {
    val output = executePhp("""
      <?php
      function factorial(${'$'}n) {
        if (${'$'}n <= 1) {
          return 1;
        }
        return ${'$'}n * factorial(${'$'}n - 1);
      }
      echo factorial(5);
    """.trimIndent())
    assertEquals("120", output.trim())
  }

  @Test fun `function with local variable scope works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = "global";
      function test() {
        ${'$'}x = "local";
        return ${'$'}x;
      }
      echo test();
      echo ${'$'}x;
    """.trimIndent())
    assertEquals("localglobal", output.trim())
  }

  @Test fun `array_merge with multiple arrays works`() {
    val output = executePhp("""
      <?php
      ${'$'}a = [1, 2];
      ${'$'}b = [3, 4];
      ${'$'}c = [5, 6];
      ${'$'}result = array_merge(${'$'}a, ${'$'}b, ${'$'}c);
      echo count(${'$'}result);
      echo ${'$'}result[0];
      echo ${'$'}result[5];
    """.trimIndent())
    assertEquals("616", output.trim())
  }

  @Test fun `arithmetic with mixed integer and float works`() {
    val output = executePhp("""
      <?php
      ${'$'}a = 10;
      ${'$'}b = 3.5;
      echo ${'$'}a + ${'$'}b;
      echo ${'$'}a - ${'$'}b;
    """.trimIndent())
    assertEquals("13.56.5", output.trim())
  }

  @Test fun `string concatenation in loop builds correctly`() {
    val output = executePhp("""
      <?php
      ${'$'}result = "";
      for (${'$'}i = 0; ${'$'}i < 5; ${'$'}i = ${'$'}i + 1) {
        ${'$'}result = ${'$'}result . ${'$'}i;
      }
      echo ${'$'}result;
    """.trimIndent())
    assertEquals("01234", output.trim())
  }

  @Test fun `while loop with complex condition works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 0;
      ${'$'}y = 10;
      while (${'$'}x < 5 && ${'$'}y > 5) {
        ${'$'}x = ${'$'}x + 1;
        ${'$'}y = ${'$'}y - 1;
      }
      echo ${'$'}x;
      echo ${'$'}y;
    """.trimIndent())
    assertEquals("55", output.trim())
  }

  @Test fun `foreach modifying array during iteration works`() {
    val output = executePhp("""
      <?php
      ${'$'}arr = [1, 2, 3];
      ${'$'}sum = 0;
      foreach (${'$'}arr as ${'$'}val) {
        ${'$'}sum = ${'$'}sum + ${'$'}val;
      }
      echo ${'$'}sum;
    """.trimIndent())
    assertEquals("6", output.trim())
  }

  @Test fun `object property chaining works`() {
    val output = executePhp("""
      <?php
      class Address {
        public ${'$'}city;
        function __construct(${'$'}c) {
          ${'$'}this->city = ${'$'}c;
        }
      }
      class Person {
        public ${'$'}address;
        function __construct() {
          ${'$'}this->address = new Address("NYC");
        }
      }
      ${'$'}p = new Person();
      echo ${'$'}p->address->city;
    """.trimIndent())
    assertEquals("NYC", output.trim())
  }

  @Test fun `method returning object allows method chaining pattern`() {
    val output = executePhp("""
      <?php
      class Builder {
        public ${'$'}value;
        function setValue(${'$'}v) {
          ${'$'}this->value = ${'$'}v;
          return ${'$'}this;
        }
        function getValue() {
          return ${'$'}this->value;
        }
      }
      ${'$'}b = new Builder();
      ${'$'}result = ${'$'}b->setValue(42);
      echo ${'$'}result->getValue();
    """.trimIndent())
    assertEquals("42", output.trim())
  }

  @Test fun `comparison with same types works`() {
    val output = executePhp("""
      <?php
      if (5 == 5) echo "1";
      if ("hello" == "hello") echo "2";
      if (true == true) echo "3";
      if (false == false) echo "4";
    """.trimIndent())
    assertEquals("1234", output.trim())
  }

  @Test fun `array with string keys and array_keys works`() {
    val output = executePhp("""
      <?php
      ${'$'}data = ["name" => "John", "age" => 30, "city" => "NYC"];
      ${'$'}keys = array_keys(${'$'}data);
      echo count(${'$'}keys);
      echo ${'$'}keys[0];
      echo ${'$'}keys[2];
    """.trimIndent())
    assertEquals("3namecity", output.trim())
  }

  @Test fun `trim function removes whitespace`() {
    val output = executePhp("""
      <?php
      ${'$'}str = "  hello world  ";
      echo trim(${'$'}str);
      echo strlen(trim(${'$'}str));
    """.trimIndent())
    assertEquals("hello world11", output.trim())
  }

  @Test fun `multiple echo statements on one line work`() {
    val output = executePhp("""
      <?php
      echo "a", "b", "c";
    """.trimIndent())
    assertEquals("abc", output.trim())
  }

  @Test fun `unary minus on variables works`() {
    val output = executePhp("""
      <?php
      ${'$'}x = 5;
      ${'$'}y = -${'$'}x;
      echo ${'$'}y;
      echo -${'$'}y;
    """.trimIndent())
    assertEquals("-55", output.trim())
  }

  @Test fun `switch with multiple cases using same code block works`() {
    val output = executePhp("""
      <?php
      ${'$'}day = 1;
      switch (${'$'}day) {
        case 0:
        case 6:
          echo "weekend";
          break;
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
          echo "weekday";
          break;
      }
    """.trimIndent())
    assertEquals("weekday", output.trim())
  }

  @Test fun `array_pop and array_push combination works`() {
    val output = executePhp("""
      <?php
      ${'$'}stack = [1, 2, 3];
      array_push(${'$'}stack, 4);
      echo count(${'$'}stack);
      ${'$'}top = array_pop(${'$'}stack);
      echo ${'$'}top;
      echo count(${'$'}stack);
    """.trimIndent())
    assertEquals("443", output.trim())
  }

  @Test fun `complex expression with parentheses evaluates correctly`() {
    val output = executePhp("""
      <?php
      ${'$'}result = (2 + 3) * (4 - 1);
      echo ${'$'}result;
    """.trimIndent())
    assertEquals("15", output.trim())
  }

  @Test fun `class method returning array works`() {
    val output = executePhp("""
      <?php
      class DataProvider {
        function getData() {
          return [1, 2, 3, 4, 5];
        }
      }
      ${'$'}dp = new DataProvider();
      ${'$'}data = ${'$'}dp->getData();
      echo count(${'$'}data);
      echo ${'$'}data[0];
      echo ${'$'}data[4];
    """.trimIndent())
    assertEquals("515", output.trim())
  }

  // Phase 1 Feature Tests - Interfaces
  @Test fun `basic interface definition works`() {
    val output = executePhp("""
      <?php
      interface Greeter {
        function greet(${'$'}name);
      }
      class EnglishGreeter implements Greeter {
        function greet(${'$'}name) {
          return "Hello " . ${'$'}name;
        }
      }
      ${'$'}g = new EnglishGreeter();
      echo ${'$'}g->greet("World");
    """.trimIndent())
    assertEquals("Hello World", output.trim())
  }

  @Test fun `instanceof with interface returns true`() {
    val output = executePhp("""
      <?php
      interface Drawable {
        function draw();
      }
      class Circle implements Drawable {
        function draw() {
          return "circle";
        }
      }
      ${'$'}c = new Circle();
      if (${'$'}c instanceof Drawable) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("yes", output.trim())
  }

  @Test fun `instanceof with non-implemented interface returns false`() {
    val output = executePhp("""
      <?php
      interface Serializable {
        function serialize();
      }
      interface Drawable {
        function draw();
      }
      class Circle implements Drawable {
        function draw() {
          return "circle";
        }
      }
      ${'$'}c = new Circle();
      if (${'$'}c instanceof Serializable) {
        echo "yes";
      } else {
        echo "no";
      }
    """.trimIndent())
    assertEquals("no", output.trim())
  }

  @Test fun `class implementing multiple interfaces works`() {
    val output = executePhp("""
      <?php
      interface Drawable {
        function draw();
      }
      interface Movable {
        function move(${'$'}x, ${'$'}y);
      }
      class Sprite implements Drawable, Movable {
        public ${'$'}x = 0;
        public ${'$'}y = 0;

        function draw() {
          return "drawing";
        }

        function move(${'$'}newX, ${'$'}newY) {
          ${'$'}this->x = ${'$'}newX;
          ${'$'}this->y = ${'$'}newY;
        }
      }
      ${'$'}s = new Sprite();
      echo ${'$'}s->draw();
      ${'$'}s->move(10, 20);
      echo ${'$'}s->x;
      echo ${'$'}s->y;
    """.trimIndent())
    assertEquals("drawing1020", output.trim())
  }

  @Test fun `instanceof with multiple interfaces works`() {
    val output = executePhp("""
      <?php
      interface A {
        function methodA();
      }
      interface B {
        function methodB();
      }
      class C implements A, B {
        function methodA() {
          return "a";
        }
        function methodB() {
          return "b";
        }
      }
      ${'$'}obj = new C();
      if (${'$'}obj instanceof A) {
        echo "A";
      }
      if (${'$'}obj instanceof B) {
        echo "B";
      }
      if (${'$'}obj instanceof C) {
        echo "C";
      }
    """.trimIndent())
    assertEquals("ABC", output.trim())
  }

  @Test fun `interface extending another interface works`() {
    val output = executePhp("""
      <?php
      interface Animal {
        function makeSound();
      }
      interface Pet extends Animal {
        function play();
      }
      class Dog implements Pet {
        function makeSound() {
          return "bark";
        }
        function play() {
          return "fetch";
        }
      }
      ${'$'}dog = new Dog();
      echo ${'$'}dog->makeSound();
      echo ${'$'}dog->play();
    """.trimIndent())
    assertEquals("barkfetch", output.trim())
  }

  @Test fun `instanceof with inherited interface works`() {
    val output = executePhp("""
      <?php
      interface Base {
        function baseMethod();
      }
      interface Extended extends Base {
        function extendedMethod();
      }
      class Implementation implements Extended {
        function baseMethod() {
          return "base";
        }
        function extendedMethod() {
          return "extended";
        }
      }
      ${'$'}obj = new Implementation();
      if (${'$'}obj instanceof Base) {
        echo "Base";
      }
      if (${'$'}obj instanceof Extended) {
        echo "Extended";
      }
    """.trimIndent())
    assertEquals("BaseExtended", output.trim())
  }

  @Test fun `class hierarchy with interface works`() {
    val output = executePhp("""
      <?php
      interface Printable {
        function printIt();
      }
      class Parent implements Printable {
        function printIt() {
          return "parent";
        }
      }
      class Child extends Parent {
        public ${'$'}value = 42;
      }
      ${'$'}c = new Child();
      echo ${'$'}c->printIt();
      echo ${'$'}c->value;
      if (${'$'}c instanceof Printable) {
        echo "yes";
      }
    """.trimIndent())
    assertEquals("parent42yes", output.trim())
  }

  @Test fun `interface with no methods works`() {
    val output = executePhp("""
      <?php
      interface Marker {
      }
      class Tagged implements Marker {
        public ${'$'}name = "test";
      }
      ${'$'}t = new Tagged();
      if (${'$'}t instanceof Marker) {
        echo "marked";
      }
      echo ${'$'}t->name;
    """.trimIndent())
    assertEquals("markedtest", output.trim())
  }

  @Test fun `multiple interface inheritance levels work`() {
    val output = executePhp("""
      <?php
      interface A {
        function methodA();
      }
      interface B extends A {
        function methodB();
      }
      interface C extends B {
        function methodC();
      }
      class D implements C {
        function methodA() {
          return "a";
        }
        function methodB() {
          return "b";
        }
        function methodC() {
          return "c";
        }
      }
      ${'$'}obj = new D();
      echo ${'$'}obj->methodA();
      echo ${'$'}obj->methodB();
      echo ${'$'}obj->methodC();
      if (${'$'}obj instanceof A) {
        echo "A";
      }
      if (${'$'}obj instanceof B) {
        echo "B";
      }
      if (${'$'}obj instanceof C) {
        echo "C";
      }
    """.trimIndent())
    assertEquals("abcABC", output.trim())
  }

  // Phase 1 Feature Tests - parent:: Keyword
  @Test fun `basic parent method call works`() {
    val output = executePhp("""
      <?php
      class Parent {
        function greet() {
          return "Hello";
        }
      }
      class Child extends Parent {
        function greet() {
          return parent::greet() . " World";
        }
      }
      ${'$'}c = new Child();
      echo ${'$'}c->greet();
    """.trimIndent())
    assertEquals("Hello World", output.trim())
  }

  @Test fun `parent method with parameters works`() {
    val output = executePhp("""
      <?php
      class Calculator {
        function add(${'$'}a, ${'$'}b) {
          return ${'$'}a + ${'$'}b;
        }
      }
      class AdvancedCalculator extends Calculator {
        function add(${'$'}a, ${'$'}b) {
          ${'$'}result = parent::add(${'$'}a, ${'$'}b);
          return ${'$'}result * 2;
        }
      }
      ${'$'}calc = new AdvancedCalculator();
      echo ${'$'}calc->add(5, 3);
    """.trimIndent())
    assertEquals("16", output.trim())
  }

  @Test fun `parent method accessing parent properties works`() {
    val output = executePhp("""
      <?php
      class Base {
        protected ${'$'}value = 10;

        function getValue() {
          return ${'$'}this->value;
        }
      }
      class Extended extends Base {
        function getDoubleValue() {
          return parent::getValue() * 2;
        }
      }
      ${'$'}e = new Extended();
      echo ${'$'}e->getDoubleValue();
    """.trimIndent())
    assertEquals("20", output.trim())
  }

  @Test fun `multiple parent method calls work`() {
    val output = executePhp("""
      <?php
      class A {
        function methodA() {
          return "A";
        }
        function methodB() {
          return "B";
        }
      }
      class C extends A {
        function combined() {
          return parent::methodA() . parent::methodB();
        }
      }
      ${'$'}c = new C();
      echo ${'$'}c->combined();
    """.trimIndent())
    assertEquals("AB", output.trim())
  }

  @Test fun `parent method call in multi-level inheritance works`() {
    val output = executePhp("""
      <?php
      class GrandParent {
        function greet() {
          return "GrandParent";
        }
      }
      class Parent extends GrandParent {
        function greet() {
          return "Parent";
        }
      }
      class Child extends Parent {
        function greet() {
          return parent::greet() . " and Child";
        }
      }
      ${'$'}c = new Child();
      echo ${'$'}c->greet();
    """.trimIndent())
    assertEquals("Parent and Child", output.trim())
  }

  @Test fun `parent method with no parameters works`() {
    val output = executePhp("""
      <?php
      class Base {
        function getMessage() {
          return "base message";
        }
      }
      class Derived extends Base {
        function getMessage() {
          return parent::getMessage() . " extended";
        }
      }
      ${'$'}d = new Derived();
      echo ${'$'}d->getMessage();
    """.trimIndent())
    assertEquals("base message extended", output.trim())
  }

  @Test fun `parent method call with string concatenation works`() {
    val output = executePhp("""
      <?php
      class Animal {
        function sound() {
          return "some sound";
        }
      }
      class Dog extends Animal {
        function sound() {
          return "bark - " . parent::sound();
        }
      }
      ${'$'}dog = new Dog();
      echo ${'$'}dog->sound();
    """.trimIndent())
    assertEquals("bark - some sound", output.trim())
  }

  // Phase 1 Feature Tests - self:: Keyword
  @Test fun `basic self method call works`() {
    val output = executePhp("""
      <?php
      class Calculator {
        public static function add(${'$'}a, ${'$'}b) {
          return ${'$'}a + ${'$'}b;
        }

        public static function addTen(${'$'}x) {
          return self::add(${'$'}x, 10);
        }
      }
      echo Calculator::addTen(5);
    """.trimIndent())
    assertEquals("15", output.trim())
  }

  @Test fun `self with static properties works`() {
    val output = executePhp("""
      <?php
      class Counter {
        public static ${'$'}count = 0;

        public static function increment() {
          self::${'$'}count = self::${'$'}count + 1;
        }

        public static function getCount() {
          return self::${'$'}count;
        }
      }
      Counter::increment();
      Counter::increment();
      Counter::increment();
      echo Counter::getCount();
    """.trimIndent())
    assertEquals("3", output.trim())
  }

  @Test fun `self vs parent behavior works`() {
    val output = executePhp("""
      <?php
      class Parent {
        public static function getName() {
          return "Parent";
        }

        public static function identify() {
          return self::getName();
        }
      }
      class Child extends Parent {
        public static function getName() {
          return "Child";
        }
      }
      echo Child::identify();
    """.trimIndent())
    assertEquals("Parent", output.trim())
  }

  @Test fun `self in inheritance chain works`() {
    val output = executePhp("""
      <?php
      class Base {
        protected static ${'$'}value = 100;

        public static function getValue() {
          return self::${'$'}value;
        }

        public static function modify(${'$'}x) {
          self::${'$'}value = self::${'$'}value + ${'$'}x;
        }
      }
      class Extended extends Base {
      }
      Extended::modify(50);
      echo Extended::getValue();
    """.trimIndent())
    assertEquals("150", output.trim())
  }

  @Test fun `self accessing static methods works`() {
    val output = executePhp("""
      <?php
      class Math {
        public static function double(${'$'}n) {
          return ${'$'}n * 2;
        }

        public static function triple(${'$'}n) {
          return ${'$'}n * 3;
        }

        public static function combine(${'$'}x) {
          return self::double(${'$'}x) + self::triple(${'$'}x);
        }
      }
      echo Math::combine(5);
    """.trimIndent())
    assertEquals("25", output.trim())
  }

  @Test fun `self with multiple parameters works`() {
    val output = executePhp("""
      <?php
      class String {
        public static function concat(${'$'}a, ${'$'}b, ${'$'}c) {
          return ${'$'}a . ${'$'}b . ${'$'}c;
        }

        public static function makePhrase(${'$'}first, ${'$'}last) {
          return self::concat(${'$'}first, " ", ${'$'}last);
        }
      }
      echo String::makePhrase("Hello", "World");
    """.trimIndent())
    assertEquals("Hello World", output.trim())
  }

  @Test fun `multiple self calls in single method works`() {
    val output = executePhp("""
      <?php
      class Data {
        public static ${'$'}a = 1;
        public static ${'$'}b = 2;
        public static ${'$'}c = 3;

        public static function getA() {
          return self::${'$'}a;
        }

        public static function getB() {
          return self::${'$'}b;
        }

        public static function getC() {
          return self::${'$'}c;
        }

        public static function getSum() {
          return self::getA() + self::getB() + self::getC();
        }
      }
      echo Data::getSum();
    """.trimIndent())
    assertEquals("6", output.trim())
  }

  // Phase 2 Feature Tests - Abstract Classes & Methods
  @Test fun `abstract class cannot be instantiated`() {
    try {
      executePhp("""
        <?php
        abstract class AbstractClass {
          abstract function doSomething();
        }
        ${'$'}obj = new AbstractClass();
      """.trimIndent())
      fail("Expected runtime exception for abstract class instantiation")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Cannot instantiate abstract class") == true ||
                 e.message?.contains("abstract") == true)
    }
  }

  @Test fun `concrete child implements abstract method works`() {
    val output = executePhp("""
      <?php
      abstract class Animal {
        abstract function makeSound();
      }
      class Dog extends Animal {
        function makeSound() {
          return "Bark";
        }
      }
      ${'$'}dog = new Dog();
      echo ${'$'}dog->makeSound();
    """.trimIndent())
    assertEquals("Bark", output.trim())
  }

  @Test fun `abstract child class does not require implementation`() {
    val output = executePhp("""
      <?php
      abstract class Animal {
        abstract function makeSound();
      }
      abstract class Mammal extends Animal {
        abstract function hasHair();
      }
      class Dog extends Mammal {
        function makeSound() {
          return "Bark";
        }
        function hasHair() {
          return true;
        }
      }
      ${'$'}dog = new Dog();
      echo ${'$'}dog->makeSound();
      if (${'$'}dog->hasHair()) {
        echo "Yes";
      }
    """.trimIndent())
    assertEquals("BarkYes", output.trim())
  }

  @Test fun `concrete class with multiple abstract methods works`() {
    val output = executePhp("""
      <?php
      abstract class Shape {
        abstract function area();
        abstract function perimeter();
      }
      class Rectangle extends Shape {
        public ${'$'}width;
        public ${'$'}height;

        function __construct(${'$'}w, ${'$'}h) {
          ${'$'}this->width = ${'$'}w;
          ${'$'}this->height = ${'$'}h;
        }

        function area() {
          return ${'$'}this->width * ${'$'}this->height;
        }

        function perimeter() {
          return 2 * (${'$'}this->width + ${'$'}this->height);
        }
      }
      ${'$'}r = new Rectangle(5, 10);
      echo ${'$'}r->area();
      echo ${'$'}r->perimeter();
    """.trimIndent())
    assertEquals("5030", output.trim())
  }

  @Test fun `abstract method inheritance through multiple levels works`() {
    val output = executePhp("""
      <?php
      abstract class A {
        abstract function methodA();
      }
      abstract class B extends A {
        abstract function methodB();
      }
      class C extends B {
        function methodA() {
          return "A";
        }
        function methodB() {
          return "B";
        }
      }
      ${'$'}obj = new C();
      echo ${'$'}obj->methodA();
      echo ${'$'}obj->methodB();
    """.trimIndent())
    assertEquals("AB", output.trim())
  }

  @Test fun `concrete class missing abstract method implementation fails`() {
    try {
      executePhp("""
        <?php
        abstract class Animal {
          abstract function makeSound();
        }
        class Dog extends Animal {
          function eat() {
            echo "eating";
          }
        }
        ${'$'}dog = new Dog();
      """.trimIndent())
      fail("Expected runtime exception for missing abstract method implementation")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("must implement abstract methods") == true ||
                 e.message?.contains("makeSound") == true)
    }
  }

  @Test fun `abstract method in non-abstract class fails`() {
    try {
      executePhp("""
        <?php
        class NotAbstract {
          abstract function doSomething();
        }
      """.trimIndent())
      fail("Expected parse exception for abstract method in non-abstract class")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("cannot be declared in non-abstract class") == true ||
                 e.message?.contains("abstract") == true)
    }
  }

  @Test fun `abstract method cannot be private`() {
    try {
      executePhp("""
        <?php
        abstract class Test {
          abstract private function doSomething();
        }
      """.trimIndent())
      fail("Expected parse exception for private abstract method")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("cannot be private") == true ||
                 e.message?.contains("private") == true)
    }
  }

  @Test fun `constructor cannot be abstract`() {
    try {
      executePhp("""
        <?php
        abstract class Test {
          abstract function __construct();
        }
      """.trimIndent())
      fail("Expected parse exception for abstract constructor")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("Constructor cannot be abstract") == true ||
                 e.message?.contains("constructor") == true)
    }
  }

  @Test fun `abstract class with concrete methods works`() {
    val output = executePhp("""
      <?php
      abstract class Vehicle {
        public ${'$'}fuel = 100;

        abstract function move();

        function refuel() {
          ${'$'}this->fuel = 100;
          return "refueled";
        }
      }
      class Car extends Vehicle {
        function move() {
          return "driving";
        }
      }
      ${'$'}car = new Car();
      echo ${'$'}car->move();
      echo ${'$'}car->refuel();
      echo ${'$'}car->fuel;
    """.trimIndent())
    assertEquals("drivingrefueled100", output.trim())
  }

  @Test fun `__toString magic method works with echo`() {
    val output = executePhp("""
      <?php
      class Person {
        public ${'$'}name;

        function __construct(${'$'}name) {
          ${'$'}this->name = ${'$'}name;
        }

        function __toString() {
          return "Person: " . ${'$'}this->name;
        }
      }
      ${'$'}person = new Person("Alice");
      echo ${'$'}person;
    """.trimIndent())
    assertEquals("Person: Alice", output.trim())
  }

  @Test fun `__toString magic method works with string concatenation`() {
    val output = executePhp("""
      <?php
      class Book {
        public ${'$'}title;

        function __construct(${'$'}title) {
          ${'$'}this->title = ${'$'}title;
        }

        function __toString() {
          return ${'$'}this->title;
        }
      }
      ${'$'}book = new Book("1984");
      echo "Reading: " . ${'$'}book;
    """.trimIndent())
    assertEquals("Reading: 1984", output.trim())
  }

  @Test fun `__toString must return string value`() {
    try {
      executePhp("""
        <?php
        class BadClass {
          function __toString() {
            return 42;
          }
        }
        ${'$'}obj = new BadClass();
        echo ${'$'}obj;
      """.trimIndent())
      fail("Expected exception for __toString returning non-string")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("must return a string") == true)
    }
  }

  @Test fun `__toString works in inheritance`() {
    val output = executePhp("""
      <?php
      class Animal {
        public ${'$'}species;

        function __construct(${'$'}species) {
          ${'$'}this->species = ${'$'}species;
        }

        function __toString() {
          return "Animal: " . ${'$'}this->species;
        }
      }

      class Dog extends Animal {
        public ${'$'}breed;

        function __construct(${'$'}breed) {
          ${'$'}this->species = "Dog";
          ${'$'}this->breed = ${'$'}breed;
        }
      }

      ${'$'}dog = new Dog("Labrador");
      echo ${'$'}dog;
    """.trimIndent())
    assertEquals("Animal: Dog", output.trim())
  }

  @Test fun `child class can override __toString`() {
    val output = executePhp("""
      <?php
      class Shape {
        function __toString() {
          return "generic shape";
        }
      }

      class Circle extends Shape {
        public ${'$'}radius;

        function __construct(${'$'}r) {
          ${'$'}this->radius = ${'$'}r;
        }

        function __toString() {
          return "circle with radius " . ${'$'}this->radius;
        }
      }

      ${'$'}shape = new Shape();
      ${'$'}circle = new Circle(5);
      echo ${'$'}shape;
      echo " and ";
      echo ${'$'}circle;
    """.trimIndent())
    assertEquals("generic shape and circle with radius 5", output.trim())
  }

  @Test fun `__get magic method works for undefined property`() {
    val output = executePhp("""
      <?php
      class MagicProperties {
        function __get(${'$'}name) {
          return "Getting property: " . ${'$'}name;
        }
      }
      ${'$'}obj = new MagicProperties();
      echo ${'$'}obj->foo;
    """.trimIndent())
    assertEquals("Getting property: foo", output.trim())
  }

  @Test fun `__set magic method works for undefined property`() {
    val output = executePhp("""
      <?php
      class MagicProperties {
        function __set(${'$'}name, ${'$'}value) {
          echo "Setting " . ${'$'}name . " to " . ${'$'}value;
        }
      }
      ${'$'}obj = new MagicProperties();
      ${'$'}obj->username = "Alice";
    """.trimIndent())
    assertEquals("Setting username to Alice", output.trim())
  }

  @Test fun `__get and __set work together for dynamic properties`() {
    val output = executePhp("""
      <?php
      class DynamicObject {
        public ${'$'}name;
        public ${'$'}age;

        function __get(${'$'}propName) {
          return "undefined";
        }

        function __set(${'$'}propName, ${'$'}value) {
          if (${'$'}propName == "name") {
            ${'$'}this->name = ${'$'}value;
          }
          if (${'$'}propName == "age") {
            ${'$'}this->age = ${'$'}value;
          }
        }
      }
      ${'$'}obj = new DynamicObject();
      ${'$'}obj->name = "Bob";
      ${'$'}obj->age = 25;
      echo ${'$'}obj->name;
      echo " is ";
      echo ${'$'}obj->age;
      echo " years old. Country: ";
      echo ${'$'}obj->country;
    """.trimIndent())
    assertEquals("Bob is 25 years old. Country: undefined", output.trim())
  }

  @Test fun `__get is called for inaccessible private property`() {
    val output = executePhp("""
      <?php
      class PrivateData {
        private ${'$'}secret = "hidden";

        function __get(${'$'}name) {
          return "Access to " . ${'$'}name . " denied via __get";
        }
      }
      ${'$'}obj = new PrivateData();
      echo ${'$'}obj->secret;
    """.trimIndent())
    assertEquals("Access to secret denied via __get", output.trim())
  }

  @Test fun `__set is called for inaccessible private property`() {
    val output = executePhp("""
      <?php
      class PrivateData {
        private ${'$'}secret = "hidden";

        function __set(${'$'}name, ${'$'}value) {
          echo "Attempt to set " . ${'$'}name . " to " . ${'$'}value . " blocked via __set";
        }
      }
      ${'$'}obj = new PrivateData();
      ${'$'}obj->secret = "exposed";
    """.trimIndent())
    assertEquals("Attempt to set secret to exposed blocked via __set", output.trim())
  }

  @Test fun `__call magic method works for undefined method`() {
    val output = executePhp("""
      <?php
      class MagicMethods {
        function __call(${'$'}name, ${'$'}arguments) {
          return "Called: " . ${'$'}name . " with " . count(${'$'}arguments) . " arguments";
        }
      }
      ${'$'}obj = new MagicMethods();
      echo ${'$'}obj->doSomething(1, 2, 3);
    """.trimIndent())
    assertEquals("Called: doSomething with 3 arguments", output.trim())
  }

  @Test fun `__call magic method works for inaccessible private method`() {
    val output = executePhp("""
      <?php
      class PrivateMethods {
        private function secret() {
          return "secret data";
        }

        function __call(${'$'}name, ${'$'}arguments) {
          return "Access to " . ${'$'}name . " denied via __call";
        }
      }
      ${'$'}obj = new PrivateMethods();
      echo ${'$'}obj->secret();
    """.trimIndent())
    assertEquals("Access to secret denied via __call", output.trim())
  }

  // Phase 1 Feature Tests - __invoke Magic Method
  @Test fun `__invoke magic method allows object to be called as function`() {
    val output = executePhp("""
      <?php
      class Adder {
        function __invoke(${'$'}a, ${'$'}b) {
          return ${'$'}a + ${'$'}b;
        }
      }
      ${'$'}add = new Adder();
      echo ${'$'}add(5, 10);
    """.trimIndent())
    assertEquals("15", output.trim())
  }

  @Test fun `__invoke with no arguments works`() {
    val output = executePhp("""
      <?php
      class Greeter {
        function __invoke() {
          return "Hello!";
        }
      }
      ${'$'}greet = new Greeter();
      echo ${'$'}greet();
    """.trimIndent())
    assertEquals("Hello!", output.trim())
  }

  @Test fun `__invoke with multiple arguments works`() {
    val output = executePhp("""
      <?php
      class Multiplier {
        function __invoke(${'$'}a, ${'$'}b, ${'$'}c) {
          return ${'$'}a * ${'$'}b * ${'$'}c;
        }
      }
      ${'$'}multiply = new Multiplier();
      echo ${'$'}multiply(2, 3, 4);
    """.trimIndent())
    assertEquals("24", output.trim())
  }

  @Test fun `__invoke can access object properties`() {
    val output = executePhp("""
      <?php
      class Counter {
        private ${'$'}count = 0;

        function __invoke() {
          ${'$'}this->count = ${'$'}this->count + 1;
          return ${'$'}this->count;
        }
      }
      ${'$'}counter = new Counter();
      echo ${'$'}counter();
      echo ${'$'}counter();
      echo ${'$'}counter();
    """.trimIndent())
    assertEquals("123", output.trim())
  }

  // Phase 1 Feature Tests - __callStatic Magic Method
  @Test fun `__callStatic magic method works for undefined static method`() {
    val output = executePhp("""
      <?php
      class StaticMagic {
        static function __callStatic(${'$'}name, ${'$'}arguments) {
          return "Static call: " . ${'$'}name . " with " . count(${'$'}arguments) . " arguments";
        }
      }
      echo StaticMagic::doSomething(1, 2, 3);
    """.trimIndent())
    assertEquals("Static call: doSomething with 3 arguments", output.trim())
  }

  @Test fun `__callStatic can return computed values`() {
    val output = executePhp("""
      <?php
      class Calculator {
        static function __callStatic(${'$'}name, ${'$'}arguments) {
          if (${'$'}name == "sum") {
            ${'$'}total = 0;
            ${'$'}i = 0;
            while (${'$'}i < count(${'$'}arguments)) {
              ${'$'}total = ${'$'}total + ${'$'}arguments[${'$'}i];
              ${'$'}i++;
            }
            return ${'$'}total;
          }
          return 0;
        }
      }
      echo Calculator::sum(10, 20, 30);
    """.trimIndent())
    assertEquals("60", output.trim())
  }

  private fun executePhp(code: String): String {
    val outputStream = ByteArrayOutputStream()
    val errorStream = ByteArrayOutputStream()

    Context.newBuilder("php")
      .out(outputStream)
      .err(errorStream)
      .option("engine.WarnInterpreterOnly", "false")
      .allowAllAccess(true)
      .build().use { context ->
        context.eval(Source.newBuilder("php", code, "test.php").build())
      }

    return outputStream.toString()
  }
}
