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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.js

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.micronaut.context.annotation.Replaces
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.*
import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.testing.annotations.TestCase

/** Tests for timer functions like `setTimeout` and `setInterval`. */
@TestCase internal class JsTimersTest : AbstractJsIntrinsicTest<JsTimersIntrinsic>() {
  @Replaces(JsTimerExecutorProviderImpl::class)
  @Factory internal class JsTimerExecutorProviderTesting : JsTimerExecutorProvider {
    @Singleton override fun provide(): ListeningScheduledExecutorService =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor())
  }

  @Inject private lateinit var subject: JsTimersIntrinsic
  override fun provide(): JsTimersIntrinsic = subject

  @Test override fun testInjectable() {
    assertNotNull(subject)
  }

  @Test fun `test setTimeout() with 0-delay`() = withContext {
    val switch = AtomicBoolean(false)
    val latch = CountDownLatch(1)
    try {
      enter()
      subject.provide().setTimeout {
        switch.compareAndSet(false, true)
        latch.countDown()
      }
    } finally {
      leave()
    }
    latch.await()
    Thread.sleep(100)  // give it time to fail because of erroneous re-scheduling
    assertTrue(switch.get())
  }

  @Test fun `test setTimeout() with 10ms delay`() = withContext {
    val stopped = AtomicLong(-1)
    val latch = CountDownLatch(1)
    val now = System.currentTimeMillis()
    try {
      enter()
      subject.provide().setTimeout(10) {
        stopped.set(System.currentTimeMillis())
        latch.countDown()
      }
    } finally {
      leave()
    }
    latch.await()
    assertNotEquals(-1, stopped.get())
    assertTrue(stopped.get() - now >= 10)
    assertTrue(stopped.get() - now < 100)
  }

  @Test fun `test setTimeout() with 100ms delay`() = withContext {
    val stopped = AtomicLong(-1)
    val latch = CountDownLatch(1)
    val now = System.currentTimeMillis()
    try {
      enter()
      subject.provide().setTimeout(100) {
        stopped.set(System.currentTimeMillis())
        latch.countDown()
      }
    } finally {
      leave()
    }
    latch.await()
    assertNotEquals(-1, stopped.get())
    assertTrue(stopped.get() - now >= 100)
    assertTrue(stopped.get() - now < 200)
  }

  @Test fun `test setTimeout() with 250ms delay`() = withContext {
    val stopped = AtomicLong(-1)
    val latch = CountDownLatch(1)
    val now = System.currentTimeMillis()
    try {
      enter()
      subject.provide().setTimeout(250) {
        stopped.set(System.currentTimeMillis())
        latch.countDown()
      }
    } finally {
      leave()
    }
    latch.await()
    assertNotEquals(-1, stopped.get())
    assertTrue(stopped.get() - now >= 250)
    assertTrue(stopped.get() - now <= 350)
  }

  @Test fun `test setTimeout() with 1s delay`() = withContext {
    val stopped = AtomicLong(-1)
    val latch = CountDownLatch(1)
    val now = System.currentTimeMillis()
    try {
      enter()
      subject.provide().setTimeout(1000) {
        stopped.set(System.currentTimeMillis())
        latch.countDown()
      }
    } finally {
      leave()
    }
    latch.await()
    assertNotEquals(-1, stopped.get())
    assertTrue(stopped.get() - now >= 1000)
    assertTrue(stopped.get() - now <= 1100)
  }

  @Test fun `test setTimeout() cancel before run`() = withContext {
    val switch = AtomicBoolean(false)
    try {
      enter()
      val token = subject.provide().setTimeout(1000) {
        switch.compareAndSet(false, true)
      }
      Thread.sleep(10)
      subject.provide().clearTimeout(token)
    } finally {
      leave()
    }
    Thread.sleep(1500)  // give it time to fail because of erroneous re-scheduling
    assertFalse(switch.get())
  }

  @Test fun `test setTimeout() cancel after run`() = withContext {
    val switch = AtomicBoolean(false)
    val latch = CountDownLatch(1)
    try {
      enter()
      val token = subject.provide().setTimeout(50) {
        switch.compareAndSet(false, true)
        latch.countDown()
      }
      latch.await()
      subject.provide().clearTimeout(token)
    } finally {
      leave()
    }
    Thread.sleep(1500)  // give it time to fail because of erroneous re-scheduling
    assertTrue(switch.get())
  }

  @Test fun `test setInterval() with 10ms delay`() = withContext {
    val counter = AtomicLong(0)
    val latch = CountDownLatch(5)
    val token = try {
      enter()
      subject.provide().setInterval(100) {
        counter.incrementAndGet()
        latch.countDown()
      }
    } finally {
      leave()
    }
    latch.await()
    subject.provide().clearInterval(token)
    val runs = counter.get()
    Thread.sleep(1000)
    assertEquals(counter.get(), runs)
    assertTrue(counter.get() >= 5)
  }

  @Test fun `test setInterval() cancel before run`() = withContext {
    val switch = AtomicBoolean(false)
    try {
      enter()
      val token = subject.provide().setInterval(1000) {
        switch.compareAndSet(false, true)
      }
      Thread.sleep(10)
      subject.provide().clearInterval(token)
    } finally {
      leave()
    }
    Thread.sleep(1500)  // give it time to fail because of erroneous re-scheduling
    assertFalse(switch.get())
  }

  @Test fun `test setInterval() cancel after run`() = withContext {
    val switch = AtomicBoolean(false)
    val latch = CountDownLatch(1)
    try {
      enter()
      val token = subject.provide().setInterval(150) {
        switch.compareAndSet(false, true)
        latch.countDown()
      }
      latch.await()
      subject.provide().clearInterval(token)
    } finally {
      leave()
    }
    Thread.sleep(1500)  // give it time to fail because of erroneous re-scheduling
    assertTrue(switch.get())
  }
}
