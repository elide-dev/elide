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

package elide.embedded

import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import elide.embedded.EmbeddedAppConfiguration.EmbeddedDispatchMode.FETCH
import elide.embedded.EmbeddedGuestLanguage.JAVA_SCRIPT
import elide.embedded.internal.EmbeddedAppImpl

class EmbeddedAppTest {
  context(TestScope) private fun prepareApp(): EmbeddedApp {
    return EmbeddedAppImpl.launch(
      id = EmbeddedAppId("test-app"),
      config = EmbeddedAppConfiguration(
        entrypoint = "index.js",
        language = JAVA_SCRIPT,
        dispatchMode = FETCH,
      ),
      context = backgroundScope.coroutineContext,
    )
  }

  @Test fun `should handle lifecycle transitions`() = runTest {
    val app = prepareApp()
    assertEquals(EmbeddedAppState.READY, app.state.value, "expected app to begin in 'ready' state")

    app.start().join()
    assertEquals(EmbeddedAppState.RUNNING, app.state.value, "expected app to move into 'running' state")

    app.stop().join()
    assertEquals(EmbeddedAppState.STOPPED, app.state.value, "expected app to move into 'stopped' state")

    app.start().join()
    assertEquals(EmbeddedAppState.RUNNING, app.state.value, "expected app to move into 'running' state after restart")
  }
}
