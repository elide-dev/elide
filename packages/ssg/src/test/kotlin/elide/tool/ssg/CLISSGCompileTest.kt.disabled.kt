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
import elide.tool.ssg.SiteCompilerParams.OutputFormat

/** Tests which invoke the SSG compiler over the CLI. */
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
class CLISSGCompileTest : AbstractSSGCompilerTest() {
  @Test fun testCommandHelp() {
    assertSuccess {
      cli("--help")
    }
  }

  @Test fun testCommandVersion() {
    assertSuccess {
      cli("--version")
    }
  }

  @Test fun testCommandLineHttpCompile(): Unit = withCompiler(outputArchive(OutputFormat.ZIP)) { _, out ->
    assertSuccess(
      cli(
        "--no-crawl",
        "--http",
        "--verbose",
        helloWorldManifest,
        embeddedApp().url.toString(),
        out.path,
      ),
    )
  }

  @Test fun testCommandLineEmptyCompile(): Unit = withCompiler(outputArchive(OutputFormat.ZIP)) { _, out ->
    assertSuccess {
      cli(
        "--no-crawl",
        "--http",
        "--verbose",
        emptyManifest,
        embeddedApp().url.toString(),
        out.path,
      )
    }
  }

  @Test fun testCommandLineHttpCompileDir(): Unit = withCompiler(outputDirectory()) { _, out ->
    assertSuccess(
      cli(
        "--no-crawl",
        "--http",
        "--verbose",
        helloWorldManifest,
        embeddedApp().url.toString(),
        out.path,
      ),
    )
  }

  @Test fun testCommandLineEmptyCompileDir(): Unit = withCompiler(outputDirectory()) { _, out ->
    assertSuccess {
      cli(
        "--no-crawl",
        "--http",
        "--verbose",
        emptyManifest,
        embeddedApp().url.toString(),
        out.path,
      )
    }
  }
}
