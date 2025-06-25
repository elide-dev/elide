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

package elide.tooling

import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.test.*

class MultiPathTests {
  @Test fun testSimpleClasspath() {
    val somePath = Paths.get("/some/example/path")
    val someJar = Paths.get("/some/example/path/to/a/file.jar")
    val classpath = Classpath.of(somePath, someJar)
    assertNotNull(classpath)
    assertEquals(2, classpath.size)
    assertTrue(somePath in classpath)
    assertTrue(somePath.absolutePathString() in classpath)
    assertTrue(someJar in classpath)
    assertTrue(someJar.absolutePathString() in classpath)
    assertFalse("made-up-path" in classpath)
    assertFalse(Paths.get("/made-up-path") in classpath)
    val fmt = classpath.asArgumentString()
    assertEquals("/some/example/path:/some/example/path/to/a/file.jar", fmt)
    val other = classpath + somePath
    assertNotNull(other)
    assertEquals(3, other.size)
    assertEquals(2, classpath.size) // should not mutate original
    val someOtherPath = Paths.get("/some/other/path")
    val other2 = classpath + someOtherPath
    assertNotNull(other2)
  }

  @Test fun testSimpleModulepath() {
    val somePath = Paths.get("/some/example/path")
    val someJar = Paths.get("/some/example/path/to/a/file.jar")
    val someJmod = Paths.get("/some/example/path/to/a/file.jmod")
    val modulepath = Modulepath.of(somePath, someJar, someJmod)
    assertNotNull(modulepath)
    assertEquals(3, modulepath.size)
    assertTrue(somePath in modulepath)
    assertTrue(somePath.toString() in modulepath)
    assertTrue(someJar in modulepath)
    assertTrue(someJar.absolutePathString() in modulepath)
    assertTrue(someJmod in modulepath)
    assertTrue(someJmod.absolutePathString() in modulepath)
    assertFalse("made-up-path" in modulepath)
    assertFalse(Paths.get("/made-up-path") in modulepath)
    val fmt = modulepath.asArgumentString()
    assertEquals(
      "/some/example/path:/some/example/path/to/a/file.jar:/some/example/path/to/a/file.jmod",
      fmt,
    )
    val other = modulepath + somePath
    assertNotNull(other)
    assertEquals(4, other.size)
    assertEquals(3, modulepath.size) // should not mutate original
    val someOtherPath = Paths.get("/some/other/path")
    val other2 = modulepath + someOtherPath
    assertNotNull(other2)
  }
}
