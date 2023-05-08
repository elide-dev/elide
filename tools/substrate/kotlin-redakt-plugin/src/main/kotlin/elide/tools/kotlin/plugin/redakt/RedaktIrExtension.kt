package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName

/**
 * # Redakt: IR Extension
 *
 * Extends the IR compiler with logic to handle detection of `Sensitive` annotations (by default,
 * [elide.annotations.data.Sensitive]). When found, the plugin will splice in replacement `toString` and related
 * methods to redact the value from logs.
 */
internal class RedaktIrExtension (
  private val messageCollector: MessageCollector,
  private val maskString: String,
  private val sensitiveAnnotation: String
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val transformer = RedaktIrVisitor(
      pluginContext,
      FqName(sensitiveAnnotation),
      maskString,
      messageCollector,
    )
    moduleFragment.transform(
      transformer,
      null,
    )
  }
}
