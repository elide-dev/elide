/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded

import tools.elide.call.engineConfiguration
import tools.elide.http.HttpMethod.GET
import tools.elide.http.httpHeader
import tools.elide.http.httpHeaders
import tools.elide.http.httpRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.embedded.api.Capability.BASELINE
import elide.embedded.api.ProtocolMode.PROTOBUF
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests Elide's embedded entrypoint. */
@TestCase class EmbeddedEntryTest : AbstractEmbeddedTest() {
  @Test fun testEntryNoArgs() = runTest {
    ElideEmbedded.create().entry(emptyArray())
  }

  @Test fun testInitializeEmptyLifecycle(): Unit = ElideEmbedded.create().let {
    it.initialize(PROTOBUF)
    it.capability(BASELINE)
    it.configure(EMBEDDED_API_VERSION, null)  // configure with defaults
    it.teardown()
  }

  @Test fun testInstanceConfigBasic() = withConfig {
    engine = engineConfiguration {
      caching = false
    }
  }.thenAssert {
    assertFalse(config.engine.caching, "caching should be disabled because we disabled it")
  }

  @Test fun testInstanceDispatchFetch() = embedded().fetch {
    // no changes
  }.then {
    assertTrue(isRunning, "instance should be running")
    exec(it.call, it.inflight).let { response ->
      assertNotNull(response, "should not get `null` from embedded response (fetch)")
    }
  }

  @Test fun testInstanceDispatchScheduled() = embedded().scheduled {
    // no changes
  }.then {
    assertTrue(isRunning, "instance should be running")
    exec(it.call, it.inflight).let { response ->
      assertNotNull(response, "should not get `null` from embedded response (scheduled)")
    }
  }

  @Test fun testInstanceDispatchQueued() = embedded().queue {
    // no changes
  }.then {
    assertTrue(isRunning, "instance should be running")
    exec(it.call, it.inflight).let { response ->
      assertNotNull(response, "should not get `null` from embedded response (queued)")
    }
  }

  @Test fun testInstanceDispatchFetchNative() = withConfig {
    // nothing yet
  }.thenAssert {
    assertTrue(isRunning, "instance should be running")
    val (call, inflight) = createFetch(native = true) {
      request = httpRequest {
        standard = GET
        path = "/"
        headers = httpHeaders {
          header.add(httpHeader {
            name = "User-Agent"
            value = "ElideEmbeddedTest"
          })
        }
      }
    }

    exec(call, inflight).let {
      assertNotNull(it, "should not get `null` from embedded response")
    }
  }
}
