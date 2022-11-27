package elide.tool.ssg

import elide.tool.ssg.SiteCompilerParams.OutputFormat
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test

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
) class CLISSGCompileTest : AbstractSSGCompilerTest() {
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
      )
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
      )
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
