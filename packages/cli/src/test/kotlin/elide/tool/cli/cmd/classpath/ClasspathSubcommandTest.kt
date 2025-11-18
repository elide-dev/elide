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
@file:Suppress("MnInjectionPoints")

package elide.tool.cli.cmd.classpath

import io.micronaut.configuration.picocli.PicocliRunner
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.test.*
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest
import elide.tooling.cli.Statics

/** Tests for the main CLI tool `classpath` entrypoint. */
@TestCase class ClasspathSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var classpath: ClasspathCommand

  override fun subcommand(): Runnable = Runnable {
    classpath.call()
  }

  @Test fun testEntrypoint() {
    assertNotNull(classpath, "should be able to init and inject classpath subcommand")
  }

  override fun runCommand() {
    assertDoesNotThrow {
      assertEquals(0, PicocliRunner.execute(ClasspathCommand::class.java, "--help"))
    }
  }

  private fun projectRoot(): Path {
    val rootProjectPath = Paths.get(System.getProperty("user.dir"))

    assertTrue(
      rootProjectPath.resolve("elide.pkl").exists(),
      "Root project path '$rootProjectPath' is incorrect: no elide pkl file",
    )
    return rootProjectPath
  }

  @Test fun `should be able to emit root project classpath`() {
    assertEquals(0, PicocliRunner.execute(ClasspathCommand::class.java, "-p", projectRoot().absolutePathString()))
  }

  @Test fun `should fail if there is no project or no deps`() {
    assertEquals(-1, PicocliRunner.execute(
      ClasspathCommand::class.java,
      "-p",
      Files.createTempDirectory("elide-test").absolutePathString(),
    ))
  }

  @Test @Ignore fun `should be able to emit root project classpath with absolute paths`() {
    assertEquals(0, PicocliRunner.execute(
      ClasspathCommand::class.java,
      "-p", projectRoot().absolutePathString(),
      "--absolute",
    ))
  }

  @Test @Ignore fun `should be able to emit root project classpath with relative paths`() {
    assertEquals(0, PicocliRunner.execute(
      ClasspathCommand::class.java,
      "-p", projectRoot().absolutePathString(),
      "--no-absolute",
    ))
  }

  @Test @Ignore fun `should be able to emit root project classpath with usage`() {
    assertEquals(0, PicocliRunner.execute(
      ClasspathCommand::class.java,
      "-p", projectRoot().absolutePathString(),
      "compile",
    ))
  }

  @Test @Ignore fun `should be able to emit root project classpath with multiple usages`() {
    assertEquals(0, PicocliRunner.execute(
      ClasspathCommand::class.java,
      "-p", projectRoot().absolutePathString(),
      "compile",
      "test",
    ))
  }
}
