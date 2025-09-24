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
package elide.runtime.plugins

import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.plugins.env.Environment
import elide.runtime.plugins.env.environment
import elide.runtime.plugins.js.JavaScript
import elide.testing.annotations.Test

class EnvPluginTest {
  @Test fun `should prevent host env access by default`() {
    assert(System.getenv().isNotEmpty()) { "A non-empty host env is required" }

    val engine = PolyglotEngine {
      configure(Environment)
      configure(JavaScript)
    }

    val context = engine.acquire()
    val guestEnv = Environment.forLanguage(JavaScript, context.unwrap())

    System.getenv().forEach {
      assertFalse(guestEnv.hasMember(it.key), "expected host env variable to be inaccessible")
    }
  }

  @Test fun `should allow host env access when configured`() {
    assert(System.getenv().isNotEmpty()) { "A non-empty host env is required" }

    val engine = PolyglotEngine {
      configure(Environment) {
        System.getenv().forEach { mapToHostEnv(it.key) }
      }

      configure(JavaScript)
    }

    val context = engine.acquire()
    val guestEnv = Environment.forLanguage(JavaScript, context.unwrap())

    System.getenv().forEach {
      assertTrue(guestEnv.hasMember(it.key), "expected host env variable to be accessible")
      assertEquals(it.value, guestEnv.getMember(it.key).asString(), "expected env variable value to match host")
    }
  }
}
