@file:OptIn(ExperimentalCoroutinesApi::class)

package elide.runtime.graalvm

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.graalvm.polyglot.Context as VMContext
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

/** Tests for the embedded [JsRuntime]. */
class JsRuntimeTest {
  companion object {
    const val testJsonCode = "(function(x,y) { return JSON.stringify({x:x,y:y}); })"
  }

  private fun testContext(): VMContext.Builder {
    return VMContext.newBuilder("js")
      .allowExperimentalOptions(true)
      .allowValueSharing(true)
  }

  @Test fun testJsRuntimeAcquire() {
    assertNotNull(JsRuntime.acquire(), "should be able to acquire JS runtime instance")
  }

  @Test fun testConcurrentEval() {
    val startGate = CountDownLatch(1)
    val endGate = CountDownLatch(1)
    val hadException = AtomicBoolean(false)
    val ctx = testContext().build()
    val contextLock = ReentrantLock()

    ctx.enter()
    val json = ctx.eval("js", testJsonCode)
    ctx.leave()

    // prep a background thread
    val t = Thread {
      try {
        startGate.await()
        try {
          for (i in 0..10000) {
            contextLock.lock()
            try {
              try {
                ctx.enter()
                val encoded = json.execute(42, 43).asString()
                assertEquals("{\"x\":42,\"y\":43}", encoded)
              } finally {
                ctx.leave()
              }
            } catch (ise: IllegalStateException) {
              hadException.set(true)
            } finally {
              contextLock.unlock()
            }
          }
        } finally {
          endGate.countDown()
        }
      } catch (_: InterruptedException) {}
    }

    try {
      t.start()
      startGate.countDown()
      for (i in 0..10000) {
        try {
          contextLock.lock()
          ctx.enter()
          val encoded = json.execute(42, 43).asString()
          assertEquals("{\"x\":42,\"y\":43}", encoded)
          ctx.leave()
        } finally {
          contextLock.unlock()
        }
      }
      endGate.await()
      t.join(10000)
    } catch (ixe: InterruptedException) {
      throw AssertionError(ixe)
    } finally {
      ctx.close()
    }
    assertFalse(hadException.get(), "should not get concurrent access exception")
  }

  @Test fun testSuspendingEval() = runTest {
    val startGate = Semaphore(1)
    val endGate = Semaphore(1)
    val hadException = AtomicBoolean(false)
    val ctx = testContext().build()
    val contextLock = Mutex()
    val executor = Executors.newFixedThreadPool(2)
    val pool = executor.asCoroutineDispatcher()

    ctx.enter()
    val json = ctx.eval("js", testJsonCode)
    ctx.leave()

    val job1Result = AtomicReference("")
    val job1 = launch(pool, start = CoroutineStart.ATOMIC) {
      startGate.acquire()
      try {
        for (i in 0..10000) {
          contextLock.withLock {
            try {
              ctx.enter()
              job1Result.set(json.execute(42, 43).asString())
            } catch (ise: IllegalStateException) {
              hadException.set(true)
            } finally {
              ctx.leave()
            }
          }
        }
      } finally {
        endGate.release()
      }
    }

    val job2Result = AtomicReference("")
    val job2 = launch(pool, start = CoroutineStart.LAZY) {
      startGate.release()
      for (i in 0..10000) {
        contextLock.withLock {
          try {
            ctx.enter()
            job2Result.set(json.execute(42, 43).asString())
          } catch (ise: IllegalStateException) {
            hadException.set(true)
          } finally {
            ctx.leave()
          }
        }
      }
    }
    job1.start()
    job2.start()
    endGate.acquire()

    try {
      job1.join()
      job2.join()
      assertEquals("{\"x\":42,\"y\":43}", job1Result.get())
      assertEquals("{\"x\":42,\"y\":43}", job2Result.get())
    } finally {
      ctx.close(true)
      pool.close()
      executor.shutdown()
    }
  }
}
