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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import elide.tool.ssg.SiteCompilerParams.OutputFormat

/** Tests which invoke the SSG compiler over the Java API, using a JAR as the application target. */
@MicronautTest(
  packages = [
    "helloworld",
    "helloworld.*",
    "elide.tools.ssg",
    "elide.tools.ssg.*",
  ],
) class JavaAPISSGCompileJarTest : AbstractSSGCompilerTest() {
  @Test @Disabled fun testProgrammaticJarCompile(): Unit = withCompiler(outputArchive(OutputFormat.ZIP)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS,
      )
    }
  }

  @Test @Disabled fun testProgrammaticJarCompileToTar(): Unit = withCompiler(outputArchive(OutputFormat.TAR)) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS,
      )
    }
  }

  @Test @Disabled fun testProgrammaticJarCompileToDir(): Unit = withCompiler(outputDirectory()) { _, out ->
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = out,
        options = SiteCompilerParams.Options.DEFAULTS,
      )
    }
  }
}
