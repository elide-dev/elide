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
package elide.tooling.web.css

import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import elide.tooling.project.ElideProjectInfo
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.web.css.CssBuilder.buildCss
import elide.tooling.web.css.CssBuilder.configureCss
import elide.tooling.web.css.CssBuilder.CssOptions
import elide.tooling.web.css.CssBuilder.CssSourceLiteral

class CssBuilderTest {
  @Test fun testDefaultCssOptions() {
    assertNotNull(CssOptions.defaults())
  }

  @Test fun testDefaultCssOptionsForProject() {
    val proj = ElideProjectInfo(
      root = Path.of(System.getProperty("user.dir")),
      manifest = ElidePackageManifest(),
      env = null,
      workspace = null,
    )
    assertNotNull(CssOptions.forProject(proj)).let { options ->
      assertNotNull(options)
      assertNotNull(options.projectRoot)
      assertEquals(options.projectRoot, proj.root)
    }
  }

  @Test fun testBuildSimpleCss() = runTest {
    configureCss(CssOptions.defaults(), sequence {
      yield(CssSourceLiteral {
        // language=css
        """
          body {
            background-color: #ffffff;
            color: #000000;
          }
        """
      })
    }).let { css ->
      assertNotNull(css)
      buildCss(css)
    }.let { result ->
      assertNotNull(result)
      val out = result.code().joinToString("")
      assertTrue(out.startsWith("body{"))
      assertTrue(out.contains("#000;"))
    }
  }
}
