package elide.tool.ssg

import elide.tool.ssg.SiteCompilerParams.OutputFormat
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Disabled
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
