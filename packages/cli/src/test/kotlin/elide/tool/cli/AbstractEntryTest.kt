package elide.tool.cli

import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference
import jakarta.inject.Inject

abstract class AbstractEntryTest {
  val testRuby = false
  val testPython = true
  val testJava = false
  val testKotlin = true
  val testSwift = false
  val testWasm = false

  companion object {
    @JvmStatic protected val rootProjectPath: Path = Paths.get(System.getProperty("elide.rootDir"))

    @JvmStatic  protected val testScriptsPath: Path = rootProjectPath
      .resolve("tools")
      .resolve("scripts")
      .toAbsolutePath()
  }

  @Inject lateinit var tool: Elide

  protected fun assertToolExitsWithCode(expected: Int, vararg args: String) {
    fun block(): Int = Elide.exec(args.toList().toTypedArray())

    // capture stdout and stderr
    val stubbedOut = ByteArrayOutputStream()
    val stubbedErr = ByteArrayOutputStream()
    val originalOut = System.out
    val originalErr = System.err
    System.setOut(PrintStream(stubbedOut))
    System.setErr(PrintStream(stubbedErr))
    val capturedOut: AtomicReference<ByteArray> = AtomicReference(null)
    val capturedErr: AtomicReference<ByteArray> = AtomicReference(null)

    val code = try {
      block()
    } finally {
      capturedOut.set(stubbedOut.toByteArray())
      capturedErr.set(stubbedErr.toByteArray())
      System.setOut(originalOut)
      System.setErr(originalErr)
    }
    assert(expected == code) {
      "should exit with code $expected, but got $code;\n" +
      "stdout: ${String(capturedOut.get())}\nstderr: ${String(capturedErr.get())}"
    }
  }

  protected fun assertToolRunsWith(vararg args: String) {
    assertToolExitsWithCode(0, *args)
  }
}
