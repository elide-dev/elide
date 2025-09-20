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
package elide.tooling.web.mdx

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.text.contains
import kotlin.text.isNotBlank
import kotlin.text.isNotEmpty
import elide.tooling.md.Markdown
import elide.tooling.md.Markdown.MarkdownOptions
import elide.tooling.md.Markdown.MarkdownSourceLiteral
import elide.tooling.md.MarkdownFlavor

class MdxBuilderTest {
  @Test fun testDefaultMarkdownOptions() {
    assertNotNull(MarkdownOptions.defaults())
  }

  @Ignore
  @Test fun testRenderMdx() = runTest {
    Markdown.renderMarkdown(style = MarkdownFlavor.Mdx) {
      MarkdownSourceLiteral {
        """
        import {Chart} from './snowfall.js'
        export const year = 2023
        
        # Last year's snowfall
        
        In {year}, the snowfall was above average.
        It was followed by a warm spring which caused
        flood conditions in many of the nearby rivers.
        
        <Chart color="#fcb32c" year={year} />
        """
      }
    }.let {
      assertNotNull(it)
      val out = it.asString()
      assertTrue(out.isNotEmpty())
      assertTrue(out.isNotBlank())
      assertTrue(out.contains("Last year's snowfall"))
      assertNull(it.metadata())
    }
  }
}
