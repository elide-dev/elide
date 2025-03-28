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
package elide.runtime.intrinsics.ai

import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.runtime.node.ElideJsModuleTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

// Tests for Elide's built-in AI modules.
@TestCase internal class ElideAIModuleTest : ElideJsModuleTest<ElideLLMModule>() {
  @Inject lateinit var module: ElideLLMModule
  override fun provide(): ElideLLMModule = module
  override val pureModuleName: String get() = "llm"

  @Test override fun testInjectable() {
    assertNotNull(module) { "Main LLM module should be injectable" }
  }

  @Test fun testLlmApiVersion() = dual {
    val expected = "v1"
    val version = provide().module().version()
    assertEquals(expected, version)
  }.guest {
    // language=JavaScript
    """
      const { version } = require("elide:llm");
      test(version()).isNotNull();
      test(version()).isEqualTo('v1');
    """
  }
}
