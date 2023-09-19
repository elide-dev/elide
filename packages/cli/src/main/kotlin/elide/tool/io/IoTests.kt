/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.io

import io.micronaut.context.annotation.Bean
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.charset.StandardCharsets.UTF_8
import elide.tool.annotations.EmbeddedTest
import elide.tool.io.WorkdirManager.WorkdirHandle
import elide.tool.testing.SelfTest
import elide.tool.testing.SelfTest.SelfTestContext
import elide.tool.testing.TestContext

/** Utility to check I/O assumptions. */
private suspend fun SelfTestContext.assertGuarantees(
  file: WorkdirHandle,
  writable: Boolean = false,
  readable: Boolean = false,
  exists: Boolean = true,
) {
  val doesExist = file.exists()
  if (exists) assertTrue(doesExist, "file `$file` should exist")
  if (exists && readable) assertTrue(file.canRead(), "file `$file` should be readable")
  if (exists && writable) assertTrue(file.canWrite(), "file `$file` should be writable")
}

/** Basic runtime working directory test. */
@Bean @EmbeddedTest class WorkdirTest : SelfTest() {
  override suspend fun SelfTestContext.test() = TestContext.assertDoesNotThrow {
    RuntimeWorkdirManager.acquire()
  }.let {
    assertNotNull(it, "should be able to acquire the runtime workdir manager")
    assertNotNull(it.workingRoot(), "temporary workdir should not be `null`")
    assertNotNull(it.cacheDirectory(), "cache directory should not be `null`")
    assertNotNull(it.flightRecorderDirectory(), "flight recorder directory should not be `null`")
    assertNotNull(it.nativesDirectory(), "natives directory should not be null")
    assertGuarantees(it.tmpDirectory(), writable = true, exists = false)
    assertGuarantees(it.cacheDirectory(), readable = true, writable = true, exists = false)
    assertGuarantees(it.nativesDirectory(), readable = true, writable = true, exists = true)
    assertGuarantees(it.flightRecorderDirectory(), readable = true, writable = true, exists = false)
  }
}

/** Write something to the temp space. */
@Bean @EmbeddedTest class TempWriteTest : SelfTest() {
  override suspend fun SelfTestContext.test() = TestContext.assertDoesNotThrow {
    RuntimeWorkdirManager.acquire()
  }.let { manager ->
    assertNotNull(manager, "should be able to acquire the runtime workdir manager")
    assertNotNull(manager.tmpDirectory(), "temporary directory should not be `null`")
    assertGuarantees(manager.tmpDirectory(), writable = true, exists = false)
    assertDoesNotThrow {
      manager.tmpDirectory().resolve("test-write.txt").let { target ->
        target.deleteOnExit()
        target.outputStream().bufferedWriter(UTF_8).use {
          it.write("hello")
        }
      }
    }
  }
}

/** Write something to the cache space. */
@Bean @EmbeddedTest class TestCacheWrite : SelfTest() {
  override suspend fun SelfTestContext.test() = TestContext.assertDoesNotThrow {
    RuntimeWorkdirManager.acquire()
  }.let { manager ->
    assertNotNull(manager, "should be able to acquire the runtime workdir manager")
    assertNotNull(manager.cacheDirectory(), "cache directory should not be `null`")
    assertGuarantees(manager.cacheDirectory(), readable = true, writable = true, exists = false)
    assertDoesNotThrow {
      manager.cacheDirectory().resolve("test-write.txt").let { target ->
        target.deleteOnExit()
        target.outputStream().bufferedWriter(UTF_8).use {
          it.write("hello")
        }
      }
    }
  }
}
