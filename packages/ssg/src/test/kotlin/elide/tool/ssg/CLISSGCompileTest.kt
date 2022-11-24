package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test

/** Tests which invoke the SSG compiler over the CLI. */
@MicronautTest(
  application = helloworld.App::class,
  startApplication = true,
  packages = [
    "helloworld",
    "helloworld.*",
    "elide.tools.ssg",
    "elide.tools.ssg.*",
  ],
) class CLISSGCompileTest : AbstractSSGCompilerTest() {
  @Test fun testCommandLineHttpCompile(): Unit = withCompiler {
    assertSuccess(
      cli(
        "--no-crawl",
        "--http",
        "--verbose",
        helloWorldManifest,
        embeddedApp().url.toString(),
        outputArchive(SiteCompilerParams.OutputFormat.ZIP).path,
      )
    )
  }

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

  @Test fun testCommandLineEmptyCompile(): Unit = withCompiler {
    assertSuccess {
      cli(
        "--no-crawl",
        "--http",
        "--verbose",
        emptyManifest,
        embeddedApp().url.toString(),
        outputArchive(SiteCompilerParams.OutputFormat.ZIP).path,
      )
    }
  }
}
