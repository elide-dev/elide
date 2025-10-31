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
