/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.tool.ssg.SiteCompilerParams.OutputFormat

/** Tests which invoke the SSG compiler over the Java API. */
@MicronautTest(
  application = hellocss.App::class,
  startApplication = true,
  packages = [
    "helloworld",
    "helloworld.*",
    "elide.tools.ssg",
    "elide.tools.ssg.*",
  ],
)
class JavaAPISSGCompileTest : AbstractSSGCompilerTest() {
  @Test fun testProgrammaticVersion() {
    val version = SiteCompiler.version()
    assertNotNull(version, "should not get `null` for tool version")
    assertTrue(version.isNotEmpty(), "tool version should not be empty")
  }

  @Test fun testProgrammaticHttpCompile(): Unit = withCompiler(outputArchive(OutputFormat.ZIP)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }

  @Test fun testProgrammaticHttpCompileCrawl(): Unit = withCompiler(outputArchive(OutputFormat.ZIP)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = true,
        ),
      )
    }
  }

  @Test fun testProgrammaticEmptyCompile(): Unit = withCompiler(outputArchive(OutputFormat.ZIP)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = emptyManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }

  @Test fun testProgrammaticEmptyTarCompile(): Unit = withCompiler(outputArchive(OutputFormat.TAR)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = emptyManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }

  @Test fun testProgrammaticHttpCompileToTar(): Unit = withCompiler(outputArchive(OutputFormat.TAR)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }

  @Test fun testProgrammaticHttpCompileToDir(): Unit = withCompiler(outputDirectory()) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }

  @Test fun testProgrammaticEmptyCompileToDir(): Unit = withCompiler(outputDirectory()) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = emptyManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }
}
