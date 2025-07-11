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
package elide.tooling.md

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import elide.tooling.md.Markdown.MarkdownSourceLiteral

class MarkdownRenderTest {
  @Test fun testRenderMarkdown() = runTest {
    Markdown.renderMarkdown {
      MarkdownSourceLiteral {
        """
        ### Hello Markdown!

        Here is some markdown to render.
        """
      }
    }.let {
      assertNotNull(it)
      assertTrue(it.isNotEmpty())
      assertTrue(it.isNotBlank())
      assertTrue(it.contains("Hello Markdown!"))
      assertTrue(it.contains("Here is some markdown to render."))
    }
  }
}
