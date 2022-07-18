package elide.server.runtime

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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
