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

import jakarta.inject.Inject
import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.runtime.gvm.internals.node.os.NodeOperatingSystemModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.OperatingSystemAPI
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `os` built-in module. */
@TestCase internal class NodeOsTest : NodeModuleConformanceTest<NodeOperatingSystemModule>() {
  override val moduleName: String get() = "os"
  override fun provide(): NodeOperatingSystemModule = NodeOperatingSystemModule()
  @Inject internal lateinit var os: OperatingSystemAPI

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("EOL")
    yield("constants")
    yield("devNull")
    yield("availableParallelism")
    yield("arch")
    yield("cpus")
    yield("endianness")
    yield("freemem")
    yield("homedir")
    yield("hostname")
    yield("loadavg")
    yield("networkInterfaces")
    yield("platform")
    yield("release")
    yield("tmpdir")
    yield("totalmem")
    yield("type")
    yield("uptime")
    yield("userInfo")
  }

  @Test override fun testInjectable() {
    assertNotNull(os, "should be able to inject instance of OS module")
  }
}
