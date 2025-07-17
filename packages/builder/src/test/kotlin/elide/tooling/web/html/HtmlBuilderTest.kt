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
package elide.tooling.web.html

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HtmlBuilderTest {
  @Test fun testHtmlOptionDefaults() {
    assertNotNull(HtmlBuilder.HtmlOptions.defaults())
    assertTrue(HtmlBuilder.HtmlOptions.defaults().minify.comments)
  }

  @Test fun testConfigureHtmlBuilder() {
    assertNotNull(HtmlBuilder.HtmlOptions.defaults())
    HtmlBuilder.configureHtml(HtmlBuilder.HtmlOptions.defaults(), sequence {
      yield(HtmlBuilder.HtmlSourceLiteral {
        // language=html
        """
          <!doctype html>
          <html lang=en>
            <head>
              <title>test html</title>
            </head>
            <body>
              <h1>Test HTML</h1>
              <p>This is a test HTML document.</p>
            </body>
          </html>
        """
      })
    })
  }

  @Test fun testMinifyHtml() = runTest {
    assertNotNull(HtmlBuilder.HtmlOptions.defaults())
    var originalCode: String? = null
    HtmlBuilder.configureHtml(HtmlBuilder.HtmlOptions.defaults(), sequence {
      yield(HtmlBuilder.HtmlSourceLiteral {
        // language=html
        """
          <!doctype html>
          <html lang='en'>
            <head>
              <title>test html</title>
            </head>
            <body>
              <h1>Test HTML</h1>
              <p>This is a test HTML document.</p>
            </body>
          </html>
        """.also {
          originalCode = it
        }
      })
    }).let { htmlBuild ->
      HtmlBuilder.buildHtml(htmlBuild)
    }.let { result ->
      assertNotNull(result)
      val code = result.code().single()
      assertNotNull(code)
      assertTrue(code.isNotEmpty())
      assertTrue(code.contains("<html lang=en>"))
      assertTrue(code.contains("<head>"))
      assertTrue(code.contains("<title>test html</title>"))
      assertNotEquals(originalCode, code)
    }
  }
}
