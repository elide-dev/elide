package elide.tool.cli

import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlin.test.assertTrue

@TestCase class ElideToolErrorLocationTest : AbstractEntryTest() {
  @Test fun testGuestErrorHighlightsCorrectLine() {
    val temp = Files.createTempFile("elide-js-error-test", ".js").toFile()
    temp.writeText(
      """
      const a = 1;
      const b = 2;
      throw new Error("boom");
      """.trimIndent(),
      Charsets.UTF_8
    )

    // capture stdout and stderr
    val stubbedOut = ByteArrayOutputStream()
    val stubbedErr = ByteArrayOutputStream()
    val originalOut = System.out
    val originalErr = System.err
    System.setOut(PrintStream(stubbedOut))
    System.setErr(PrintStream(stubbedErr))

    val code = try {
      Elide.exec(arrayOf("run", temp.absolutePath))
    } finally {
      System.setOut(originalOut)
      System.setErr(originalErr)
    }

    val stderr = stubbedErr.toString("UTF-8")
    // Should have failed with a non-zero code
    assertTrue(code != 0, "expected non-zero exit code, got $code; stderr=\n$stderr")
    // Should point to line 3 where the error occurs
    assertTrue(
      stderr.contains("→ 3┊") || stderr.contains("\u2192 3┊"),
      "expected error marker on line 3; stderr=\n$stderr"
    )
  }
}

