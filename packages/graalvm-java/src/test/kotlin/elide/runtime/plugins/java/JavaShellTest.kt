package elide.runtime.plugins.java

import jdk.jshell.JShell
import org.junit.jupiter.api.Test
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.java.shell.GuestExecutionControlProvider
import elide.runtime.plugins.jvm.Jvm

@OptIn(DelicateElideApi::class) class JavaShellTest {
  private val logging by lazy { Logging.of(JavaShellTest::class) }

  @Test fun testJavaShell() {
    logging.debug("Configuring engine")
    val engine = PolyglotEngine {
      install(Jvm)
      install(Java)
    }

    logging.debug("Acquiring context")
    val context = engine.acquire()

    logging.debug("Preparing JShell backend provider")
    val provider = GuestExecutionControlProvider(context)

    logging.debug("Configuring JShell")
    val shell = JShell.builder()
      .executionEngine(provider, mutableMapOf())
      .build()

    logging.debug("Evaluating sample")
    shell.eval("int a = 0;").forEach { logging.info { "> $it" } }
    shell.eval("System.out.println(a);").forEach { logging.info { "> $it" } }
  }
}