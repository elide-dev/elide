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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.NodeStdlib
import elide.runtime.gvm.internals.node.process.NodeProcess
import elide.runtime.gvm.internals.node.process.NodeProcessModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.ProcessAPI
import elide.runtime.intrinsics.js.node.process.ProcessArch
import elide.runtime.intrinsics.js.node.process.ProcessPlatform
import elide.testing.annotations.TestCase

@TestCase internal class NodeProcessTest : NodeModuleConformanceTest<NodeProcessModule>() {
  @Inject internal lateinit var process: ProcessAPI
  @Inject internal lateinit var module: NodeProcessModule

  override fun provide(): NodeProcessModule = module
  override val moduleName: String get() = "process"

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("env")
    yield("pid")
    yield("cwd")
    yield("argv")
    yield("arch")
    yield("platform")
  }

  @Test override fun testInjectable() {
    assertNotNull(process, "should be able to inject host-side process module")
    assertNotNull(NodeStdlib.process, "should be able to obtain `process` via `NodeStdlib`")
  }

  // ---- Host

  @Test fun `host env property should not be null`() {
    assertNotNull(process.env, "should have an environment accessor")
  }

  @Test fun `host cwd property should not be null`() {
    assertNotNull(process.cwd, "should have a cwd value")
  }

  @Test fun `host pid property should not be null`() {
    assertNotNull(process.pid, "should have a cwd value")
  }

  @Test fun `host argv property should not be null`() {
    assertNotNull(process.argv, "should have a argv value")
  }

  @Test fun `host platform property should not be null`() {
    assertNotNull(process.platform, "should have a platform value")
  }

  @Test fun `host arch property should not be null`() {
    assertNotNull(process.arch, "should have a arch value")
  }

  @Test fun `host argv should be empty by default`() {
    assertTrue(process.argv.isEmpty(), "argv should be empty")
  }

  @Test fun `host env should have full host environment`() {
    assertTrue(process.env.isNotEmpty(), "env should not be empty")
    val env = process.env
    System.getenv().entries.forEach {
      assertTrue(env.contains(it.key), "env should contain key '${it.key}'")
      assertEquals(it.value, env[it.key], "env['${it.key}'] should match host value (got: '${env[it.key]}')")
    }
  }

  @Test fun `pid should match host pid by default`() {
    assertEquals(ProcessHandle.current().pid(), process.pid)
  }

  @Test fun `cwd should match host cwd by default`() {
    assertEquals(System.getProperty("user.dir"), process.cwd)
  }

  @Test fun `platform should match host platform by default`() {
    assertEquals(ProcessPlatform.host(), ProcessPlatform.resolve(process.platform))
  }

  @Test fun `arch should match host arch by default`() {
    assertEquals(ProcessArch.host(), ProcessArch.resolve(process.arch))
  }

  // ---- Stubbed

  private val stubbed = NodeProcess.obtain(allow = false)

  @Test fun `stubbed env property should not be null`() {
    assertNotNull(stubbed.env, "should have an environment accessor")
  }

  @Test fun `stubbed cwd property should not be null`() {
    assertNotNull(stubbed.cwd, "should have a cwd value")
  }

  @Test fun `stubbed pid property should not be null`() {
    assertNotNull(stubbed.pid, "should have a cwd value")
  }

  @Test fun `stubbed argv property should not be null`() {
    assertNotNull(stubbed.argv, "should have a argv value")
  }

  @Test fun `stubbed platform property should not be null`() {
    assertNotNull(stubbed.platform, "should have a platform value")
  }

  @Test fun `stubbed arch property should not be null`() {
    assertNotNull(stubbed.arch, "should have a arch value")
  }

  @Test fun `stubbed argv should be empty by default`() {
    assertTrue(stubbed.argv.isEmpty(), "argv should be empty")
  }

  @Test fun `stubbed env should be empty by default`() {
    assertTrue(stubbed.env.isEmpty())
  }

  @Test fun `stubbed cwd property should be empty`() {
    assertEquals("", stubbed.cwd)
  }

  @Test fun `stubbed pid property should be -1`() {
    assertEquals(-1, stubbed.pid)
  }

  @Test fun `stubbed platform should be host platform`() {
    assertEquals(ProcessPlatform.host(), ProcessPlatform.resolve(stubbed.platform))
  }

  @Test fun `stubbed arch should be host arch`() {
    assertEquals(ProcessArch.host(), ProcessArch.resolve(stubbed.arch))
  }

  @Test fun `stubbed exit should not exit the vm`() {
    stubbed.exit(0)
  }
}
