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
package elide.tooling.jvm.resolver

import java.nio.file.Path
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.testing.annotations.Test
import elide.tooling.ClasspathSpec
import elide.tooling.lockfile.ElideLockfile
import elide.tooling.lockfile.LockfileStanza
import elide.tooling.lockfile.loadLockfile
import elide.tooling.lockfile.typedStanza

class MavenLockfileResolverTest {
  @Test fun testLoadFromElidesBinaryLockfile() = runTest {
    val projectRoot = requireNotNull(System.getProperty("elide.root")) {
      "Failed to find project root path"
    }.let {
      Path.of(it)
    }
    val lockfile = coroutineScope {
      loadLockfile(projectRoot).await()
    }
    val mavenLockfile = requireNotNull(
      lockfile?.lockfile?.typedStanza<ElideLockfile.MavenLockfile>(LockfileStanza.MAVEN)
    )
    val resolver = MavenLockfileResolver.of(
      mavenLockfile,
      projectRoot,
    )
    assertNotNull(resolver)
  }

  @Test fun testLoadElideCompileClasspath() = runTest {
    val projectRoot = requireNotNull(System.getProperty("elide.root")) {
      "Failed to find project root path"
    }.let {
      Path.of(it)
    }
    val lockfile = coroutineScope {
      loadLockfile(projectRoot).await()
    }
    val mavenLockfile = requireNotNull(
      lockfile?.lockfile?.typedStanza<ElideLockfile.MavenLockfile>(LockfileStanza.MAVEN)
    )
    val resolver = MavenLockfileResolver.of(
      mavenLockfile,
      projectRoot,
    )
    assertNotNull(resolver)

    val compileClasspath = resolver.classpathProvider(ClasspathSpec.CompileClasspath)?.classpath()
    assertNotNull(compileClasspath)
    assertTrue(compileClasspath.isNotEmpty())
  }

  @Test fun testLoadElideTestCompileClasspath() = runTest {
    val projectRoot = requireNotNull(System.getProperty("elide.root")) {
      "Failed to find project root path"
    }.let {
      Path.of(it)
    }
    val lockfile = coroutineScope {
      loadLockfile(projectRoot).await()
    }
    val mavenLockfile = requireNotNull(
      lockfile?.lockfile?.typedStanza<ElideLockfile.MavenLockfile>(LockfileStanza.MAVEN)
    )
    val resolver = MavenLockfileResolver.of(
      mavenLockfile,
      projectRoot,
    )
    assertNotNull(resolver)

    val testCompileClasspath = resolver.classpathProvider(ClasspathSpec.TestCompile)?.classpath()
    assertNotNull(testCompileClasspath)
    assertTrue(testCompileClasspath.isNotEmpty())
  }

  @Test fun testLoadElideRuntimeClasspath() = runTest {
    val projectRoot = requireNotNull(System.getProperty("elide.root")) {
      "Failed to find project root path"
    }.let {
      Path.of(it)
    }
    val lockfile = coroutineScope {
      loadLockfile(projectRoot).await()
    }
    val mavenLockfile = requireNotNull(
      lockfile?.lockfile?.typedStanza<ElideLockfile.MavenLockfile>(LockfileStanza.MAVEN)
    )
    val resolver = MavenLockfileResolver.of(
      mavenLockfile,
      projectRoot,
    )
    assertNotNull(resolver)

    val runtimeClasspath = resolver.classpathProvider(ClasspathSpec.Runtime)?.classpath()
    assertNotNull(runtimeClasspath)
    assertTrue(runtimeClasspath.isNotEmpty())
  }

  @Test fun testLoadElideTestRuntimeClasspath() = runTest {
    val projectRoot = requireNotNull(System.getProperty("elide.root")) {
      "Failed to find project root path"
    }.let {
      Path.of(it)
    }
    val lockfile = coroutineScope {
      loadLockfile(projectRoot).await()
    }
    val mavenLockfile = requireNotNull(
      lockfile?.lockfile?.typedStanza<ElideLockfile.MavenLockfile>(LockfileStanza.MAVEN)
    )
    val resolver = MavenLockfileResolver.of(
      mavenLockfile,
      projectRoot,
    )
    assertNotNull(resolver)

    val testRuntimeClasspath = resolver.classpathProvider(ClasspathSpec.TestRuntime)?.classpath()
    assertNotNull(testRuntimeClasspath)
    assertTrue(testRuntimeClasspath.isNotEmpty())
  }
}
