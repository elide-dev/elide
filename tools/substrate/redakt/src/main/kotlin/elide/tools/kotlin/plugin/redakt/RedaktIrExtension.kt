package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 *
 */
internal class RedaktIrExtension (
  private val messageCollector: MessageCollector,
  private val string: String,
  private val file: String
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'string' = $string")
    messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'file' = $file")
  }
}
