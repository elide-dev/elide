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
package elide.tooling.project.agents

import kotlin.test.*

class AdviceTest {
  private fun AgentAdvice.hasElideAdvice(): Boolean = stanzas.filterIsInstance<Section>().any {
    "Elide" in it.headingText
  }

  private fun AgentAdvice.hasCustomAdvice(): Boolean = stanzas.filterIsInstance<Section>().any {
    "Elide" !in it.headingText && "Project" !in it.headingText
  }

  private fun AgentAdvice.hasProjectAdvice(): Boolean = stanzas.filterIsInstance<Section>().any {
    "Project" in it.headingText
  }

  private fun assertAdvice(advice: AgentAdvice, nonempty: Boolean = true) {
    assertNotNull(advice)
    assertNotNull(advice.stanzas)

    if (nonempty) {
      assertTrue(advice.stanzas.isNotEmpty(), "expected advice to not be empty")
    } else {
      assertTrue(advice.stanzas.isEmpty(), "expected advice to be empty")
    }
  }

  private fun assertHasElideAdvice(advice: AgentAdvice) {
    assertTrue(advice.hasElideAdvice())
  }

  private fun assertHasCustomAdvice(advice: AgentAdvice) {
    assertTrue(advice.hasCustomAdvice())
  }

  @Test fun testBuildDefaultAdviceNoProject() {
    assertNotNull(AgentAdvice.defaults()).let { advice ->
      assertAdvice(advice)
      assertHasElideAdvice(advice)
      assertFalse(advice.hasProjectAdvice())
      assertFalse(advice.hasCustomAdvice())
    }
  }

  @Test fun testBuildCustomNoDefaults() {
    assertNotNull(AgentAdvice.build {
      section(heading("Here is as some advice"), text {
        """
          And some text
        """
      })
    }).let { advice ->
      assertAdvice(advice)
      assertFalse(advice.hasElideAdvice())
      assertFalse(advice.hasProjectAdvice())
      assertHasCustomAdvice(advice)
    }
  }

  @Test fun testBuildCustomWithDefaults() {
    assertNotNull(AgentAdvice.withDefaults {
      section(heading("Here is as some advice"), text("And some text"))
    }).let { advice ->
      assertAdvice(advice)
      assertHasElideAdvice(advice)
      assertFalse(advice.hasProjectAdvice())
      assertHasCustomAdvice(advice)
    }
  }
}
