package elide.tool.cli.control

import jdk.jshell.spi.ExecutionControl
import jdk.jshell.spi.ExecutionControlProvider
import jdk.jshell.spi.ExecutionEnv

/**
 * # Execution Control
 *
 * Installed as the execution control provider for an Espresso-based JShell.
 */
class EspressoExecutionControlProvider: ExecutionControlProvider {
  override fun name(): String {
    TODO("Not yet implemented")
  }

  override fun generate(env: ExecutionEnv?, parameters: MutableMap<String, String>?): ExecutionControl {
    TODO("Not yet implemented")
  }
}
