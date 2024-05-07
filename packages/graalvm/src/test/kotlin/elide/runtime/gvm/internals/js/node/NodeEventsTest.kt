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
package elide.runtime.gvm.internals.js.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.events.NodeEventsModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.EventsAPI
import elide.testing.annotations.TestCase

/** Tests for the built-in `assert` module. */
@TestCase internal class NodeEventsTest @Inject constructor(internal val events: EventsAPI)
  : NodeModuleConformanceTest<NodeEventsModule>() {
  override val moduleName: String get() = "events"
  override fun provide(): NodeEventsModule = NodeEventsModule()

  // @TODO(sgammon): Does not yet comply with all methods.
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("EventEmitter")
    yield("Event")
    yield("EventListener")
    yield("EventTarget")
  }

  @Test override fun testInjectable() {
    assertNotNull(events)
  }
}
