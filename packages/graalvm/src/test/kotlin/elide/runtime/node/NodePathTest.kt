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
@file:OptIn(DelicateElideApi::class)
@file:Suppress("JSUnresolvedReference", "LargeClass")

package elide.runtime.node

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Paths
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.js.node.PathAPI
import elide.runtime.node.path.*
import elide.runtime.node.path.PathStyle.POSIX
import elide.runtime.node.path.PathStyle.WIN32
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.io.path.Path as KotlinPath
import elide.runtime.intrinsics.js.node.path.Path as PathIntrinsic

fun PathAPI.testDirname(parsed: PathIntrinsic): String? {
  return dirname(parsed)
}

@Suppress("RedundantNullableReturnType")
fun PathAPI.testBasename(parsed: PathIntrinsic): String? {
  return basename(parsed)
}

@Suppress("RedundantNullableReturnType")
fun PathAPI.testExtname(parsed: PathIntrinsic): String? {
  return extname(parsed)
}

// @TODO: this will need normalization for non-unix test runs
// @TODO: this was the first use of the `assert` module; the order of expected/actual args is probably wrong
@TestCase internal class NodePathTest : NodeModuleConformanceTest<NodePathsModule>() {
  override val moduleName: String get() = "path"

  override fun provide(): NodePathsModule = NodePathsModule()
  private fun unixPaths() = NodePaths.create(POSIX)
  private fun windowsPaths() = NodePaths.create(WIN32)
  @Inject internal lateinit var paths: NodePathsModule

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("sep")
    yield("delimiter")
    yield("parse")
    yield("join")
    yield("isAbsolute")
    yield("dirname")
    yield("basename")
    yield("extname")
    yield("posix")
    yield("win32")
    yield("toNamespacedPath")
  }

  @Test override fun testInjectable() {
    assertNotNull(paths)
  }

  // Path module should not be injected globally.
  @Test fun `path module should not be present in globals by default`() = executeGuest {
    // language=javascript
    """
      test(path).isNotNull();
    """
  }.fails()

  // Private path intrinsic should be present.
  @Test fun `path module intrinsic should be present`() = executeGuest {
    // language=javascript
    """
      test(primordials.node_path).isNotNull();
      test(primordials.node_assert).isNotNull();
    """
  }.doesNotFail()

  @Test fun `should be able to create the path module`() {
    assertNotNull(NodePaths.create())
    assertNotNull(NodePaths.create(POSIX))
    assertNotNull(NodePaths.create(WIN32))
  }

  @Test fun `parsed path equals should behave reasonably`(): Unit = unixPaths().let {
    val one = it.parse("/sample/path/one")
    val other = it.parse("/sample/path/two")
    val third = it.parse("/sample/path/one")
    assertNotSame(one, other)
    assertNotSame(one, third)
    assertNotSame(other, third)
    assertEquals(one, third)
    assertNotEquals(one, other)
    assertNotEquals(other, third)
  }

  @Test fun `parsed path hashCode should behave reasonably`(): Unit = unixPaths().let {
    val one = it.parse("/sample/path/one")
    val other = it.parse("/sample/path/two")
    val third = it.parse("/sample/path/one")
    assertNotSame(one, other)
    assertNotSame(one, third)
    assertNotSame(other, third)
    assertEquals(one.hashCode(), third.hashCode())
    assertNotEquals(one.hashCode(), other.hashCode())
    assertNotEquals(other.hashCode(), third.hashCode())
    assertEquals(one.hashCode(), one.hashCode())
    assertEquals(one.hashCode(), one.hashCode())
  }

  @Test fun `should be able to parse posix-style paths`(): Unit = unixPaths().let {
    val parsed = assertNotNull(it.parse("/sample/cool/path"))
    assertNotSame(parsed, it.parse("/sample/cool/path"))
    assertEquals("/sample/cool/path", parsed.toString())
  }

  @Test fun `should be able to parse windows-style paths`(): Unit = windowsPaths().let {
    val parsed = assertNotNull(it.parse("C:\\sample\\cool\\path"))
    assertNotSame(parsed, it.parse("C:\\sample\\cool\\path"))
    assertEquals("C:\\sample\\cool\\path", parsed.toString())
  }

  @Test fun `separator should be valid for posix`(): Unit = assertEquals(unixPaths().sep, "/")
  @Test fun `separator should be valid for win32`(): Unit = assertEquals(windowsPaths().sep, "\\")
  @Test fun `separator length should be 1 for posix`(): Unit = assertTrue(unixPaths().sep.length == 1)
  @Test fun `separator length should be 1 for win32`(): Unit = assertTrue(windowsPaths().sep.length == 1)
  @Test fun `delimiter should be valid for posix`(): Unit = assertEquals(unixPaths().delimiter, ":")
  @Test fun `delimiter should be valid for win32`(): Unit = assertEquals(windowsPaths().delimiter, ";")
  @Test fun `delimiter length should be 1 for posix`(): Unit = assertTrue(unixPaths().delimiter.length == 1)
  @Test fun `delimiter length should be 1 for win32`(): Unit = assertTrue(windowsPaths().delimiter.length == 1)

  @Test fun `join should work for posix-style paths`(): Unit = unixPaths().let {
    val parsed = assertNotNull(it.parse("/sample/cool/path"))
    assertNotSame(parsed, it.parse("/sample/cool/path"))
    assertEquals(parsed.toString(), "/sample/cool/path")
    val other = it.parse("other/cool/path")
    assertNotSame(other, parsed)
    assertNotEquals(other, parsed)
    assertEquals("/sample/cool/path/other/cool/path", it.join(parsed, other))
  }

  @Test fun `join should work for windows-style paths`(): Unit = windowsPaths().let {
    val parsed = assertNotNull(it.parse("C:\\sample\\cool\\path"))
    assertNotSame(parsed, it.parse("C:\\sample\\cool\\path"))
    assertEquals("C:\\sample\\cool\\path", parsed.toString())
    val other = it.parse("other\\cool\\path")
    assertNotSame(other, parsed)
    assertNotEquals(other, parsed)
    assertEquals("C:\\sample\\cool\\path\\other\\cool\\path", it.join(parsed, other))
  }

  @Test fun `should be able to convert path intrinsic to kotlin path`() {
    val parsed = assertNotNull(unixPaths().parse("/sample/cool/path"))
    assertEquals("/sample/cool/path", parsed.toString())
    assertEquals("/sample/cool/path", parsed.toKotlinPath().toString())
    val win32 = assertNotNull(windowsPaths().parse("C:\\sample\\cool\\path"))
    assertEquals("C:\\sample\\cool\\path", win32.toString())
    assertEquals("C:\\sample\\cool\\path", win32.toKotlinPath().toString())
  }

  @Test fun `should be able to convert path intrinsic to java path`() {
    val parsed = assertNotNull(unixPaths().parse("/sample/cool/path"))
    assertEquals("/sample/cool/path", parsed.toString())
    assertEquals("/sample/cool/path", parsed.toJavaPath().toString())
    val win32 = assertNotNull(windowsPaths().parse("C:\\sample\\cool\\path"))
    assertEquals("C:\\sample\\cool\\path", win32.toString())
    assertEquals("C:\\sample\\cool\\path", win32.toJavaPath().toString())
  }

  @Test fun `parsed paths should behave like strings`() {
    val parsed = assertNotNull(unixPaths().parse("/sample/cool/path"))
    val subject = "/sample/cool/path"
    assertEquals(subject, parsed.toString())
    assertEquals(subject.length, parsed.length)
    assertEquals(subject.substring(3), parsed.substring(3))
    assertEquals(subject.substring(3, 5), parsed.substring(3, 5))
    assertEquals(subject.subSequence(3..5), parsed.subSequence(3..5))
    assertEquals(subject.subSequence(3, 5), parsed.subSequence(3, 5))
    assertEquals(subject[0], parsed[0])
    assertEquals(subject[3], parsed[3])
  }

  @Test fun `parsed paths should be comparable as strings`() {
    val parsed = assertNotNull(unixPaths().parse("/sample/cool/path"))
    val other = unixPaths().parse("/sample/cool/path")
    val third = unixPaths().parse("/sample/cool/path/other")
    assertEquals(0, parsed.compareTo(other))
    assertEquals(0, other.compareTo(parsed))
    assertTrue(parsed < third)
    assertTrue(third > parsed)
    assertTrue(parsed <= other)
    assertTrue(parsed >= other)
    assertTrue(parsed <= third)
    assertTrue(parsed < third)
    assertTrue(third > other)
    assertTrue(third >= other)
  }

  @Test fun `should be able to parse absolute posix-style paths`() {
    val parsed = assertNotNull(unixPaths().parse("/sample/cool/path"))
    assertEquals("/sample/cool", parsed.dir)
    assertEquals("/sample/cool/path", parsed.toString())
    assertEquals("path", parsed.base)
    assertEquals("path", parsed.name)
    assertEquals("", parsed.ext)
    assertEquals("/", parsed.root)
    assertTrue(parsed.isAbsolute)
    val parsed2 = assertNotNull(unixPaths().parse("/sample/cool/path.txt"))
    assertEquals("/sample/cool", parsed2.dir)
    assertEquals("/sample/cool/path.txt", parsed2.toString())
    assertEquals("path.txt", parsed2.base)
    assertEquals("path", parsed2.name)
    assertEquals(".txt", parsed2.ext)
    assertTrue(parsed2.isAbsolute)
  }

  @Test fun `should be able to parse absolute windows-style paths`() {
    val parsed = assertNotNull(windowsPaths().parse("C:\\sample\\cool\\path"))
    assertEquals("C:\\sample\\cool", parsed.dir)
    assertEquals("C:\\sample\\cool\\path", parsed.toString())
    assertEquals("path", parsed.base)
    assertEquals("path", parsed.name)
    assertEquals("", parsed.ext)
    assertEquals("C:\\", parsed.root)
    assertTrue(parsed.isAbsolute)
    val parsed2 = assertNotNull(windowsPaths().parse("C:\\sample\\cool\\path.txt"))
    assertEquals("C:\\sample\\cool", parsed2.dir)
    assertEquals("C:\\sample\\cool\\path.txt", parsed2.toString())
    assertEquals("path.txt", parsed2.base)
    assertEquals("path", parsed2.name)
    assertEquals(".txt", parsed2.ext)
    assertEquals("C:\\", parsed.root)
    assertTrue(parsed2.isAbsolute)
  }

  @Test fun `should be able to parse relative unix-style paths`() {
    val parsed = assertNotNull(unixPaths().parse("sample/cool/path"))
    assertEquals("sample/cool", parsed.dir)
    assertEquals("sample/cool/path", parsed.toString())
    assertEquals("path", parsed.base)
    assertEquals("path", parsed.name)
    assertEquals("", parsed.ext)
    assertFalse(parsed.isAbsolute)
    val parsed2 = assertNotNull(unixPaths().parse("sample/cool/path.txt"))
    assertEquals("sample/cool", parsed2.dir)
    assertEquals("sample/cool/path.txt", parsed2.toString())
    assertEquals("path.txt", parsed2.base)
    assertEquals("path", parsed2.name)
    assertEquals(".txt", parsed2.ext)
    assertFalse(parsed2.isAbsolute)
  }

  @Test fun `should be able to parse relative windows-style paths`() {
    val parsed = assertNotNull(windowsPaths().parse("sample\\cool\\path"))
    assertEquals("sample\\cool", parsed.dir)
    assertEquals("sample\\cool\\path", parsed.toString())
    assertEquals("path", parsed.base)
    assertEquals("path", parsed.name)
    assertEquals("", parsed.ext)
    assertFalse(parsed.isAbsolute)
    val parsed2 = assertNotNull(windowsPaths().parse("sample\\cool\\path.txt"))
    assertEquals("sample\\cool", parsed2.dir)
    assertEquals("sample\\cool\\path.txt", parsed2.toString())
    assertEquals("path.txt", parsed2.base)
    assertEquals("path", parsed2.name)
    assertEquals(".txt", parsed2.ext)
    assertFalse(parsed2.isAbsolute)
  }

  @Test fun `should be able to obtain os-explicit path utilities`() {
    assertNotNull(NodePaths.create())
    assertNotNull(NodePaths.create(POSIX))
    assertNotNull(NodePaths.create(WIN32))
    assertNotNull(NodePaths.create(POSIX).posix)
    assertNotNull(NodePaths.create(POSIX).win32)
    assertNotNull(NodePaths.create().posix)
    assertNotNull(NodePaths.create().win32)
    assertNotNull(NodePaths.create(WIN32).posix)
    assertNotNull(NodePaths.create(WIN32).win32)
    assertSame(NodePaths.create(POSIX), NodePaths.create(POSIX))
    assertSame(NodePaths.create(POSIX), NodePaths.create(POSIX).posix)
    assertSame(NodePaths.create(POSIX), NodePaths.create(WIN32).posix)
    assertSame(NodePaths.create(WIN32), NodePaths.create(WIN32))
    assertSame(NodePaths.create(WIN32), NodePaths.create(WIN32).win32)
    assertSame(NodePaths.create(WIN32), NodePaths.create(POSIX).win32)
  }

  @Test fun `paths should be copyable`() {
    val relative = assertNotNull(unixPaths().parse("some/cool/path"))
    val absolute = assertNotNull(unixPaths().parse("/some/cool/path"))
    val copied = assertNotNull(relative.copy())
    assertNotSame(relative, copied)
    assertEquals(relative, copied)
    val copied2 = assertNotNull(absolute.copy())
    assertNotSame(absolute, copied2)
    assertEquals(absolute, copied2)
  }

  private data class TestPaths(
    val absoluteDir: String,
    val absoluteFile: String,
    val relativeDir: String,
    val relativeFile: String,
    val basenameTests: List<Pair<String, String?>>,
    val dirnameTests: List<Pair<String, String?>>,
    val extnameTests: List<Pair<String, String?>>,
  ) {
    val allRelative = listOf(relativeDir, relativeFile)
    val allAbsolute = listOf(absoluteDir, absoluteFile)
    val all = allRelative + allAbsolute

    companion object {
      val unix = TestPaths(
        absoluteDir = "/sample/cool",
        absoluteFile = "/sample/cool/path",
        relativeDir = "sample/cool",
        relativeFile = "sample/cool/path",
        basenameTests = listOf(
          "sample/cool" to "cool",
          "sample/cool/path" to "path",
          "/sample/cool" to "cool",
          "/sample/cool/path" to "path",
          "sample/cool/test.txt" to "test.txt",
          "/sample/cool/test.txt" to "test.txt",
          "test.txt" to "test.txt",
        ),
        dirnameTests = listOf(
          "sample/cool" to "sample",
          "sample/cool/path" to "sample/cool",
          "/sample/cool" to "/sample",
          "/sample/cool/path" to "/sample/cool",
          "sample/cool/test.txt" to "sample/cool",
          "/sample/cool/test.txt" to "/sample/cool",
          "test.txt" to ".",
        ),
        extnameTests = listOf(
          "sample/cool" to "",
          "sample/cool/path" to "",
          "sample/cool/test.txt" to ".txt",
          "/sample/cool" to "",
          "/sample/cool/path" to "",
          "sample/cool/test.txt" to ".txt",
          "/sample/cool/test.txt" to ".txt",
          "test" to "",
          "text.txt" to ".txt",
        ),
      )

      val windows = TestPaths(
        absoluteDir = "C:\\sample\\cool",
        absoluteFile = "C:\\sample\\cool\\path",
        relativeDir = "sample\\cool",
        relativeFile = "sample\\cool\\path",
        basenameTests = listOf(
          "sample\\cool" to "cool",
          "sample\\cool\\path" to "path",
          "C:\\sample\\cool" to "cool",
          "C:\\sample\\cool\\path" to "path",
          "sample\\cool\\test.txt" to "test.txt",
          "C:\\sample\\cool\\test.txt" to "test.txt",
        ),
        dirnameTests = listOf(
          "sample\\cool" to "sample",
          "sample\\cool\\path" to "sample\\cool",
          "C:\\sample\\cool" to "C:\\sample",
          "C:\\sample\\cool\\path" to "C:\\sample\\cool",
          "sample\\cool\\test.txt" to "sample\\cool",
          "C:\\sample\\cool\\test.txt" to "C:\\sample\\cool",
          "test.txt" to ".",
        ),
        extnameTests = listOf(
          "sample\\cool" to "",
          "sample\\cool\\path" to "",
          "sample\\cool\\test.txt" to ".txt",
          "C:\\sample\\cool" to "",
          "C:\\sample\\cool\\path" to "",
          "sample\\cool\\test.txt" to ".txt",
          "C:\\sample\\cool\\test.txt" to ".txt",
        ),
      )
    }
  }

  private fun testPathApiConformanceFromJvm(style: PathStyle, label: String, api: PathAPI, testPaths: TestPaths) {
    // the `sep` property should be non-null, string, length 1
    assertNotNull(api.sep, "`sep` should not be null (type: $label)")
    assertTrue(api.sep.isNotEmpty(), "`sep` should not be empty (type: $label)")
    assertEquals(1, api.sep.length, "`sep` should be length 1 (type: $label)")

    // the `delimiter` property should meet the same requirements as `sep`
    assertNotNull(api.delimiter, "`delimiter` should not be null (type: $label)")
    assertTrue(api.delimiter.isNotEmpty(), "`delimiter` should not be empty (type: $label)")
    assertEquals(1, api.delimiter.length, "`delimiter` should be length 1 (type: $label)")

    // should be able to parse all sample paths
    testPaths.all.forEach { path ->
      val parsed = assertNotNull(api.parse(path, style), "Failed to parse path: $path (type: $label)")
      assertEquals(path, path, "Parsed path does not equal itself: $path (type: $label)")
      assertEquals(path, parsed.toString(), "Parsed path does not match original: $path (type: $label)")
    }

    // relative paths should not be absolute
    testPaths.allRelative.forEach { path ->
      val parsed = assertNotNull(api.parse(path, style), "Failed to parse relative path: $path (type: $label)")
      assertFalse(parsed.isAbsolute, "Relative path should not be absolute: $path (type: $label)")
      assertFalse(api.isAbsolute(parsed), "Relative path should not be absolute (API): $path (type: $label)")
    }

    // absolute paths should be absolute
    testPaths.allAbsolute.forEach { path ->
      val parsed = assertNotNull(api.parse(path, style), "Failed to parse absolute path: $path (type: $label)")
      assertTrue(parsed.isAbsolute, "Absolute path should be absolute: $path (type: $label)")
      assertTrue(api.isAbsolute(parsed), "Absolute path should be absolute (API): $path (type: $label)")
    }

    // each basename and dirname test should match
    listOf(
      "dirname" to (testPaths.dirnameTests to api::testDirname),
      "basename" to (testPaths.basenameTests to api::testBasename),
      "extname" to (testPaths.extnameTests to api::testExtname),
    ).forEach { (testProfile, suite) ->
      val (samples, testOp) = suite
      samples.forEach { (path, expected) ->
        val parsed = assertNotNull(
          api.parse(path, style),
          "Failed to parse path for '$testProfile' test (got `null`): $path (type: $label / style: $style)",
        )
        assertEquals(
          expected,
          testOp.invoke(parsed),
          "Test for method '$testProfile' does not match expected result: $path (type: $label / style: $style)",
        )
      }
    }
  }

  @Test fun `path api should conform for posix paths`(): Unit = testPathApiConformanceFromJvm(
    POSIX,
    "unix",
    unixPaths(),
    TestPaths.unix,
  )

  @Test fun `path api should conform for windows paths`(): Unit = testPathApiConformanceFromJvm(
    WIN32,
    "win32",
    windowsPaths(),
    TestPaths.windows,
  )

  @Test fun `parsing paths should work from javascript`() = executeGuest {
    // language=javascript
    """
      const { parse } = require("path");
      const { equal } = require("assert");
      const parsed1 = parse("/sample/cool/path");
      equal(parsed1.dir, '/sample/cool');
      equal(parsed1.base, 'path');
      equal(parsed1.name, 'path');
      equal(parsed1.ext, '');
      equal(parsed1.root, '/');
      equal(parsed1.toString(), '/sample/cool/path');
      const parsed2 = parse("sample/cool/path");
      equal(parsed2.dir, 'sample/cool');
      equal(parsed2.base, 'path');
      equal(parsed2.name, 'path');
      equal(parsed2.ext, '');
      equal(parsed2.root, '');
      equal(parsed2.toString(), 'sample/cool/path');
      const parsed3 = parse("/sample/cool/path.txt");
      equal(parsed3.dir, '/sample/cool');
      equal(parsed3.base, 'path.txt');
      equal(parsed3.name, 'path');
      equal(parsed3.ext, '.txt');
      equal(parsed3.root, '/');
      equal(parsed3.toString(), '/sample/cool/path.txt');
      const parsed4 = parse("sample/cool/path.txt");
      equal(parsed4.dir, 'sample/cool');
      equal(parsed4.base, 'path.txt');
      equal(parsed4.name, 'path');
      equal(parsed4.ext, '.txt');
      equal(parsed4.root, '');
      equal(parsed4.toString(), 'sample/cool/path.txt');
      const parsed5 = parse("sample/cool/../path.txt");
      equal(parsed5.dir, 'sample/cool/..');
      equal(parsed5.base, 'path.txt');
      equal(parsed5.name, 'path');
      equal(parsed5.ext, '.txt');
      equal(parsed5.root, '');
      equal(parsed5.toString(), 'sample/cool/../path.txt');
      const parsed6 = parse("/sample/cool/../path.txt");
      equal(parsed6.dir, '/sample/cool/..');
      equal(parsed6.base, 'path.txt');
      equal(parsed6.name, 'path');
      equal(parsed6.ext, '.txt');
      equal(parsed6.root, '/');
      equal(parsed6.toString(), '/sample/cool/../path.txt');
      const parsed7 = parse("sample/cool/./path.txt");
      equal(parsed7.dir, 'sample/cool/.');
      equal(parsed7.base, 'path.txt');
      equal(parsed7.name, 'path');
      equal(parsed7.ext, '.txt');
      equal(parsed7.root, '');
      equal(parsed7.toString(), 'sample/cool/./path.txt');
      const parsed8 = parse("/sample/cool/./path.txt");
      equal(parsed8.dir, '/sample/cool/.');
      equal(parsed8.base, 'path.txt');
      equal(parsed8.name, 'path');
      equal(parsed8.ext, '.txt');
      equal(parsed8.root, '/');
      equal(parsed8.toString(), '/sample/cool/./path.txt');
      parsed1;
    """
  }.thenAssert {
    val value = assertNotNull(it.returnValue(), "should get return value for guest path parse")
    // @TODO: adopting `ProxyObject` means this object will no longer show up as a host object.
    // assertTrue(value.isHostObject, "returned path should be a host object")
    // val obj = assertNotNull(value.asHostObject<PathIntrinsic>(), "should be able to decode as host path object")
    // assertIs<PathIntrinsic>(obj, "resulting object should be a host-side path")
    assertEquals("/sample/cool/path", value.getMember("toString").execute().asString())
  }

  @Test fun `parse conformance with node`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    assertEquals("/sample/cool/path", parsed.toString())
    assertEquals(parsed, path.parse("/sample/cool/path"))
    assertEquals(path.parse("some/relative/path"), path.parse("some/relative/path"))
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse } = require("path");
      const parsed = parse('/sample/cool/path');
      equal(parse('some/relative/path').dir, parse('some/relative/path').dir);
      equal(parse('some/relative/path').name, parse('some/relative/path').name);
      equal(parse('some/relative/path').base, parse('some/relative/path').base);
      equal(parse('some/relative/path').ext, parse('some/relative/path').ext);
    """
  }

  @Test fun `join conformance with node (absolute, posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    val other = assertNotNull(path.parse("other/cool/path"), "should be able to parse another path")
    val joined = path.join(parsed, other)
    assertEquals("/sample/cool/path/other/cool/path", joined)
    val joinedParsed = assertNotNull(path.parse("/sample/cool/path/other/cool/path"))
    assertEquals(joinedParsed.toString(), joined)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { join } = require("path");
      const joined = join('/sample/cool/path', 'other/cool/path');
      equal(joined, '/sample/cool/path/other/cool/path');
    """
  }

  @Test fun `join conformance with node (absolute, posix, strings)`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    val other = assertNotNull(path.parse("other/cool/path"), "should be able to parse another path")
    val joined = path.join(parsed, other)
    assertEquals("/sample/cool/path/other/cool/path", joined)
    val joinedParsed = assertNotNull(path.parse("/sample/cool/path/other/cool/path"))
    assertEquals(joinedParsed.toString(), joined)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { join } = require("path");
      const joined = join('/sample/cool/path', 'other/cool/path');
      equal(joined, '/sample/cool/path/other/cool/path');
    """
  }

  @Test fun `join conformance with node (relative, posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("sample/cool/path"), "should be able to parse a path")
    val other = assertNotNull(path.parse("other/cool/path"), "should be able to parse another path")
    val joined = path.join(parsed, other)
    assertEquals("sample/cool/path/other/cool/path", joined)
    val joinedParsed = assertNotNull(path.parse("sample/cool/path/other/cool/path"))
    assertEquals(joinedParsed.toString(), joined)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { join } = require("path");
      const joined = join('sample/cool/path', 'other/cool/path');
      equal(joined, 'sample/cool/path/other/cool/path');
    """
  }

  @Test fun `join conformance with node (absolute, win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    val parsed = assertNotNull(
      path.parse("C:\\sample\\cool\\path", WIN32),
      "should be able to parse a path",
    )
    val other = assertNotNull(
      path.parse("other\\cool\\path", WIN32),
      "should be able to parse another path",
    )
    val joined = path.join(parsed, other)
    assertEquals("C:\\sample\\cool\\path\\other\\cool\\path", joined)
    val joinedParsed = assertNotNull(path.parse("C:\\sample\\cool\\path\\other\\cool\\path", WIN32))
    assertEquals(joinedParsed.toString(), joined)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const path = require("path");
      const { join } = path.win32;
      const joined = join('C:\\sample\\cool\\path', 'other\\cool\\path');
      equal(joined, 'C:\\sample\\cool\\path\\other\\cool\\path');
    """
  }

  @Test fun `join conformance with node (relative, win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    val parsed = assertNotNull(
      path.parse("sample\\cool\\path", WIN32),
      "should be able to parse a path",
    )
    val other = assertNotNull(
      path.parse("other\\cool\\path", WIN32),
      "should be able to parse another path",
    )
    val joined = path.join(parsed, other)
    assertEquals("sample\\cool\\path\\other\\cool\\path", joined)
    val joinedParsed = assertNotNull(path.parse("sample\\cool\\path\\other\\cool\\path", WIN32))
    assertEquals(joinedParsed.toString(), joined)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const path = require("path");
      const { join } = path.win32;
      const joined = join('sample\\cool\\path', 'other\\cool\\path');
      equal(joined, 'sample\\cool\\path\\other\\cool\\path');
    """
  }

  @Test fun `basename should accept strings`() = unixPaths().let {
    assertEquals("hello", it.basename("/sample/cool/hello"))
    assertEquals("hello.txt", it.basename("/sample/cool/hello.txt"))
    assertEquals("hello", it.basename("/sample/cool/hello.txt", ".txt"))
    assertEquals("hello", it.basename("/sample/cool/hello.txt", "txt"))
    assertEquals("hello.txt", it.basename("/sample/cool/hello.txt", ".bin"))
    assertEquals("hello.txt", it.basename("/sample/cool/hello.txt", "bin"))

    assertEquals("path.txt", it.basename(it.parse("sample/cool/path.txt")))
    assertEquals("hello", it.basename("sample/cool/hello"))
    assertEquals("hello.txt", it.basename("sample/cool/hello.txt"))
    assertEquals("hello", it.basename("sample/cool/hello.txt", ".txt"))
    assertEquals("hello", it.basename("sample/cool/hello.txt", "txt"))
    assertEquals("hello.txt", it.basename("sample/cool/hello.txt", ".bin"))
    assertEquals("hello.txt", it.basename("sample/cool/hello.txt", "bin"))

    assertEquals("path.txt", it.basename(it.parse("path.txt")))
    assertEquals("hello", it.basename("hello"))
    assertEquals("hello.txt", it.basename("hello.txt"))
    assertEquals("hello", it.basename("hello.txt", ".txt"))
    assertEquals("hello", it.basename("hello.txt", "txt"))
    assertEquals("hello.txt", it.basename("hello.txt", ".bin"))
    assertEquals("hello.txt", it.basename("hello.txt", "bin"))
  }

  @Test fun `basename should accept kotlin paths`() = unixPaths().let {
    assertEquals("hello", it.basename(it.parse("/sample/cool/hello").toKotlinPath()))
    assertEquals("hello.txt", it.basename(it.parse("/sample/cool/hello.txt").toKotlinPath()))
    assertEquals("hello", it.basename(it.parse("/sample/cool/hello.txt").toKotlinPath(), ".txt"))
    assertEquals("hello", it.basename(it.parse("/sample/cool/hello.txt").toKotlinPath(), "txt"))
    assertEquals("hello.txt", it.basename(it.parse("/sample/cool/hello.txt").toKotlinPath(), ".bin"))
    assertEquals("hello.txt", it.basename(it.parse("/sample/cool/hello.txt").toKotlinPath(), "bin"))

    assertEquals("path.txt", it.basename(it.parse("sample/cool/path.txt").toKotlinPath()))
    assertEquals("hello", it.basename(it.parse("sample/cool/hello").toKotlinPath()))
    assertEquals("hello.txt", it.basename(it.parse("sample/cool/hello.txt").toKotlinPath()))
    assertEquals("hello", it.basename(it.parse("sample/cool/hello.txt").toKotlinPath(), ".txt"))
    assertEquals("hello", it.basename(it.parse("sample/cool/hello.txt").toKotlinPath(), "txt"))
    assertEquals("hello.txt", it.basename(it.parse("sample/cool/hello.txt").toKotlinPath(), ".bin"))
    assertEquals("hello.txt", it.basename(it.parse("sample/cool/hello.txt").toKotlinPath(), "bin"))

    assertEquals("path.txt", it.basename(it.parse("path.txt").toKotlinPath()))
    assertEquals("hello", it.basename(it.parse("hello").toKotlinPath()))
    assertEquals("hello.txt", it.basename(it.parse("hello.txt").toKotlinPath()))
    assertEquals("hello", it.basename(it.parse("hello.txt").toKotlinPath(), ".txt"))
    assertEquals("hello", it.basename(it.parse("hello.txt").toKotlinPath(), "txt"))
    assertEquals("hello.txt", it.basename(it.parse("hello.txt").toKotlinPath(), ".bin"))
    assertEquals("hello.txt", it.basename(it.parse("hello.txt").toKotlinPath(), "bin"))
  }

  @Test fun `dirname should accept strings`() = unixPaths().let {
    assertEquals("/sample/cool", it.dirname("/sample/cool/hello"))
    assertEquals("/sample/cool", it.dirname("/sample/cool/hello.txt"))
    assertEquals("sample/cool", it.dirname("sample/cool/hello"))
    assertEquals("sample/cool", it.dirname("sample/cool/hello.txt"))
    assertEquals(".", it.dirname("hello"))
    assertEquals(".", it.dirname("hello.txt"))
  }

  @Test fun `dirname should accept kotlin paths`() = unixPaths().let {
    assertEquals("/sample/cool", it.dirname(it.parse("/sample/cool/hello").toKotlinPath()))
    assertEquals("/sample/cool", it.dirname(it.parse("/sample/cool/hello.txt").toKotlinPath()))
    assertEquals("sample/cool", it.dirname(it.parse("sample/cool/hello").toKotlinPath()))
    assertEquals("sample/cool", it.dirname(it.parse("sample/cool/hello.txt").toKotlinPath()))
    assertEquals(".", it.dirname(it.parse("hello").toKotlinPath()))
    assertEquals(".", it.dirname(it.parse("hello.txt").toKotlinPath()))
  }

  @Test fun `extname should accept strings`() = unixPaths().let {
    assertEquals("", it.extname("/sample/cool/hello"))
    assertEquals(".txt", it.extname("/sample/cool/hello.txt"))
    assertEquals("", it.extname("sample/cool/hello"))
    assertEquals(".txt", it.extname("sample/cool/hello.txt"))
    assertEquals("", it.extname("hello"))
    assertEquals(".txt", it.extname("hello.txt"))
  }

  @Test fun `extname should kotlin paths`() = unixPaths().let {
    assertEquals("", it.extname(it.parse("/sample/cool/hello").toKotlinPath()))
    assertEquals(".txt", it.extname(it.parse("/sample/cool/hello.txt").toKotlinPath()))
    assertEquals("", it.extname(it.parse("sample/cool/hello").toKotlinPath()))
    assertEquals(".txt", it.extname(it.parse("sample/cool/hello.txt").toKotlinPath()))
    assertEquals("", it.extname(it.parse("hello").toKotlinPath()))
    assertEquals(".txt", it.extname(it.parse("hello.txt").toKotlinPath()))
  }

  @Test fun `join should accept strings`() = unixPaths().let {
    assertEquals(it.join("some/cool/path", "another/path"), "some/cool/path/another/path")
    assertEquals(it.join("some/cool/path", "another", "path"), "some/cool/path/another/path")
    assertEquals(it.join("path", "another", "path", "here"), "path/another/path/here")
    assertEquals(it.join("path", "another", "path", "here", "now"), "path/another/path/here/now")
    assertEquals(it.join("some/path/already/joined"), "some/path/already/joined")
    assertEquals(it.join("some/path/already/joined", "another"), "some/path/already/joined/another")
  }

  @Test fun `join should accept parsed paths`() = unixPaths().let {
    assertEquals(
      it.join(it.parse("some/cool/path"), it.parse("another/path")),
      "some/cool/path/another/path",
    )
    assertEquals(
      it.join(it.parse("some/cool/path"), it.parse("another/path")),
      "some/cool/path/another/path",
    )
    assertEquals(
      it.join(it.parse("path"), it.parse("another/path/here")),
      "path/another/path/here",
    )
    assertEquals(
      it.join(it.parse("path"), it.parse("another/path/here/now")),
      "path/another/path/here/now",
    )
    assertEquals(
      it.join(it.parse("some/path/already/joined")),
      "some/path/already/joined",
    )
    assertEquals(
      it.join(it.parse("some/path/already/joined"), it.parse("another")),
      "some/path/already/joined/another",
    )
  }

  @Test fun `join should accept kotlin paths`() = unixPaths().let {
    assertEquals(
      it.join(it.parse("some/cool/path").toKotlinPath(), it.parse("another/path").toKotlinPath()),
      "some/cool/path/another/path",
    )
    assertEquals(
      it.join(it.parse("some/cool/path").toKotlinPath(), it.parse("another/path").toKotlinPath()),
      "some/cool/path/another/path",
    )
    assertEquals(
      it.join(it.parse("path").toKotlinPath(), it.parse("another/path/here").toKotlinPath()),
      "path/another/path/here",
    )
    assertEquals(
      it.join(it.parse("path").toKotlinPath(), it.parse("another/path/here/now").toKotlinPath()),
      "path/another/path/here/now",
    )
    assertEquals(
      it.join(it.parse("some/path/already/joined").toKotlinPath()),
      "some/path/already/joined",
    )
  }

  @Test fun `basename conformance with node`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    val basename = path.basename(parsed)
    assertEquals("path", basename)
    assertEquals("path.txt", path.basename(path.parse("/sample/cool/path.txt")))
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { basename } = require("path");
      equal(basename('/sample/cool/path'), 'path');
      equal(basename('/sample/cool/hello'), 'hello');
      equal(basename('sample/cool/hello'), 'hello');
      equal(basename('sample/cool/path'), 'path');
      equal(basename('sample/cool/hello.txt'), 'hello.txt');
    """
  }

  @Test fun `extname conformance with node`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    val extname = path.extname(parsed)
    assertEquals("", extname)
    assertEquals(".txt", path.extname(path.parse("/sample/cool/path.txt")))
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { extname } = require("path");
      equal(extname('/sample/cool/path'), '');
      equal(extname('/sample/cool/path.txt'), '.txt');
    """
  }

  @Test fun `sep conformance with node (posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    assertEquals("/", path.sep)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { sep } = require("path");
      equal(sep, '/');
    """
  }

  @Test fun `delimiter conformance with node (posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    assertEquals(":", path.delimiter)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { delimiter } = require("path");
      equal(delimiter, ':');
    """
  }

  @Test fun `sep conformance with node (win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    assertEquals("\\", path.sep)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { win32 } = require("path");
      equal(win32.sep, '\\');
    """
  }

  @Test fun `delimiter conformance with node (win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    assertEquals(";", path.delimiter)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { win32 } = require("path");
      equal(win32.delimiter, ';');
    """
  }

  @Test fun `explicitly posix path tools`() = conforms {
    assertNotNull(NodePaths.create(POSIX))
    assertNotNull(NodePaths.create(POSIX).posix)
    assertSame(NodePaths.create(POSIX), NodePaths.create(POSIX))
    assertSame(NodePaths.create(POSIX), NodePaths.create(POSIX).posix)
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { posix } = require("path");
      ok(!!posix);
    """
  }

  @Test fun `explicitly win32 path tools`() = conforms {
    assertNotNull(NodePaths.create(WIN32))
    assertNotNull(NodePaths.create(WIN32).win32)
    assertSame(NodePaths.create(WIN32), NodePaths.create(WIN32))
    assertSame(NodePaths.create(WIN32), NodePaths.create(WIN32).win32)
  }.guest {
    // language=javascript
    """
      const { ok } = require("assert");
      const { win32 } = require("path");
      ok(!!win32);
    """
  }

  @Test fun `isAbsolute conformance with node (posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    assertTrue(parsed.isAbsolute)
    assertTrue(path.isAbsolute(parsed))
    assertFalse(path.isAbsolute(path.parse("sample/cool/path")))
    assertFalse(path.parse("sample/cool/path").isAbsolute)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { isAbsolute } = require("path");
      equal(isAbsolute('/sample/cool/path'), true);
      equal(isAbsolute('sample/cool/path'), false);
    """
  }

  @Ignore("Broken on POSIX") @Test fun `isAbsolute conformance with node (win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    val parsed = assertNotNull(
      path.parse("C:\\sample\\cool\\path", WIN32),
      "should be able to parse a path",
    )
    assertTrue(parsed.isAbsolute)
    assertTrue(path.isAbsolute(parsed))
    assertFalse(path.isAbsolute(path.parse("sample\\cool\\path", WIN32)))
    assertFalse(path.parse("sample\\cool\\path", WIN32).isAbsolute)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { win32 } = require("path");
      const { isAbsolute } = win32;
      equal(isAbsolute('C:\\sample\\cool\\path'), true);
      equal(isAbsolute('sample\\cool\\path'), false);
    """
  }

  @Test fun `relative conformance with node`() = conforms {
    val path = NodePaths.create(POSIX)
    val from = assertNotNull(path.parse("/sample/cool/path"), "should be able to parse a path")
    val to = assertNotNull(path.parse("/sample/cool/path/other"), "should be able to parse another path")
    val relative = path.relative(from, to)
    assertEquals("other", relative)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { relative } = require("path");
      equal(relative('/sample/cool/path', '/sample/cool/path/other').toString(), 'other');
    """
  }

  @Test fun `normalize conformance with node`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path/../other"), "should be able to parse a path")
    val normalized = path.normalize(parsed)
    assertEquals("/sample/cool/other", normalized)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { normalize } = require("path");
      equal(normalize('/sample/cool/path/../other'), '/sample/cool/other');
    """
  }

  @Test fun `resolve conformance with node`() = conforms {
    val path = NodePaths.create(POSIX)
    val resolved = path.resolve(
      path.parse("/sample/cool/path"),
      path.parse("other/cool/path"),
      path.parse("another/cool/path"),
    )
    assertEquals("/sample/cool/path/other/cool/path/another/cool/path", resolved)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse, resolve } = require("path");
      const resolved = resolve(
        '/sample/cool/path',
        'other/cool/path',
        'another/cool/path',
      );
      equal(resolved, '/sample/cool/path/other/cool/path/another/cool/path');
    """
  }

  @Test fun `format with invalid types should fail`() {
    assertFailsWith<IllegalStateException> { unixPaths().format(5) }
    assertFailsWith<IllegalStateException> { windowsPaths().format(5) }
  }

  @Test fun `format conformance with node`() = conforms {
    val parsed = assertNotNull(unixPaths().parse("/sample/cool/path"), "should be able to parse a path")
    val formatted = unixPaths().format(parsed)
    assertEquals("/sample/cool/path", formatted)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse, format } = require("path");
      const parsed = parse('/sample/cool/path');
      const obj = { dir: '/sample/cool', base: 'path' };
      equal(format(parsed), '/sample/cool/path');
      equal(format(obj), '/sample/cool/path');
    """
  }

  @Test fun `path utils should default to posix`() {
    assertEquals(POSIX, assertNotNull(PathUtils.activePathStyle(null)))
  }

  @Test fun `path utils should properly recognize os`() = listOf(
    "posix" to POSIX,
    "POSIX" to POSIX,
    "POSIX " to POSIX,
    "linux" to POSIX,
    "LINUX" to POSIX,
    " LINUX " to POSIX,
    "darwin" to POSIX,
    "DARWIN" to POSIX,
    "something-else" to POSIX,
    "win" to WIN32,
    "win32" to WIN32,
    "windows" to WIN32,
    "WIN" to WIN32,
    "WIN32" to WIN32,
    "WINDOWS" to WIN32,
    " WINDOWS " to WIN32,
  ).forEach { (os, expected) ->
    assertEquals(expected, PathUtils.activePathStyle(os), "should recognize os '$os' as $expected")
  }

  @Test fun `path styles should properly recognize absolute paths`() {
    PathStyle.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertTrue(it.name.isNotEmpty())
    }
    assertFalse(POSIX.isAbsolute(unixPaths().parse("some/relative/path").toKotlinPath()))
    assertTrue(POSIX.isAbsolute(unixPaths().parse("/some/absolute/path").toKotlinPath()))
    assertFalse(WIN32.isAbsolute(windowsPaths().parse("some\\relative\\path").toKotlinPath()))
    assertFalse(WIN32.isAbsolute(windowsPaths().parse("hi").toKotlinPath()))
    assertFalse(WIN32.isAbsolute(windowsPaths().parse("C:hi").toKotlinPath()))
    assertTrue(WIN32.isAbsolute(windowsPaths().parse("C:\\some\\absolute\\path").toKotlinPath()))
  }

  @Test fun `path equals should be lenient (jvm)`() = unixPaths().let {
    assertEquals(
      it.parse("/sample/cool/path"),
      it.parse("/sample/cool/path"),
      "two equivalent parsed paths should be equal",
    )
    assertEquals(
      it.parse("/sample/cool/path").toString(),
      it.parse("/sample/cool/path").toString(),
      "two equivalent parsed paths should be equal as strings",
    )
    assertEquals(
      it.parse("sample/cool/path").toString(),
      it.parse("sample/cool/path").toString(),
      "two equivalent relative parsed paths should be equal",
    )
    assertTrue(
      it.parse("/sample/cool/path").equals(it.parse("/sample/cool/path").toString()),
      "parsed path should be equal to its string representation",
    )
    assertTrue(
      it.parse("/sample/cool/path").equals(it.parse("/sample/cool/path").toKotlinPath()),
      "parsed path should be equal to its kotlin path equivalent",
    )
    assertTrue(
      it.parse("/sample/cool/path") == it.parse("/sample/cool/path").toJavaPath(),
      "parsed path should be equal to its java path equivalent",
    )
  }

  @Test fun `path parse should yield path object (absolute dir)`() = conforms {
    unixPaths().let {
      val parsed = assertNotNull(it.parse("/sample/cool/path"))
      assertIs<PathIntrinsic>(parsed)
      assertEquals("/sample/cool/path", parsed.toString())
      assertEquals("/sample/cool", parsed.dir)
      assertEquals("path", parsed.base)
      assertEquals("path", parsed.name)
      assertEquals("", parsed.ext)
      assertEquals("/", parsed.root)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse } = require("path");
      const parsed = parse('/sample/cool/path');
      equal(parsed.dir, '/sample/cool');
      equal(parsed.base, 'path');
      equal(parsed.name, 'path');
      equal(parsed.ext, '');
      equal(parsed.root, '/');
    """
  }

  @Test fun `path parse should yield path object (absolute file)`() = conforms {
    unixPaths().let {
      val parsed = assertNotNull(it.parse("/sample/cool/path.txt"))
      assertIs<PathIntrinsic>(parsed)
      assertEquals("/sample/cool/path.txt", parsed.toString())
      assertEquals("/sample/cool", parsed.dir)
      assertEquals("path.txt", parsed.base)
      assertEquals("path", parsed.name)
      assertEquals(".txt", parsed.ext)
      assertEquals("/", parsed.root)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse } = require("path");
      const parsed = parse('/sample/cool/path.txt');
      equal(parsed.dir, '/sample/cool');
      equal(parsed.base, 'path.txt');
      equal(parsed.name, 'path');
      equal(parsed.ext, '.txt');
      equal(parsed.root, '/');
    """
  }

  @Test fun `path parse should yield path object (relative dir)`() = conforms {
    unixPaths().let {
      val parsed = assertNotNull(it.parse("sample/cool/path"))
      assertIs<PathIntrinsic>(parsed)
      assertEquals("sample/cool/path", parsed.toString())
      assertEquals("sample/cool", parsed.dir)
      assertEquals("path", parsed.base)
      assertEquals("path", parsed.name)
      assertEquals("", parsed.ext)
      assertEquals("", parsed.root)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse } = require("path");
      const parsed = parse('sample/cool/path');
      equal(parsed.dir, 'sample/cool');
      equal(parsed.base, 'path');
      equal(parsed.name, 'path');
      equal(parsed.ext, '');
      equal(parsed.root, '');
    """
  }

  @Test fun `path parse should yield path object (relative file)`() = conforms {
    unixPaths().let {
      val parsed = assertNotNull(it.parse("sample/cool/path.txt"))
      assertIs<PathIntrinsic>(parsed)
      assertEquals("sample/cool/path.txt", parsed.toString())
      assertEquals("sample/cool", parsed.dir)
      assertEquals("path.txt", parsed.base)
      assertEquals("path", parsed.name)
      assertEquals(".txt", parsed.ext)
      assertEquals("", parsed.root)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse } = require("path");
      const parsed = parse('sample/cool/path.txt');
      equal(parsed.dir, 'sample/cool');
      equal(parsed.base, 'path.txt');
      equal(parsed.name, 'path');
      equal(parsed.ext, '.txt');
      equal(parsed.root, '');
    """
  }

  @Ignore @Test fun `path format should yield expected path (absolute dir)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("/sample/cool/path"))
      assertEquals("/sample/cool/path", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse, format } = require("path");
      const parsed = parse('/sample/cool/path');
      equal(format(parsed), '/sample/cool/path');
    """
  }

  @Test fun `path format should yield expected path (absolute file)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("/sample/cool/path.txt"))
      assertEquals("/sample/cool/path.txt", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse, format } = require("path");
      const parsed = parse('/sample/cool/path.txt');
      equal(format(parsed), '/sample/cool/path.txt');
    """
  }

  @Ignore @Test fun `path format should yield expected path (relative dir)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("sample/cool/path"))
      assertEquals("sample/cool/path", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse, format } = require("path");
      const parsed = parse('sample/cool/path');
      equal(format(parsed), 'sample/cool/path');
    """
  }

  @Ignore @Test fun `path format should yield expected path (relative file)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("sample/cool/path.txt"))
      assertEquals("sample/cool/path.txt", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { parse, format } = require("path");
      const parsed = parse('sample/cool/path.txt');
      equal(format(parsed), 'sample/cool/path.txt');
    """
  }

  @Ignore @Test fun `path format should be able to render dir-only paths (non-root)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("sample/cool"))
      assertEquals("sample/cool", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ dir: 'sample/cool' }), 'sample/cool/');
    """
  }

  @Ignore @Test fun `path format should be able to render dir-only paths (root)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("/sample/cool"))
      assertEquals("/sample/cool", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ dir: 'sample/cool', root: '/' }), 'sample/cool/');
    """
  }

  @Test fun `path format should be able to render name-only paths (non-root)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("sample/cool"))
      assertEquals("sample/cool", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: 'sample' }), 'sample');
    """
  }

  @Test fun `path format should be able to render name-only paths (root)`() = conforms {
    unixPaths().let {
      val formatted = it.format(it.parse("sample/cool"))
      assertEquals("sample/cool", formatted)
    }
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: 'sample', root: '/' }), '/sample');
    """
  }

  @Test fun `path format should be tolerant of nulls (root)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: 'sample', ext: '.txt', root: null }), 'sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (dir)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: 'sample', ext: '.txt', dir: null }), 'sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (base)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: 'sample', ext: '.txt', base: null }), 'sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (name)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: null, base: 'sample.txt' }), 'sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (ext)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ ext: null, base: 'sample.txt' }), 'sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (root, with parent)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ root: null, dir: 'hello/sample', base: 'sample.txt' }), 'hello/sample/sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (name, with parent)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ name: null, dir: 'hello/sample', base: 'sample.txt' }), 'hello/sample/sample.txt');
    """
  }

  @Test fun `path format should be tolerant of nulls (ext, with parent)`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ ext: null, dir: 'hello/sample', base: 'sample.txt' }), 'hello/sample/sample.txt');
    """
  }

  @Test fun `path format should ignore name and ext if base is present`() = conforms {
    // nothing host-side to test
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { format } = require("path");
      equal(format({ ext: null, dir: 'hello/sample', base: 'sample.txt' }), 'hello/sample/sample.txt');
    """
  }

  @Test fun `pathbuf factory should accept string paths`() {
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from("some", "cool", "path")).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from("some/cool", "path")).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from("some/cool/path")).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from("some/cool/path", POSIX)).toString(),
    )
    assertEquals(
      "some\\cool\\path",
      assertNotNull(PathBuf.from("some\\cool\\path", WIN32)).toString(),
    )
  }

  @Test fun `pathbuf factory should accept kotlin paths`() {
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from(KotlinPath("some", "cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from(KotlinPath("some/cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from(KotlinPath("some/cool/path"))).toString(),
    )
  }

  @Test fun `pathbuf factory should accept java paths`() {
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from(Paths.get("some", "cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from(Paths.get("some/cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathBuf.from(Paths.get("some/cool/path"))).toString(),
    )
  }

  @Test fun `path factory should accept string paths`() {
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from("some", "cool", "path")).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from("some/cool", "path")).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from("some/cool/path")).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from("some/cool/path", POSIX)).toString(),
    )
    assertEquals(
      "some\\cool\\path",
      assertNotNull(PathIntrinsic.from("some\\cool\\path", WIN32)).toString(),
    )
  }

  @Test fun `path factory should accept kotlin paths`() {
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from(KotlinPath("some", "cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from(KotlinPath("some/cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from(KotlinPath("some/cool/path"))).toString(),
    )
  }

  @Test fun `path factory should accept java paths`() {
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from(Paths.get("some", "cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from(Paths.get("some/cool", "path"))).toString(),
    )
    assertEquals(
      "some/cool/path",
      assertNotNull(PathIntrinsic.from(Paths.get("some/cool/path"))).toString(),
    )
  }

  @Test fun `asking the posix path factory to parse windows paths should error`() {
    assertFailsWith<IllegalArgumentException> {
      NodePaths.create(POSIX).parse("C:\\sample\\cool\\path", WIN32)
    }
  }

  @Test fun `asking the windows path factory to parse posix paths should error`() {
    assertFailsWith<IllegalArgumentException> {
      NodePaths.create(WIN32).parse("/sample/cool/path", POSIX)
    }
  }

  @Test fun `normalize conformance with node (absolute, posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("/sample/cool/path/../other"), "should be able to parse a path")
    val normalized = path.normalize(parsed)
    assertEquals("/sample/cool/other", normalized)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { normalize } = require("path");
      equal(normalize('/sample/cool/path/../other'), '/sample/cool/other');
    """
  }

  @Ignore("Broken on POSIX") @Test fun `normalize conformance with node (absolute, win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    val parsed = assertNotNull(
      path.parse("C:\\sample\\cool\\path\\..\\other", WIN32),
      "should be able to parse a path",
    )
    val normalized = path.normalize(parsed)
    assertEquals("C:\\sample\\cool\\other", normalized)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { win32 } = require("path");
      const { normalize } = win32;
      // must be wrapped in `parse` for cross-platform use, since this is usually run on unix-style systems
      equal(normalize('C:\\sample\\cool\\path\\..\\other'), 'C:\\sample\\cool\\other');
    """
  }

  @Test fun `normalize conformance with node (relative, posix)`() = conforms {
    val path = NodePaths.create(POSIX)
    val parsed = assertNotNull(path.parse("sample/cool/path/../other"), "should be able to parse a path")
    val normalized = path.normalize(parsed)
    assertEquals("sample/cool/other", normalized)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { normalize } = require("path");
      equal(normalize('sample/cool/path/./other'), 'sample/cool/path/other');
      equal(normalize('sample/cool/path/././other'), 'sample/cool/path/other');
      equal(normalize('sample/cool/path/./././other'), 'sample/cool/path/other');
      equal(normalize('sample/cool/path/../other'), 'sample/cool/other');
      equal(normalize('sample/cool/path/../../other'), 'sample/other');
      equal(normalize('sample/cool/path/../.././other'), 'sample/other');
      equal(normalize('sample/cool/path/../../././other'), 'sample/other');
    """
  }

  @Ignore("Broken on POSIX") @Test fun `normalize conformance with node (relative, win32)`() = conforms {
    val path = NodePaths.create(WIN32)
    val parsed = assertNotNull(
      path.parse("sample\\cool\\path\\..\\other", WIN32),
      "should be able to parse a path",
    )
    val normalized = path.normalize(parsed)
    assertEquals("sample\\cool\\other", normalized)
  }.guest {
    // language=javascript
    """
      const { equal } = require("assert");
      const { win32 } = require("path");
      const { normalize, parse } = win32;
      // must be wrapped in `parse` for cross-platform use, since this is usually run on unix-style systems
      equal(normalize('sample\\cool\\path\\.\\.\\other'), 'sample\\cool\\path\\other');
      equal(normalize('sample\\cool\\path\\..\\other'), 'sample\\cool\\other');
      equal(normalize('sample\\cool\\path\\.\\.\\.\\other'), 'sample\\cool\\path\\other');
      equal(normalize('sample\\cool\\path\\..\\..\\other'), 'sample\\other');
      equal(normalize('sample\\cool\\path\\..\\..\\.\\other'), 'sample\\other');
      equal(normalize('sample\\cool\\path\\..\\..\\.\\.\\other'), 'sample\\other');
    """
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "/sample/cool/path, /sample/cool/path",
      "/sample/cool/path.txt, /sample/cool/path.txt",
      "sample/cool/path, sample/cool/path",
      "sample/cool/path.txt, sample/cool/path.txt",
      "/sample/cool/../path, /sample/path",
      "/sample/cool/../path.txt, /sample/path.txt",
      "sample/cool/../path, sample/path",
      "sample/cool/../path.txt, sample/path.txt",
      "path, path",
      "path.txt, path.txt",
      "/path, /path",
      "/path.txt, /path.txt",
      "sample/././././path, sample/path",
      "sample/././././path.txt, sample/path.txt",
      "sample/cool/././././path, sample/cool/path",
      "sample/cool/././././path.txt, sample/cool/path.txt",
      "sample/cool/././../path, sample/path",
    ],
  )
  fun normalize(path: String, expected: String) {
    val parsed = unixPaths().parse(path)
    val normalized = unixPaths().normalize(parsed)
    assertEquals(expected, normalized)
    assertEquals(unixPaths().normalize(path), normalized)
    assertEquals(
      normalized,
      node(
        // language=js
        """
          const { normalize } = require("path");
          output(normalize("$path"));
        """,
      ),
      "should normalize to same output as node",
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "/sample/cool, /somepath, /sample/cool/somepath",
      "/sample/cool, somepath, /sample/cool/somepath",
      "sample/cool, /somepath, sample/cool/somepath",
      "sample/cool, somepath, sample/cool/somepath",
      "/sample/cool, /somepath/another, /sample/cool/somepath/another",
      "/sample/cool, somepath/another, /sample/cool/somepath/another",
      "sample/cool, /somepath/another, sample/cool/somepath/another",
      "/sample/cool, /somepath.txt, /sample/cool/somepath.txt",
      "/sample/cool, somepath.txt, /sample/cool/somepath.txt",
      "sample/cool, /somepath.txt, sample/cool/somepath.txt",
      "sample/cool, somepath.txt, sample/cool/somepath.txt",
      "/sample/cool, /somepath/another.txt, /sample/cool/somepath/another.txt",
      "/sample/cool, somepath/another.txt, /sample/cool/somepath/another.txt",
      "hello, hi, hello/hi",
      "hello, hi.txt, hello/hi.txt",
      "hello, hi/there, hello/hi/there",
      "/hello, hi, /hello/hi",
      "/hello, hi.txt, /hello/hi.txt",
      "/hello, hi/there, /hello/hi/there",
    ],
  )
  fun join(left: String, right: String, expected: String) {
    val parsedL = unixPaths().parse(left)
    val parsedR = unixPaths().parse(right)
    val joined = unixPaths().join(parsedL, parsedR)
    val joinedStr = unixPaths().join(left, right)
    assertEquals(expected, joined)
    assertEquals(expected, joinedStr)
    assertEquals(
      joined,
      node(
        // language=js
        """
          const { join } = require("path");
          output(join("$left", "$right"));
        """,
      ),
      "should join to same output as node",
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "/sample/cool/path, path",
      "/sample/cool/path.txt, path.txt",
      "sample/cool/path, path",
      "sample/cool/path.txt, path.txt",
      "/sample/cool/../path, path",
      "/sample/cool/../path.txt, path.txt",
      "sample/cool/../path, path",
      "sample/cool/../path.txt, path.txt",
      "path, path",
      "path.txt, path.txt",
      "/path, path",
      "/path.txt, path.txt",
      "sample/././././path, path",
      "sample/././././path.txt, path.txt",
      "sample/cool/././././path, path",
      "sample/cool/././././path.txt, path.txt",
      "sample/cool/././../path, path",
    ],
  )
  fun basename(value: String, expected: String) {
    val parsed = unixPaths().parse(value)
    val basename = unixPaths().basename(parsed)
    val basenameStr = unixPaths().basename(value)
    assertEquals(expected, basename)
    assertEquals(expected, basenameStr)
    assertEquals(
      basename,
      node(
        // language=js
        """
          const { basename } = require("path");
          output(basename("$value"));
        """,
      ),
      "basename result should be same as node",
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "/sample/cool/path, /sample/cool",
      "/sample/cool/path.txt, /sample/cool",
      "sample/cool/path, sample/cool",
      "sample/cool/path.txt, sample/cool",
      "path, .",
      "path.txt, .",
      "sample/././././path, sample/./././.",
      "sample/cool/././././path, sample/cool/./././.",
    ],
  )
  fun dirname(value: String, expected: String?) {
    val parsed = unixPaths().parse(value)
    val dirname = unixPaths().dirname(parsed)
    val dirnameStr = unixPaths().dirname(value)
    assertEquals(expected ?: "", dirname)
    assertEquals(expected ?: "", dirnameStr)
    assertEquals(
      dirname,
      node(
        // language=js
        """
          const { dirname } = require("path");
          output(dirname("$value"));
        """,
      ),
      "dirname result should be same as node",
    )
  }
}

