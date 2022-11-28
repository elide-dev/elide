package elide.tool.ssg

import elide.tool.ssg.SiteCompilerParams.OutputFormat
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

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
