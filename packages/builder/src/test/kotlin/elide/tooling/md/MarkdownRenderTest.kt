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
  @Test fun testParseFrontmatter() = runTest {
    Markdown.renderMarkdown {
      MarkdownSourceLiteral {
        // language=markdown
        """
        ---
        title: Some Title
        another: Field
        ---
        ### Hello Markdown!

        Here is some markdown to render.
        """
      }
    }.let {
      assertNotNull(it)
      val out = it.asString()
      val metadata = it.metadata()
      assertTrue(out.isNotEmpty())
      assertTrue(out.isNotBlank())
      assertTrue(out.contains("Hello Markdown!"))
      assertTrue(out.contains("Here is some markdown to render."))
      assertFalse(out.contains("---"))
      assertFalse(out.contains("title: Some Title"))
      assertNotNull(metadata)
      assertEquals(2, metadata.size)
      assertTrue("another" in metadata)
    }
  }

  @Test fun testParseFrontmatterFullYaml() = runTest {
    Markdown.renderMarkdown {
      MarkdownSourceLiteral {
        // language=markdown
        """
        ---
        title: Some Title
        another: Field
        yet-another:
        - this one is a list
        ---
        ### Hello Markdown!

        Here is some markdown to render.
        """
      }
    }.let {
      assertNotNull(it)
      val out = it.asString()
      val metadata = it.metadata()
      assertTrue(out.isNotEmpty())
      assertTrue(out.isNotBlank())
      assertTrue(out.contains("Hello Markdown!"))
      assertTrue(out.contains("Here is some markdown to render."))
      assertFalse(out.contains("---"))
      assertFalse(out.contains("title: Some Title"))
      assertNotNull(metadata)
      assertEquals(3, metadata.size)
      assertTrue("another" in metadata)
      assertTrue("yet-another" in metadata)
      assertIs<List<*>>(metadata["yet-another"])
    }
  }

  @Test fun testRenderMarkdown() = runTest {
    Markdown.renderMarkdown {
      MarkdownSourceLiteral {
        // language=markdown
        """
        ### Hello Markdown!

        Here is some markdown to render.
        """
      }
    }.let {
      assertNotNull(it)
      val out = it.asString()
      assertTrue(out.isNotEmpty())
      assertTrue(out.isNotBlank())
      assertTrue(out.contains("Hello Markdown!"))
      assertTrue(out.contains("Here is some markdown to render."))
      assertNull(it.metadata())
    }
  }

  @Test fun testRenderMarkdownGfm() = runTest {
    Markdown.renderMarkdown(style = MarkdownFlavor.GitHub) {
      MarkdownSourceLiteral {
        // language=markdown
        """
        ### Hello Markdown!

        Here is some markdown to render. Some GFM specific markdown:
        ```js
        console.log("Hello, world!");
        ```
        """
      }
    }.let {
      assertNotNull(it)
      val out = it.asString()
      assertTrue(out.isNotEmpty())
      assertTrue(out.isNotBlank())
      assertTrue(out.contains("Hello Markdown!"))
      assertTrue(out.contains("Here is some markdown to render."))
      assertTrue(out.contains("Some GFM"))
      assertTrue(out.contains("<code class=\"language-js\">"))
      assertNull(it.metadata())
    }
  }

  @Test fun testRenderMarkdownCommon() = runTest {
    Markdown.renderMarkdown(style = MarkdownFlavor.CommonMark) {
      MarkdownSourceLiteral {
        // language=markdown
        """
        ### Hello Markdown!

        Here is some markdown to render. This is specifically common markdown.
        """
      }
    }.let {
      assertNotNull(it)
      val out = it.asString()
      assertTrue(out.isNotEmpty())
      assertTrue(out.isNotBlank())
      assertTrue(out.contains("Hello Markdown!"))
      assertTrue(out.contains("Here is some markdown to render."))
      assertNull(it.metadata())
    }
  }
}
