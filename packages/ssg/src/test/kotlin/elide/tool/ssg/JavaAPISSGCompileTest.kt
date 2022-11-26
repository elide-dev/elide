package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
) class JavaAPISSGCompileTest : AbstractSSGCompilerTest() {
  @Test fun testProgrammaticVersion() {
    val version = SiteCompiler.version()
    assertNotNull(version, "should not get `null` for tool version")
    assertTrue(version.isNotEmpty(), "tool version should not be empty")
  }

  @Test fun testProgrammaticHttpCompile(): Unit = withCompiler {
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = helloWorldManifest,
        target = embeddedApp().url.toString(),
        output = outputArchive(SiteCompilerParams.OutputFormat.ZIP),
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }

  @Test fun testProgrammaticEmptyCompile(): Unit = withCompiler {
    assertDoesNotThrow {
      SiteCompiler.compile(
        manifest = emptyManifest,
        target = embeddedApp().url.toString(),
        output = outputArchive(SiteCompilerParams.OutputFormat.ZIP),
        options = SiteCompilerParams.Options.DEFAULTS.copy(
          httpMode = true,
          crawl = false,
        ),
      )
    }
  }
}
