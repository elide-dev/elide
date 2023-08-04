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

@file:OptIn(
  DelicateCoroutinesApi::class,
  ExperimentalCoroutinesApi::class
)

package benchmarks.gvm

import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.WorkHandler
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.util.DaemonThreadFactory
import org.openjdk.jmh.annotations.*
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

/** Tests for JS VM dispatch performance. */
@Suppress("DuplicatedCode")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
class GuestVMDispatchBenchmark {
  companion object {
    const val workloadFactor = 1

    private const val testJsonCode = "(function(x,y) { return JSON.stringify({x:x,y:y}); })"
    private val mainThreadSurrogate = newSingleThreadContext("gvm-bench")
    private val ctx = testContext().build()
    private val ctx2 = testContext().build()
    private val ctx3 = testContext().build()
    private val ctx4 = testContext().build()
    private val json: Value = ctx.let {
      it.enter()
      val value = it.eval("js", testJsonCode)
      it.leave()
      value
    }
    private val json2: Value = ctx2.let {
      it.enter()
      val value = it.eval("js", testJsonCode)
      it.leave()
      value
    }
    private val json3: Value = ctx2.let {
      it.enter()
      val value = it.eval("js", testJsonCode)
      it.leave()
      value
    }
    private val json4: Value = ctx2.let {
      it.enter()
      val value = it.eval("js", testJsonCode)
      it.leave()
      value
    }
    private val executor = Executors.newFixedThreadPool(
      4,
      DaemonThreadFactory.INSTANCE,
    )
    private val pool = executor.asCoroutineDispatcher()
    private val eventFactory = DisruptorTestEventFactory()
    private val disruptor: Disruptor<DisruptorGuestInvocationEvent> = Disruptor(
      eventFactory,
      1024,
      executor,
    )
    private val contextLock = Mutex()
    private val contextLock2 = Mutex()

    init {
      // evaluate test json and warm up the VM to equalize tests as rough benchmarks
      print("Please wait while the VM is warming up...")
      ctx.enter()
      (0..1000).forEach {
        json.execute(it, it)
      }
      ctx.leave()

      ctx2.enter()
      (0..1000).forEach {
        json2.execute(it, it)
      }
      ctx2.leave()

      ctx3.enter()
      (0..1000).forEach {
        json3.execute(it, it)
      }
      ctx3.leave()

      ctx4.enter()
      (0..1000).forEach {
        json4.execute(it, it)
      }
      ctx4.leave()
    }

    private val handler1 = DisruptorTestHandler(
      ctx,
      json,
    )

    private val handler2 = DisruptorTestHandler(
      ctx2,
      json2,
    )

    private val handler3 = DisruptorTestHandler(
      ctx3,
      json3,
    )

    private val handler4 = DisruptorTestHandler(
      ctx4,
      json4,
    )

    private fun testContext(): VMContext.Builder {
      return VMContext.newBuilder("js")
        .allowExperimentalOptions(true)
        .allowValueSharing(true)
    }
  }

  private class DisruptorGuestInvocationEvent {
    private var arg1: Int = 0
    private var arg2: Int = 0
    private var callback: ((String) -> Unit) = {}

    fun set(arg1: Int, arg2: Int, cbk: (String) -> Unit) {
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
    override fun newInstance(): DisruptorGuestInvocationEvent = DisruptorGuestInvocationEvent()
  }

  private class DisruptorTestHandler constructor (
    val context: VMContext,
    val target: Value,
  ) : EventHandler<DisruptorGuestInvocationEvent>, WorkHandler<DisruptorGuestInvocationEvent> {
    // Whether the worker is executing.
    private var locked: Boolean = false

    // Processed event count.
    private var processed: Long = 0

    override fun onEvent(event: DisruptorGuestInvocationEvent) {
      processed += 1
      context.enter()
      val result = target.execute(42, 43).asString()
      context.leave()
      event.respond(result)
    }

    override fun onEvent(event: DisruptorGuestInvocationEvent, sequence: Long, endOfBatch: Boolean) {
      if (!locked) {
        locked = true
        context.enter()
      }
      processed += 1
      val result = target.execute(42, 43).asString()
      if (endOfBatch) {
        locked = false
        context.leave()
      }
      event.respond(result)
    }
  }

  @Setup fun setUp() {
    disruptor.handleEventsWith(
      handler1,
    )
    disruptor.start()
    Dispatchers.setMain(mainThreadSurrogate)
  }

  @TearDown fun tearDown() {
    try {
      ctx.close(true)
      disruptor.shutdown()
      pool.close()
      executor.shutdown()
      Dispatchers.resetMain()
      mainThreadSurrogate.close()
    } catch (err: Throwable) {
      // ignore
    }
  }

  fun benchmarkCoroutinesTwoBatches() {
    val hadException = AtomicBoolean(false)

    runBlocking {
      val job1 = launch(pool, start = CoroutineStart.LAZY) {
        contextLock.withLock {
          try {
            ctx.enter()
            for (i in 0..workloadFactor) {
              assert("{\"x\":42,\"y\":43}" == json.execute(42, 43).asString())
            }
          } catch (ise: IllegalStateException) {
            hadException.set(true)
          } finally {
            ctx.leave()
          }
        }
      }

      job1.start()
      job1.join()
    }
    assert(!hadException.get()) {
      "should not get concurrent access exception"
    }
  }

  @Benchmark fun benchmarkDisruptor() {
    try {
      val waiter = CountDownLatch(1)
      val calls = AtomicInteger(0)
      val validator = { subj: String ->
        assert("{\"x\":42,\"y\":43}" == subj)
        if (calls.incrementAndGet() == workloadFactor) {
          waiter.countDown()
        }
      }

      try {
        val buf = disruptor.ringBuffer
        for (i in 0..workloadFactor) {
          buf.publishEvent { event, _ ->
            event.set(42, 43, validator)
          }
        }
        waiter.await()
      } catch (ixe: InterruptedException) {
        Thread.interrupted()
      }
    } catch (err: Throwable) {
      val writer = StringWriter()
      val printer = PrintWriter(writer)
      err.printStackTrace(printer)
      print(printer.toString())
    }
  }
}
