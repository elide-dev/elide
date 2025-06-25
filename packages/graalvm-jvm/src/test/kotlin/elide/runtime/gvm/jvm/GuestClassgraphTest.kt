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
package elide.runtime.gvm.jvm

import kotlinx.coroutines.test.runTest
import elide.testing.annotations.Test
import elide.tooling.Classpath
import kotlin.test.*

class GuestClassgraphTest {
  @Test fun testClassgraph() = runTest {
    val classpath = Classpath.fromOriginOf(GuestClassgraphTest::class)
    assertNotNull(classpath, "should be able to build classpath container on jvm")
    val classgraph = GuestClassgraph.buildFrom(classpath)
    assertNotNull(classgraph, "should be able to build classgraph from classpath")
    classgraph.scanResult().use {
      val classes = it.allClasses
      assertNotNull(classes)
      val thisTest = classes.get("elide.runtime.gvm.jvm.GuestClassgraphTest")
      assertNotNull(thisTest, "failed to locate known-good class in jar")
    }
  }

  @Test fun testClassgraphScanForAnnotation() = runTest {
    val classpath = Classpath.fromOriginOf(GuestClassgraphTest::class)
    assertNotNull(classpath, "should be able to build classpath container on jvm")
    val classgraph = GuestClassgraph.buildFrom(classpath)
    assertNotNull(classgraph, "should be able to build classgraph from classpath")
    classgraph.scanResult().use {
      val classes = it.allClasses
      assertNotNull(classes)
      val thisTest = classes.get("elide.runtime.gvm.jvm.GuestClassgraphTest")
      assertNotNull(thisTest, "failed to locate known-good class in jar")
      val testMethods = thisTest.declaredMethodInfo.filter {
        it.hasAnnotation(Test::class.java)
      }
      assertNotNull(testMethods)
      assertTrue(testMethods.isNotEmpty())
    }
  }

  @Test fun testClasspathBuilder() = runTest {
    val classpath = Classpath.fromOriginOf(GuestClassgraphTest::class)
    assertNotNull(classpath, "should be able to build classpath container on jvm")
    val classgraph = GuestClassgraph.buildFrom(classpath) {
      classgraph.apply {
        disableNestedJarScanning()
      }
    }
    assertNotNull(classgraph, "should be able to build classgraph from classpath")
    classgraph.scanResult().use {
      val classes = it.allClasses
      assertNotNull(classes)
      val thisTest = classes.get("elide.runtime.gvm.jvm.GuestClassgraphTest")
      assertNotNull(thisTest, "failed to locate known-good class in jar")
      val testMethods = thisTest.declaredMethodInfo.filter {
        it.hasAnnotation(Test::class.java)
      }
      assertNotNull(testMethods)
      assertTrue(testMethods.isNotEmpty())
    }
  }
}
