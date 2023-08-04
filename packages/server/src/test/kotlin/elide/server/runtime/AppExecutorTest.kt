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

package elide.server.runtime

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertDoesNotThrow
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the [AppExecutor] interface and default app executor. */
@MicronautTest class AppExecutorTest {
  @Inject lateinit var executor: AppExecutor

  @Test fun testInjectable() {
    assertNotNull(executor, "should be able to acquire an app executor via DI")
  }

  @Test fun testAcquire() {
    assertNotNull(
      AppExecutor.DefaultExecutor.acquire(),
      "should be able to acquire default executor statically"
    )
  }

  @Test fun testExecutorService() {
    assertNotNull(
      AppExecutor.DefaultExecutor.acquire().service(),
      "should be able to acquire default executor as `ExecutorService`"
    )
    assertNotNull(
      AppExecutor.DefaultExecutor.acquire().executor(),
      "should be able to acquire default executor as `Executor`"
    )
  }

  @Test @Disabled fun testRunAsyncTask() {
    val result = assertDoesNotThrow {
      runBlocking {
        AppExecutor.async {
          return@async 5
        }.await()
      }
    }
    assertEquals(
      5,
      result,
      "should be able to run task on `async` scheduler and retrieve result"
    )
  }

  @Test fun testRunIOTask() {
    val result = assertDoesNotThrow {
      runBlocking {
        AppExecutor.io {
          return@io 5
        }
      }
    }
    assertEquals(
      5,
      result,
      "should be able to run task on `io` scheduler and retrieve result"
    )
  }
}
