@file:OptIn(ExperimentalCoroutinesApi::class)

package elide.runtime.graalvm

import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.util.DaemonThreadFactory
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graalvm.polyglot.Value
import org.junit.jupiter.api.Assertions.*
import org.graalvm.polyglot.Context as VMContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/** Tests for the embedded [JsRuntime]. */
@TestCase class JsRuntimeTest {
  companion object {
    const val testJsonCode = "(function(x,y) { return JSON.stringify({x:x,y:y}); })"
    const val workloadFactor = 10_000
  }

  private val ctx = testContext().build()
  private val json: Value

  init {
    // evaluate test json and warm up the VM to equalize tests as rough benchmarks
    print("Please wait while the VM is warming up...")
    ctx.enter()
    json = ctx.eval("js", testJsonCode)
    (0..workloadFactor * 2).forEach { json.execute(it, it) }
    ctx.leave()
  }

  private fun testContext(): VMContext.Builder {
    return VMContext.newBuilder("js")
      .allowExperimentalOptions(true)
      .allowValueSharing(true)
  }

  private class DisruptorGuestInvocationEvent {
    private var arg1: Int = 0
    private var arg2: Int = 0
    private var callback: ((String) -> Unit) = {}

    internal fun set(arg1: Int, arg2: Int, cbk: (String) -> Unit) {
      this.arg1 = arg1
      this.arg2 = arg2
      this.callback = cbk
    }

    // First argument.
    fun arg1(): Int = arg1

    // Second argument.
    fun arg2(): Int = arg2

    // Perform callback.
    fun respond(answer: String) = callback(answer)

    override fun toString(): String {
      return "InvocationTestEvent{$arg1, $arg2}"
    }
  }

  private class DisruptorTestEventFactory : EventFactory<DisruptorGuestInvocationEvent> {
    /** @inheritDoc */
    override fun newInstance(): DisruptorGuestInvocationEvent = DisruptorGuestInvocationEvent()
  }

  private class DisruptorTestHandler constructor (
    val context: VMContext,
    val target: Value,
    val expectedCount: Long,
    val completion: CountDownLatch,
  ) : EventHandler<DisruptorGuestInvocationEvent> {
    // Whether the worker is executing.
    private var locked: Boolean = false

    override fun onEvent(event: DisruptorGuestInvocationEvent, sequence: Long, endOfBatch: Boolean) {
      if (!locked) {
        context.enter()
      }
      val result = target.execute(42, 43).asString()
      if (endOfBatch) {
        context.leave()
        locked = false
      }
      if (sequence == expectedCount) {
        completion.countDown()
      }
      event.respond(result)
    }
  }

  @Test fun testJsRuntimeAcquire() {
    assertNotNull(JsRuntime.acquire(), "should be able to acquire JS runtime instance")
  }

  @Test fun testConcurrentEval() {
    val startGate = CountDownLatch(1)
    val endGate = CountDownLatch(1)
    val hadException = AtomicBoolean(false)
    val contextLock = ReentrantLock()

    // prep a background thread
    val t = Thread {
      try {
        startGate.await()
        try {
          for (i in 0..workloadFactor) {
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
      for (i in 0..workloadFactor) {
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

  @Test fun testSuspendingEval() {
    val hadException = AtomicBoolean(false)
    val executor = Executors.newFixedThreadPool(2)
    val pool = executor.asCoroutineDispatcher()

    try {
      runBlocking {
        val test = launch(pool, start = CoroutineStart.LAZY) {
          val contextLock = Mutex()

          val job1 = launch(pool, start = CoroutineStart.LAZY) {
            for (i in 0..workloadFactor) {
              contextLock.withLock {
                try {
                  ctx.enter()
                  assertEquals("{\"x\":42,\"y\":43}", json.execute(42, 43).asString())
                } catch (ise: IllegalStateException) {
                  hadException.set(true)
                } finally {
                  ctx.leave()
                }
              }
            }
          }

          job1.start()
          for (i in 0..workloadFactor) {
            contextLock.withLock {
              try {
                ctx.enter()
                assertEquals("{\"x\":42,\"y\":43}", json.execute(42, 43).asString())
              } catch (ise: IllegalStateException) {
                hadException.set(true)
              } finally {
                ctx.leave()
              }
            }
          }

          job1.join()
        }
        test.join()
      }
    } finally {
      ctx.close(true)
      pool.close()
      executor.shutdown()
    }
    assertFalse(hadException.get(), "should not get concurrent access exception")
  }

  @Test fun testDisruptorEval() {
    val eventFactory = DisruptorTestEventFactory()
    val disruptor: Disruptor<DisruptorGuestInvocationEvent> = Disruptor(
      eventFactory,
      1024,
      DaemonThreadFactory.INSTANCE,
    )
    val ctx = testContext().build()
    ctx.enter()
    val value = ctx.eval("js", testJsonCode)
    ctx.leave()

    val completionLatch = CountDownLatch(1)
    val handler = DisruptorTestHandler(
      ctx,
      value,
      workloadFactor.toLong(),
      completionLatch,
    )
    disruptor.handleEventsWith(
      handler,
    )
    var called = false
    val validator = { subj: String ->
      if (!called) called = true
      assertEquals("{\"x\":42,\"y\":43}", subj)
    }

    try {
      disruptor.start()
      val buf = disruptor.ringBuffer
      for (i in 0..workloadFactor) {
        buf.publishEvent { event, _ ->
          event.set(42, 43, validator)
        }
      }
      completionLatch.await()

    } finally {
      disruptor.shutdown()
    }
    assertTrue(called, "should receive callback from disruptor")
  }
}
