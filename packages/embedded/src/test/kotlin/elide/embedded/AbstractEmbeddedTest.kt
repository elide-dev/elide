/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

@file:Suppress("NOTHING_TO_INLINE")

package elide.embedded

import org.junit.jupiter.api.assertDoesNotThrow
import tools.elide.call.HostConfigurationKt
import tools.elide.call.hostConfiguration
import tools.elide.call.v1alpha1.*
import tools.elide.http.HttpMethod.GET
import tools.elide.http.httpHeader
import tools.elide.http.httpHeaders
import tools.elide.http.httpRequest
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.embedded.api.Capability
import elide.embedded.api.Capability.BASELINE
import elide.embedded.api.InFlightCallInfo
import elide.embedded.api.InstanceConfiguration
import elide.embedded.api.ProtocolMode.PROTOBUF
import elide.embedded.api.UnaryNativeCall
import elide.util.UUID

/** Shared test utilities for embedded runtime testing. */
abstract class AbstractEmbeddedTest : CallBuilder {
  interface EmbeddedTestContext

  class PreconfiguredRequestContext(
    private val requestContext: AtomicReference<Pair<UnaryNativeCall, InFlightCallInfo>?>,
  ) {
    val call: UnaryNativeCall get() = requestContext.get()?.first ?: error("Test request context not initialized")
    val inflight: InFlightCallInfo get() = requestContext.get()?.second ?: error("Test request context not initialized")
  }

  class ConfiguredEmbeddedTestContext (
    val instance: ElideEmbedded,
    val config: InstanceConfiguration,
    val capabilities: EnumSet<Capability> = EnumSet.of(BASELINE),
    var shouldStart: Boolean = true,
    var args: MutableList<String> = ArrayList(),
    val requestContext: AtomicReference<Pair<UnaryNativeCall, InFlightCallInfo>?> = AtomicReference(null),
    val preconfiguredRequestContext: PreconfiguredRequestContext = PreconfiguredRequestContext(requestContext),
  ) : EmbeddedTestContext, CallBuilder {
    val lockedForTest: AtomicBoolean = AtomicBoolean(false)
    private val started: AtomicBoolean = AtomicBoolean(false)
    val isRunning: Boolean get() = started.get()

    fun doStart() {
      if (started.compareAndSet(false, true)) {
        instance.start(args)
      }
    }

    suspend inline fun <R> withLock(crossinline op: suspend () -> R): R {
      lockedForTest.compareAndSet(false, true)
      return op().also {
        lockedForTest.set(false)
      }
    }

    fun capability(capability: Capability) = apply {
      capabilities.add(capability)
    }

    inline fun customize(crossinline op: ConfiguredEmbeddedTestContext.() -> Unit): ConfiguredEmbeddedTestContext {
      return apply {
        op()
      }
    }

    inline fun fetch(crossinline op: FetchRequestKt.Dsl.() -> Unit): ConfiguredEmbeddedTestContext = apply {
      val fetch = createFetch {
        request = httpRequest {
          standard = GET
          path = "/"
          headers = httpHeaders {
            header.add(
              httpHeader {
              name = "User-Agent"
              value = "ElideEmbeddedTest"
            }
            )
          }
        }

        op()
      }
      requestContext.set(fetch)
    }

    inline fun scheduled(
      crossinline op: ScheduledInvocationRequestKt.Dsl.() -> Unit
    ): ConfiguredEmbeddedTestContext = apply {
      val alarm = createScheduled {
        op()
      }
      requestContext.set(alarm)
    }

    inline fun queue(crossinline op: QueueInvocationRequestKt.Dsl.() -> Unit): ConfiguredEmbeddedTestContext = apply {
      val queue = createQueued {
        batch = queueMessageBatch {
          messages.add(queueMessage {
            id = UUID.random()
          })
          messages.add(queueMessage {
            id = UUID.random()
          })
        }
        op()
      }
      requestContext.set(queue)
    }

    suspend inline fun prepareAndRun(
      crossinline op: suspend ConfiguredEmbeddedTestContext.(req: PreconfiguredRequestContext) -> Unit
    ) {
      withLock {
        assertEquals(
          0,
          assertDoesNotThrow("initialization routine should not throw") { instance.initialize(PROTOBUF) },
          "expected successful initialization",
        )
        capabilities.forEach {
          assertEquals(
            0,
            assertDoesNotThrow("capability should not throw (value: '$it')") {
              instance.capability(it)
            },
            "expected capability to be added successfully: '$it'",
          )
        }
        assertEquals(
          0,
          assertDoesNotThrow("configuration routine should not throw") {
            instance.configure(EMBEDDED_API_VERSION, config)
          },
          "expected successful configuration",
        )
        val err: AtomicReference<Throwable> = AtomicReference(null)

        if (shouldStart) try {
          doStart()
        } catch (exc: Throwable) {
          err.set(exc)
        }
        if (err.get() == null) try {
          try {
            op(preconfiguredRequestContext)
          } catch (exc: Throwable) {
            err.set(exc)
          }
        } finally {
          instance.teardown()
        }
        when (val exc = err.get()) {
          null -> { /* nothing to do */ }
          else -> throw exc  // rethrow inner
        }
      }
    }

    inline fun then(
      crossinline op: suspend ConfiguredEmbeddedTestContext.(req: PreconfiguredRequestContext) -> Unit
    ): Unit = runTest {
      prepareAndRun {
        op.invoke(this, it)
      }
    }

    inline fun thenAssert(crossinline op: suspend ConfiguredEmbeddedTestContext.() -> Unit) = runTest {
      prepareAndRun {
        op.invoke(this)
      }
    }

    suspend fun exec(
      call: UnaryNativeCall,
      inflight: InFlightCallInfo,
    ): UnaryInvocationResponse {
      // the call factory should not need to decode already-decoded calls
      assertTrue(call.ready, "call should be pre-loaded")

      // start the call
      assertEquals(
        0,
        assertDoesNotThrow { instance.enterDispatch(call, inflight) },
        "return code from `enterDispatch` should be `0` for successful call",
      )

      // poll the call once for fun
      assertEquals(
        0,
        assertDoesNotThrow { instance.dispatchPoll(inflight) },
        "should be able to poll a known-good active call",
      )

      // gather the response
      val out = assertDoesNotThrow { instance.response(call, inflight) }
      assertNotNull(out, "should not get `null` from embedded response")

      // finish the call
      assertEquals(
        0,
        assertDoesNotThrow { instance.exitDispatch(call, inflight) },
        "should be able to exit dispatch to finish an embedded native call",
      )
      return out
    }
  }

  companion object {
    const val EMBEDDED_API_VERSION: String = "v1alpha1"

    inline fun embedded(): ConfiguredEmbeddedTestContext {
      return ConfiguredEmbeddedTestContext(
        ElideEmbedded.create(),
        InstanceConfiguration.createFrom(hostConfiguration {
          // no defaults at this time
        })
      )
    }

    inline fun withConfig(builder: HostConfigurationKt.Dsl.() -> Unit): ConfiguredEmbeddedTestContext {
      return ConfiguredEmbeddedTestContext(
        ElideEmbedded.create(),
        InstanceConfiguration.createFrom(hostConfiguration {
          apply(builder)
        })
      )
    }
  }
}
