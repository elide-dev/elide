package elide.tools.kotlin.plugin.redakt

import com.google.auto.service.AutoService
import elide.tools.kotlin.plugin.RedaktPlugin
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 *
 */
@AutoService(ComponentRegistrar::class)
internal class RedaktRegistrar(
  private val defaultString: String,
  private val defaultFile: String,
) : ComponentRegistrar {
  @Suppress("unused")  // Used by service loader
  constructor() : this(
    defaultString = "Hello, World!",
    defaultFile = "file.txt"
  )

  /** @inheritDoc */
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) {
    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val string = configuration.get(RedaktPlugin.ARG_STRING, defaultString)
    val file = configuration.get(RedaktPlugin.ARG_FILE, defaultFile)
    IrGenerationExtension.registerExtension(
      project,
      RedaktIrExtension(messageCollector, string, file),
    )
  }
}
